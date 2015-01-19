package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import jersey.repackaged.com.google.common.util.concurrent.Futures;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockHenkiloAsyncResource implements HenkiloAsyncResource {
    public static AtomicBoolean serviceIsAvailable = new AtomicBoolean(true);
    private static final Logger LOG = LoggerFactory.getLogger(MockHenkiloAsyncResource.class);
    public List<HenkiloCreateDTO> henkiloPrototyypit = null;

    @Override
    public Future<List<Henkilo>> haeTaiLuoHenkilot(final List<HenkiloCreateDTO> hp) {
        return Optional.ofNullable(MockHenkiloAsyncResource.<List<Henkilo>>serviceAvailableCheck()).orElseGet(
                () -> {
                    if (hp == null) {
                        LOG.error("Null-prototyyppi lista!");
                        throw new RuntimeException("Null-prototyyppi lista!");
                    }
                    //this.henkiloPrototyypit = henkiloPrototyypit;
                    LOG.info("MockHenkilöAsyncResource sai {}kpl henkilöitä. Tehdään konversio ja palautetaan immediate future.", hp.size());
                    henkiloPrototyypit = hp;
                    return Futures.immediateFuture(henkiloPrototyypit.stream()
                            .map(prototyyppi -> toHenkilo(prototyyppi))
                            .collect(Collectors.toList()));
                });
    }

    private Henkilo toHenkilo(HenkiloCreateDTO proto) {
        final Henkilo henkilo = new Henkilo();
        henkilo.setHenkiloTyyppi(proto.henkiloTyyppi);
        henkilo.setKutsumanimi(proto.kutsumanimi);
        henkilo.setSukunimi(proto.sukunimi);
        henkilo.setEtunimet(proto.etunimet);
        henkilo.setHetu(proto.hetu);
        henkilo.setOidHenkilo(MockData.hakijaOid);
        return henkilo;
    }
    private static <T> Future<T> serviceAvailableCheck() {
        if(!serviceIsAvailable.get()) {
            return Futures.immediateFailedFuture(new RuntimeException("MockHenkilöpalvelu on kytketty pois päältä!"));
        }
        return null;
    }
}
