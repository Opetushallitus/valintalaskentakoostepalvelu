package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import akka.actor.ActorSystem;
import akka.actor.TypedActor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;

/**
 * 
 * @author jussija
 *
 * osittainenlaskenta?
 */
public class LaskentaSupervisorActorImpl implements LaskentaSupervisor {
	private final static Logger LOG = LoggerFactory.getLogger(LaskentaSupervisorActorImpl.class);

	private final Map<String,LaskentaActorWrapper> ajossaOlevatLaskennat;
	private final ActorSystem actorSystem;

	public LaskentaSupervisorActorImpl(ActorSystem actorSystem) {
		this.ajossaOlevatLaskennat = Maps.newConcurrentMap();
		this.actorSystem = actorSystem;
	}

	@Override
	public void workAvailable() {

	}

	@Override
	public void valmis(String uuid) {
		lopeta(uuid, ajossaOlevatLaskennat.remove(uuid));
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
	
	public void luoJaKaynnistaLaskenta(String uuid, String hakuOid, boolean osittainen, LaskentaActor laskentaActor) {
		try {
			laskentaActor.aloita();
		} catch(Exception e) {
			LOG.error("\r\n###\r\n### Laskenta uuid:lle {} haulle {} ei kaynnistynyt!\r\n###", uuid, hakuOid);
		}
		ajossaOlevatLaskennat.merge(uuid, new LaskentaActorWrapper(uuid, hakuOid, osittainen, laskentaActor), (oldValue, value) -> {
			LOG.warn("\r\n###\r\n### Laskenta uuid:lle {} haulle {} oli jo kaynnissa! Lopetataan vanha laskenta!\r\n###", uuid, hakuOid);
			lopeta(uuid, oldValue);
			return value;
		});
		
	}

	public List<Laskenta> ajossaOlevatLaskennat() {
		return Lists.newArrayList(ajossaOlevatLaskennat.values());
	}

	public Laskenta haeLaskenta(String uuid) {
		return ajossaOlevatLaskennat.get(uuid);
	}

}
