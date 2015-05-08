package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.List;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import akka.actor.ActorSystem;

import com.typesafe.config.ConfigFactory;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class LaskentaActorSystem implements ValintalaskentaKerrallaRouteValvomo, ValintalaskentaKerrallaRoute {
	private final LaskentaActorFactory laskentaActorFactory;
	private final LaskentaSupervisor laskentaSupervisor;

	@Autowired
	public LaskentaActorSystem(
			LaskentaSeurantaAsyncResource seurantaAsyncResource,
			ValintaperusteetAsyncResource valintaperusteetAsyncResource,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource
	) {
		ActorSystem actorSystem = ActorSystem.create("ValintalaskentaActorSystem", ConfigFactory.defaultOverrides());
		this.laskentaSupervisor = new LaskentaSupervisorImpl(actorSystem);
		this.laskentaActorFactory = new LaskentaActorFactory(
				valintalaskentaAsyncResource,
				applicationAsyncResource,
				valintaperusteetAsyncResource,
				seurantaAsyncResource,
				suoritusrekisteriAsyncResource,
				laskentaSupervisor
		);
	}

	@Override
	public void workAvailable() {}

	@Override
	public void suoritaValintalaskentaKerralla(final ParametritDTO parametritDTO, final LaskentaAloitus laskentaAloitus) {
		LaskentaActor laskentaActor = laskentaActorFactory.createLaskentaActor(new LaskentaActorParams(laskentaAloitus, parametritDTO));
		laskentaSupervisor.luoJaKaynnistaLaskenta(laskentaAloitus.getUuid(), laskentaAloitus.getHakuOid(), laskentaAloitus.isOsittainenLaskenta(), laskentaActor);
	}

	@Override
	public List<Laskenta> ajossaOlevatLaskennat() {
		return laskentaSupervisor.ajossaOlevatLaskennat();
	}

	@Override
	public Laskenta haeLaskenta(String uuid) {
		return laskentaSupervisor.haeLaskenta(uuid);
	}
}
