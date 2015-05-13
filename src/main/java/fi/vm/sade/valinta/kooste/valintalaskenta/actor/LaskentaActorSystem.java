package fi.vm.sade.valinta.kooste.valintalaskenta.actor;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.ConfigFactory;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskennanKaynnistajaActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.WorkAvailable;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.WorkerAvailable;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
	public LaskentaActorSystem(LaskentaSeurantaAsyncResource seurantaAsyncResource, LaskentaKaynnistin laskentaKaynnistin, LaskentaActorFactory laskentaActorFactory) {
		this.laskentaActorFactory = laskentaActorFactory;
        this.laskentaKaynnistin = laskentaKaynnistin;
        this.seurantaAsyncResource = seurantaAsyncResource;
        this.actorSystem = ActorSystem.create("ValintalaskentaActorSystem", ConfigFactory.defaultOverrides());
        laskennanKaynnistajaActor = actorSystem.actorOf(LaskennanKaynnistajaActor.props(this));
    }

    @PostConstruct
    @Override
    public void workAvailable() {
        laskennanKaynnistajaActor.tell(new WorkAvailable(), null);
    }

    @Override
    public void suoritaValintalaskentaKerralla(final ParametritDTO parametritDTO, final LaskentaAloitus laskentaAloitus) {
        LaskentaActor laskentaActor = laskentaActorFactory.createLaskentaActor(this,new LaskentaActorParams(laskentaAloitus, parametritDTO));
        try {
            luoJaKaynnistaLaskenta(laskentaAloitus.getUuid(), laskentaAloitus.getHakuOid(), laskentaAloitus.isOsittainenLaskenta(), laskentaActor);
        }catch (Exception e){
            //temp nop
        }
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

    public String haeJaKaynnistaLaskenta() {
        final ValueConsumer<String> consumer = new ValueConsumer<>();
        seurantaAsyncResource.otaSeuraavaLaskentaTyonAlle(consumer, (Throwable t) -> {
        });

        final String uuid = consumer.getValue();
        if (null == uuid)
            return null;

        ValueConsumer<LaskentaActorParams> laskentaActorParamsConsumer = new ValueConsumer<>();
        laskentaKaynnistin.haeLaskentaParams(uuid, params-> {
            laskentaActorParamsConsumer.accept(params);
        });
        final LaskentaActorParams laskentaActorParams = laskentaActorParamsConsumer.getValue();
        if (null == laskentaActorParams)
            return null;
        LaskentaActor laskentaActor = laskentaActorFactory.createLaskentaActor(this, laskentaActorParams);

        return luoJaKaynnistaLaskenta(uuid, laskentaActorParams.getHakuOid(), laskentaActorParams.isOsittainen(), laskentaActor);
    }

    String luoJaKaynnistaLaskenta(String uuid, String hakuOid, boolean osittainen, LaskentaActor laskentaActor) {
        try {
            laskentaActor.aloita();
        } catch (Exception e) {
            LOG.error("\r\n###\r\n### Laskenta uuid:lle {} haulle {} ei kaynnistynyt!\r\n###", uuid, hakuOid);
            return null;
        }

        ajossaOlevatLaskennat.merge(uuid, new LaskentaActorWrapper(uuid, hakuOid, osittainen, laskentaActor), (LaskentaActorWrapper oldValue, LaskentaActorWrapper value) -> {
            LOG.warn("\r\n###\r\n### Laskenta uuid:lle {} haulle {} oli jo kaynnissa! Lopetataan vanha laskenta!\r\n###", uuid, hakuOid);
            lopeta(uuid, oldValue);
            return value;
        });
        return uuid;
    }

    private void lopeta(String uuid, LaskentaActorWrapper l) {
        if (l != null) {
            try {
                TypedActor.get(actorSystem).poisonPill(l.laskentaActor());
                LOG.info("PoisonPill lahetetty onnistuneesti Actorille {}", uuid);
            } catch (Exception e) {
                LOG.error("PoisonPill lahetys epaonnistui Actorille {}: {}", uuid, e.getMessage());
            }
        } else {
            LOG.warn("Yritettiin valmistaa laskentaa {} mutta laskenta ei ollut enaa ajossa!", uuid);
        }
    }

    private class ValueConsumer<T> implements Consumer<T>{
        private T value;

        @Override
        public void accept(T value) {
            this.value = value;
        }

        public T getValue(){
            return value;
        }
    }
}
