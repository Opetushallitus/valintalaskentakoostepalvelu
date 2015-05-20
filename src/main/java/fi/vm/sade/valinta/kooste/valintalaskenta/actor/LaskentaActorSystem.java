package fi.vm.sade.valinta.kooste.valintalaskenta.actor;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigFactory;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaStarterActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

import static fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaStarterActor.*;

/**
 * @author Jussi Jartamo
 */
@Service
public class LaskentaActorSystem implements ValintalaskentaKerrallaRouteValvomo, ValintalaskentaKerrallaRoute, LaskentaSupervisor {
    private final static Logger LOG = LoggerFactory.getLogger(LaskentaActorSystem.class);

    private final LaskentaActorFactory laskentaActorFactory;

    private final LaskentaSeurantaAsyncResource seurantaAsyncResource;
    private final ActorSystem actorSystem;
    private final ActorRef laskennanKaynnistajaActor;
    private final Map<String, LaskentaActorWrapper> ajossaOlevatLaskennat = Maps.newConcurrentMap();
    private final LaskentaKaynnistin laskentaKaynnistin;

    @Autowired
    public LaskentaActorSystem(LaskentaSeurantaAsyncResource seurantaAsyncResource, LaskentaKaynnistin laskentaKaynnistin, LaskentaActorFactory laskentaActorFactory,
                               @Value("${valintalaskentakoostepalvelu.maxWorkerCount:8}") int maxWorkers) {
        this.laskentaActorFactory = laskentaActorFactory;
        this.laskentaKaynnistin = laskentaKaynnistin;
        this.seurantaAsyncResource = seurantaAsyncResource;
        this.actorSystem = ActorSystem.create("ValintalaskentaActorSystem", ConfigFactory.defaultOverrides());
        laskennanKaynnistajaActor = actorSystem.actorOf(props(this, maxWorkers));
    }

    @PostConstruct
    @Override
    public void workAvailable() {
        laskennanKaynnistajaActor.tell(new WorkAvailable(), null);
    }

    @Override
    public void suoritaValintalaskentaKerralla(final ParametritDTO parametritDTO, final LaskentaAloitus laskentaAloitus) {
        LaskentaActor laskentaActor = laskentaActorFactory.createLaskentaActor(this, new LaskentaActorParams(laskentaAloitus, parametritDTO));
        luoJaKaynnistaLaskenta(laskentaAloitus.getUuid(), laskentaAloitus.getHakuOid(), laskentaAloitus.isOsittainenLaskenta(), laskentaActor);
    }

    @Override
    public List<Laskenta> ajossaOlevatLaskennat() {
        return Lists.newArrayList(ajossaOlevatLaskennat.values());
    }

    @Override
    public Laskenta haeLaskenta(String uuid) {
        return ajossaOlevatLaskennat.get(uuid);
    }

    @Override
    public void valmis(String uuid) {
        laskennanKaynnistajaActor.tell(new WorkerAvailable(), ActorRef.noSender());
        lopeta(uuid, ajossaOlevatLaskennat.remove(uuid));
    }

    public void haeJaKaynnistaLaskenta() {
        seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle(this::kaynnistaLaskentaJosLaskettavaa, (Throwable t) -> {});
    }

    private void kaynnistaLaskentaJosLaskettavaa(String uuid) {
        if (uuid == null) {
            LOG.info("Ei laskettavaa");
            laskennanKaynnistajaActor.tell(new NoWorkAvailable(), ActorRef.noSender());
        } else {
            LOG.info("Luodaan ja aloitetaan Laskenta uuid:lle {}", uuid);
            laskentaKaynnistin.haeLaskentaParams(uuid, params -> luoJaKaynnistaLaskenta(uuid, params.getHakuOid(), params.isOsittainen(), laskentaActorFactory.createLaskentaActor(this, params)));
        }
    }

    protected void luoJaKaynnistaLaskenta(String uuid, String hakuOid, boolean osittainen, LaskentaActor laskentaActor) {
        try {
            laskentaActor.aloita();
        } catch (Exception e) {
            LOG.error("\r\n###\r\n### Laskenta uuid:lle {} haulle {} ei kaynnistynyt!\r\n###", uuid, hakuOid, e);
        }

        ajossaOlevatLaskennat.merge(uuid, new LaskentaActorWrapper(uuid, hakuOid, osittainen, laskentaActor), (LaskentaActorWrapper oldValue, LaskentaActorWrapper value) -> {
            LOG.warn("\r\n###\r\n### Laskenta uuid:lle {} haulle {} oli jo kaynnissa! Lopetataan vanha laskenta!\r\n###", uuid, hakuOid);
            lopeta(uuid, oldValue);
            return value;
        });
    }

    private void lopeta(String uuid, LaskentaActorWrapper l) {
        if (l != null) {
            try {
                TypedActor.get(actorSystem).poisonPill(l.laskentaActor());
                LOG.info("PoisonPill lahetetty onnistuneesti Actorille {}", uuid);
            } catch (Exception e) {
                LOG.error("PoisonPill lahetys epaonnistui Actorille {}: {}", uuid, e);
            }
        } else {
            LOG.warn("Yritettiin valmistaa laskentaa {} mutta laskenta ei ollut enaa ajossa!", uuid);
        }
    }
}
