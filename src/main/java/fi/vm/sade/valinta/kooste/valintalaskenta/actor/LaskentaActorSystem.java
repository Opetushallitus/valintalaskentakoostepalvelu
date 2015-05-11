package fi.vm.sade.valinta.kooste.valintalaskenta.actor;


import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.typesafe.config.ConfigFactory;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskennanKaynnistajaActor;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.WorkAvailable;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Service
public class LaskentaActorSystem implements ValintalaskentaKerrallaRouteValvomo, ValintalaskentaKerrallaRoute {
	private final LaskentaActorFactory laskentaActorFactory;
	private final LaskentaSupervisor laskentaSupervisor;
    private final ActorRef laskennanKaynnistajaActor;

	@Autowired
	public LaskentaActorSystem(
			LaskentaSeurantaAsyncResource seurantaAsyncResource,
			ValintaperusteetAsyncResource valintaperusteetAsyncResource,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource
	) {
        ActorSystem actorSystem = ActorSystem.create("ValintalaskentaActorSystem", ConfigFactory.defaultOverrides());
		this.laskentaSupervisor = new LaskentaSupervisorImpl(actorSystem, this::workerAvailable);

		this.laskentaActorFactory = new LaskentaActorFactory(
				valintalaskentaAsyncResource,
				applicationAsyncResource,
				valintaperusteetAsyncResource,
				seurantaAsyncResource,
				suoritusrekisteriAsyncResource,
				laskentaSupervisor
		);

        laskennanKaynnistajaActor = actorSystem.actorOf(LaskennanKaynnistajaActor.props(laskentaSupervisor));
    }

    public void workerAvailable(Object evvk){
        laskennanKaynnistajaActor.tell(new WorkAvailable(), ActorRef.noSender());
    }

	@Override
	public void workAvailable() {
        laskennanKaynnistajaActor.tell(new WorkAvailable(), null);
    }

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
