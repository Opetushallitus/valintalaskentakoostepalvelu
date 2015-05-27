package fi.vm.sade.valinta.kooste.valintalaskenta.actor;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigFactory;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

import static fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaStarterActor.*;

/**
 * @author Jussi Jartamo
 */
@Service
@DependsOn("LaskentaSeurantaAsyncResource")
public class LaskentaActorSystem implements ValintalaskentaKerrallaRouteValvomo, ValintalaskentaKerrallaRoute, LaskentaSupervisor {
    private final static Logger LOG = LoggerFactory.getLogger(LaskentaActorSystem.class);

    private final LaskentaActorFactory laskentaActorFactory;

    private final LaskentaSeurantaAsyncResource seurantaAsyncResource;
    private final ActorSystem actorSystem;
    private final ActorRef laskennanKaynnistajaActor;
    private final Map<String, LaskentaActorWrapper> runningLaskentas = Maps.newConcurrentMap();
    private final LaskentaStarter laskentaStarter;

    @Autowired
    public LaskentaActorSystem(LaskentaSeurantaAsyncResource seurantaAsyncResource, LaskentaStarter laskentaStarter, LaskentaActorFactory laskentaActorFactory,
                               @Value("${valintalaskentakoostepalvelu.maxWorkerCount:8}") int maxWorkers) {
        this.laskentaActorFactory = laskentaActorFactory;
        this.laskentaStarter = laskentaStarter;
        this.seurantaAsyncResource = seurantaAsyncResource;
        this.actorSystem = ActorSystem.create("ValintalaskentaActorSystem", ConfigFactory.defaultOverrides());
        laskennanKaynnistajaActor = actorSystem.actorOf(props(this, maxWorkers));
    }

    @PostConstruct
    @Override
    public void workAvailable() {
        laskennanKaynnistajaActor.tell(new WorkAvailable(), ActorRef.noSender());
    }

    @Override
    public void suoritaValintalaskentaKerralla(final ParametritDTO parametritDTO, final LaskentaStartParams laskentaStartParams) {
        LaskentaActor laskentaActor = laskentaActorFactory.createLaskentaActor(this, new LaskentaActorParams(laskentaStartParams, parametritDTO));
        createAndStartLaskenta(laskentaStartParams, laskentaActor);
    }

    @Override
    public List<Laskenta> runningLaskentas() {
        return Lists.newArrayList(runningLaskentas.values());
    }

    @Override
    public Laskenta fetchLaskenta(String uuid) {
        return runningLaskentas.get(uuid);
    }

    @Override
    public void ready(String uuid) {
        LaskentaActorWrapper actorWrapper = runningLaskentas.remove(uuid);
        stopActor(uuid, actorWrapper.laskentaActor());
    }

    public void fetchAndStartLaskenta(ActorRef starterActor) {
        seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle(
                this::startLaskentaIfWorkAvailable,
                (Throwable t) -> {
                    starterActor.tell(WorkerAvailable.class, ActorRef.noSender());
                    throw new RuntimeException("Laskennan käynnistys epäonnistui", t);
                });
    }

    private void startLaskentaIfWorkAvailable(String uuid) {
        if (uuid == null) {
            LOG.info("Ei laskettavaa");
            laskennanKaynnistajaActor.tell(new NoWorkAvailable(), ActorRef.noSender());
        } else {
            LOG.info("Luodaan ja aloitetaan Laskenta uuid:lle {}", uuid);
            laskentaStarter.fetchLaskentaParams(
                    laskennanKaynnistajaActor,
                    uuid,
                    params -> createAndStartLaskenta(params.getLaskentaStartParams(), laskentaActorFactory.createLaskentaActor(this, params))
            );
        }
    }

    protected void createAndStartLaskenta(LaskentaStartParams params, LaskentaActor laskentaActor) {
        String uuid = params.getUuid();
        String hakuOid = params.getHakuOid();

        try {
            laskentaActor.start();
        } catch (Exception e) {
            LOG.error("\r\n###\r\n### Laskenta uuid:lle {} haulle {} ei kaynnistynyt!\r\n###", uuid, hakuOid, e);
        }

        runningLaskentas.merge(uuid, new LaskentaActorWrapper(params, laskentaActor), (LaskentaActorWrapper oldValue, LaskentaActorWrapper value) -> {
            LOG.warn("\r\n###\r\n### Laskenta uuid:lle {} haulle {} oli jo kaynnissa! Lopetataan vanha laskenta!\r\n###", uuid, hakuOid);
            stopActor(uuid, oldValue.laskentaActor());
            return value;
        });
    }

    private void stopActor(String uuid, LaskentaActor actor) {
        laskennanKaynnistajaActor.tell(new WorkerAvailable(), ActorRef.noSender());
        if (actor != null) {
            try {
                TypedActor.get(actorSystem).poisonPill(actor);
                LOG.info("PoisonPill lahetetty onnistuneesti Actorille " + uuid);
            } catch (Exception e) {
                LOG.error("PoisonPill lahetys epaonnistui Actorille " + uuid, e);
            }
        } else {
            LOG.warn("Yritettiin sammuttaa laskenta " + uuid + ", mutta laskenta ei ollut enaa ajossa!");
        }
    }
}
