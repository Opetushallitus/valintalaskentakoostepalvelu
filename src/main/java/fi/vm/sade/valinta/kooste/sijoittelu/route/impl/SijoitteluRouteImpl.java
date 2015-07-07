package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import static fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute.SIJOITTELU_REITTI;

import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import fi.vm.sade.valinta.kooste.KoostepalveluRouteBuilder;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoittelunValvonta;

@Component
public class SijoitteluRouteImpl extends KoostepalveluRouteBuilder<Sijoittelu> implements SijoittelunValvonta {
    private static final Logger LOG = LoggerFactory.getLogger(SijoitteluRouteImpl.class);
    private final String DEADLETTERCHANNEL = "direct:sijoittelun_deadletterchannel";
    private final SijoitteleAsyncResource sijoitteluResource;

    @Autowired
    public SijoitteluRouteImpl(SijoitteleAsyncResource sijoitteluResource) {
        this.sijoitteluResource = sijoitteluResource;
    }

    @Override
    protected Cache<String, Sijoittelu> configureCache() {
        return CacheBuilder.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).removalListener(new RemovalListener<String, Sijoittelu>() {
            public void onRemoval(RemovalNotification<String, Sijoittelu> notification) {
                LOG.info("{} siivottu pois muistista", notification.getValue());
            }
        }).build();
    }

    @Override
    public Sijoittelu haeAktiivinenSijoitteluHaulle(String hakuOid) {
        return getKoostepalveluCache().getIfPresent(hakuOid);
    }

    @Override
    public void configure() throws Exception {
        interceptFrom(SIJOITTELU_REITTI).process(Reititys.<Sijoittelu>kuluttaja(l -> {
            Sijoittelu vanhaSijoittelu = getKoostepalveluCache().getIfPresent(l.getHakuOid());
            if (vanhaSijoittelu != null && vanhaSijoittelu.isTekeillaan()) {
                // varmistetaan etta uudelleen ajon reunatapauksessa
                // mahdollisesti viela suorituksessa oleva vanha
                // laskenta
                // lakkaa kayttamasta resursseja ja siivoutuu ajallaan
                // pois
                throw new RuntimeException("Sijoittelu haulle " + l.getHakuOid() + " on jo kaynnissa!");
            }
            getKoostepalveluCache().put(l.getHakuOid(), l);
        }));
        from(DEADLETTERCHANNEL)
                .routeId("Sijoittelun deadletterchannel")
                .process(exchange -> LOG.error("Sijoittelu paattyi virheeseen {}\r\n{}", simple("${exception.message}").evaluate(exchange, String.class), simple("${exception.stacktrace}").evaluate(exchange, String.class)))
                .stop();
        from(SIJOITTELU_REITTI)
                .errorHandler(deadLetterChannel())
                .routeId("Sijoittelureitti")
                .threads()
                .process(Reititys.<Sijoittelu>kuluttaja((s -> {
                    LOG.info("Aloitetaan sijoittelu haulle {}", s.getHakuOid());
                    sijoitteluResource.sijoittele(s.getHakuOid(), success -> {
                        s.setValmis();
                    }, e -> {
                        LOG.error("Sijoittelu epaonnistui haulle " + s.getHakuOid(), e);
                        s.setOhitettu();
                    });
                })));
    }

    @Override
    protected String deadLetterChannelEndpoint() {
        return DEADLETTERCHANNEL;
    }
}
