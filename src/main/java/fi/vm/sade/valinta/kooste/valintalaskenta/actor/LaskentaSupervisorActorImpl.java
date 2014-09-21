package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedActorExtension;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;

/**
 * 
 * @author jussija
 *
 *         osittainenlaskenta?
 */
public class LaskentaSupervisorActorImpl implements LaskentaSupervisor {
	private final static Logger LOG = LoggerFactory
			.getLogger(LaskentaSupervisorActorImpl.class);
	private final Set<LaskentaActorWrapper> ajossaOlevatLaskennat;

	public LaskentaSupervisorActorImpl() {
		this.ajossaOlevatLaskennat = Sets.newHashSet();
	}

	@Override
	public void valmis(String uuid) {
		ajossaOlevatLaskennat
				.removeIf(l -> {
					if (uuid.equals(l.getUuid())) {
						try {
							TypedActor.get(TypedActor.context().system())
									.poisonPill(l);
							LOG.error(
									"PoisonPill lahetetty onnistuneesti Actorille {}",
									uuid);
						} catch (Exception e) {
							LOG.error(
									"PoisonPill lahetys epaonnistui Actorille {}: {}",
									uuid, e.getMessage());
						}
						return true;
					}
					return false;
				});
	}

	public void luoJaKaynnistaLaskenta(String uuid, String hakuOid,
			Function<LaskentaSupervisor, LaskentaActor> laskentaProducer) {
		LaskentaActor laskentaActor = laskentaProducer.apply(TypedActor.self());
		laskentaActor.aloita();
		ajossaOlevatLaskennat.add(new LaskentaActorWrapper(uuid, hakuOid,
				laskentaActor));
	}

	public List<Laskenta> ajossaOlevatLaskennat() {
		return Lists.newArrayList(ajossaOlevatLaskennat);
	}

	public Laskenta haeLaskenta(String uuid) {
		return ajossaOlevatLaskennat.stream()
				.filter(l -> uuid.equals(l.getUuid())).findAny().orElse(null);
	}

}
