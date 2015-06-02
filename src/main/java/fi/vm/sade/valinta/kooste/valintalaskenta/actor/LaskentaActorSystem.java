package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import akka.actor.ActorSystem;
import akka.actor.TypedActor;
import akka.actor.TypedActorExtension;
import akka.actor.TypedProps;
import akka.japi.Creator;

import com.typesafe.config.ConfigFactory;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
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
public class LaskentaActorSystem implements
		ValintalaskentaKerrallaRouteValvomo, ValintalaskentaKerrallaRoute {
	private static final Logger LOG = LoggerFactory
			.getLogger(LaskentaActorSystem.class);
	private final TypedActorExtension typed;
	private final ActorSystem actorSystem;
	private static final Integer HAE_KAIKKI_VALINNANVAIHEET = new Integer(-1);

	private final LaskentaActorFactory laskentaActorFactory;
	private final LaskentaSupervisor laskentaSupervisor;

	@Autowired
	public LaskentaActorSystem(
			LaskentaSeurantaAsyncResource seurantaAsyncResource,
			ValintaperusteetAsyncResource valintaperusteetAsyncResource,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
		this.actorSystem = ActorSystem.create("ValintalaskentaActorSystem",
				ConfigFactory.defaultOverrides());
		
		this.typed = TypedActor.get(actorSystem);
		this.laskentaSupervisor = new LaskentaSupervisorActorImpl(actorSystem);
		this.laskentaActorFactory = new LaskentaActorFactory(
				valintalaskentaAsyncResource, applicationAsyncResource,
				valintaperusteetAsyncResource, seurantaAsyncResource,
				suoritusrekisteriAsyncResource, laskentaSupervisor);
	}

	@Override
	public void suoritaValintalaskentaKerralla(
			final HakuV1RDTO haku,
			final ParametritDTO parametritDTO,
			final LaskentaAloitus laskentaAloitus) {
		final LaskentaTyyppi laskentaTyyppi = asLaskentaTyyppi(laskentaAloitus);
		final Integer valinnanvaiheet = asValinnanvaihe(laskentaAloitus
				.getValinnanvaihe());
		final String uuid = laskentaAloitus.getUuid();
		final String hakuOid = laskentaAloitus.getHakuOid();
		final boolean erillishaku = laskentaAloitus.isErillishaku();
		final Collection<HakukohdeJaOrganisaatio> hakukohdeJaOrganisaatio = laskentaAloitus
				.getHakukohdeDtos()
				.stream()
				.map(hk -> new HakukohdeJaOrganisaatio(hk.getHakukohdeOid(), hk
						.getOrganisaatioOid())).collect(Collectors.toList());
		laskentaSupervisor.luoJaKaynnistaLaskenta(uuid, hakuOid,
				laskentaAloitus.isOsittainenLaskenta(), lsup -> {
					return typed.typedActorOf(new TypedProps<LaskentaActor>(
							LaskentaActor.class, new Creator<LaskentaActor>() {
								private static final long serialVersionUID = 8521766139538840217L;

								public LaskentaActor create() throws Exception {
									if (fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi.VALINTARYHMA
											.equals(laskentaAloitus.getTyyppi())) {
										LOG.info("Muodostetaan VALINTARYHMALASKENTA");
										return laskentaActorFactory
												.createValintaryhmaActor(uuid,
														haku,
														parametritDTO,
														erillishaku,
														valinnanvaiheet,
														hakukohdeJaOrganisaatio);
									} else {
										if (LaskentaTyyppi.VALINTAKOELASKENTA
												.equals(laskentaTyyppi)) {
											LOG.info("Muodostetaan VALINTAKOELASKENTA");
											return laskentaActorFactory
													.createValintakoelaskentaActor(
															uuid, haku,
															parametritDTO,
															erillishaku,
															valinnanvaiheet,
															hakukohdeJaOrganisaatio);
										}
										if (LaskentaTyyppi.VALINTALASKENTA
												.equals(laskentaTyyppi)) {
											LOG.info("Muodostetaan VALINTALASKENTA");
											return laskentaActorFactory
													.createValintalaskentaActor(
															uuid, haku,
															parametritDTO,
															erillishaku,
															valinnanvaiheet,
															hakukohdeJaOrganisaatio);
										} else {
											LOG.info(
													"Muodostetaan KAIKKI VAIHEET LASKENTA koska valinnanvaihe oli {} ja valintakoelaskenta ehto {}",
													laskentaAloitus
															.getValinnanvaihe(),
													laskentaAloitus
															.getValintakoelaskenta());
											return laskentaActorFactory
													.createValintalaskentaJaValintakoelaskentaActor(
															uuid, haku,
															parametritDTO,
															erillishaku,
															valinnanvaiheet,
															hakukohdeJaOrganisaatio);
										}
									}
								}
							}));
				});

	}

	@Override
	public List<Laskenta> ajossaOlevatLaskennat() {
		return laskentaSupervisor.ajossaOlevatLaskennat();
	}

	@Override
	public Laskenta haeLaskenta(String uuid) {
		return laskentaSupervisor.haeLaskenta(uuid);
	}

	/**
	 * Tilapainen workaround resurssin valinnanvaiheen normalisointiin.
	 */
	private Integer asValinnanvaihe(Integer valinnanvaihe) {
		if (HAE_KAIKKI_VALINNANVAIHEET.equals(valinnanvaihe)) {
			return null;
		} else {
			return valinnanvaihe;
		}
	}

	/**
	 * Tilapainen workaround resurssin syotteiden normalisointiin
	 */
	private LaskentaTyyppi asLaskentaTyyppi(LaskentaAloitus l) {
		if (Boolean.TRUE.equals(l.getValintakoelaskenta())) {
			return LaskentaTyyppi.VALINTAKOELASKENTA;
		} else {
			if (l.getValinnanvaihe() == null) {
				return LaskentaTyyppi.KAIKKI;
			} else {
				return LaskentaTyyppi.VALINTALASKENTA;
			}
		}
	}

}
