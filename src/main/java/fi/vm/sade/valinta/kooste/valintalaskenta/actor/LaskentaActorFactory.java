package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintakoelaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintalaskentaJaValintakoelaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintalaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintaryhmaPalvelukutsuYhdiste;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintaryhmatKatenoivaValintalaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.SuoritusrekisteriPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategiaImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.YksiPalvelukutsuKerrallaPalvelukutsuStrategia;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class LaskentaActorFactory {

	private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
	private final LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource;
	private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
	private final LaskentaSupervisor laskentaSupervisor;

	public LaskentaActorFactory(
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			ValintaperusteetAsyncResource valintaperusteetAsyncResource,
			LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource,
			SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
			LaskentaSupervisor laskentaSupervisor) {
		this.laskentaSupervisor = laskentaSupervisor;
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
		this.applicationAsyncResource = applicationAsyncResource;
		this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
		this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
		this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
	}

	public LaskentaActor createValintaryhmaActor(final String uuid,
			final String hakuOid,
			ParametritDTO parametritDTO,
			boolean erillishaku,
			final Integer valinnanvaihe,
			final Collection<HakukohdeJaOrganisaatio> hakukohdeOids) {
		final PalvelukutsuStrategia laskentaStrategia = createStrategia();
		final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
		final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
		final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
		final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
		final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
				laskentaStrategia, valintaperusteetStrategia,
				hakemuksetStrategia, hakijaryhmatStrategia,
				suoritusrekisteriStrategia);

		final List<ValintaryhmaPalvelukutsuYhdiste> valintaryhmaPalvelukutsuYhdiste = Lists
				.newArrayList();
		final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakemuksetPalvelukutsu>> hakemuksetPalvelukutsut = Lists
				.newArrayList();
		final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<ValintaperusteetPalvelukutsu>> valintaperusteetPalvelukutsut = Lists
				.newArrayList();
		final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakijaryhmatPalvelukutsu>> hakijaryhmatPalvelukutsut = Lists
				.newArrayList();
		final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<SuoritusrekisteriPalvelukutsu>> suoritusrekisteriPalvelukutsut = Lists
				.newArrayList();
		hakukohdeOids
				.forEach(hk -> {
					UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(uuid,hk);
					ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu = new ValintaperusteetPalvelukutsu(
							uudiHk, valinnanvaihe, valintaperusteetAsyncResource);
					HakemuksetPalvelukutsu hakemuksetPalvelukutsu = new HakemuksetPalvelukutsu(
							hakuOid, uudiHk, applicationAsyncResource);
					SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu = new SuoritusrekisteriPalvelukutsu(
							uudiHk, suoritusrekisteriAsyncResource);
					HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu = new HakijaryhmatPalvelukutsu(
							uudiHk, valintaperusteetAsyncResource);

					hakemuksetPalvelukutsut
							.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(
									hakemuksetPalvelukutsu, hakemuksetStrategia));

					valintaperusteetPalvelukutsut
							.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(
									valintaperusteetPalvelukutsu,
									valintaperusteetStrategia));

					hakijaryhmatPalvelukutsut
							.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(
									hakijaryhmatPalvelukutsu,
									hakijaryhmatStrategia));

					suoritusrekisteriPalvelukutsut
							.add(new PalvelukutsuJaPalvelukutsuStrategiaImpl<>(
									suoritusrekisteriPalvelukutsu,
									suoritusrekisteriStrategia));

					valintaryhmaPalvelukutsuYhdiste
							.add(new ValintaryhmaPalvelukutsuYhdiste(hk
									.getHakukohdeOid(), hakemuksetPalvelukutsu,
									valintaperusteetPalvelukutsu,
									hakijaryhmatPalvelukutsu,
									suoritusrekisteriPalvelukutsu));

				});

		ValintaryhmatKatenoivaValintalaskentaPalvelukutsu laskentaPk = new ValintaryhmatKatenoivaValintalaskentaPalvelukutsu(
				parametritDTO,
				erillishaku,
				new UuidHakukohdeJaOrganisaatio(uuid, new HakukohdeJaOrganisaatio("Valintaryhmalaskenta("+hakukohdeOids.size()+"kohdetta)",
						"kaikkiOrganisaatiot")), valintalaskentaAsyncResource,
				valintaryhmaPalvelukutsuYhdiste, hakemuksetPalvelukutsut,
				valintaperusteetPalvelukutsut, hakijaryhmatPalvelukutsut,
				suoritusrekisteriPalvelukutsut);

		ValintaryhmaLaskentaActorImpl v = new ValintaryhmaLaskentaActorImpl(
				laskentaSupervisor, uuid, hakuOid, laskentaPk, strategiat,
				laskentaStrategia, laskentaSeurantaAsyncResource);
		laskentaPk.setCallback(v);
		return v;
	}

	public LaskentaActor createValintakoelaskentaActor(final String uuid,
			final String hakuOid,
			ParametritDTO parametritDTO,
			boolean erillishaku,
			final Integer valinnanvaihe,
			final Collection<HakukohdeJaOrganisaatio> hakukohdeOids) {
		final PalvelukutsuStrategia laskentaStrategia = createStrategia();
		final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
		final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
		final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
		final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
				laskentaStrategia, valintaperusteetStrategia,
				hakemuksetStrategia, suoritusrekisteriStrategia);
		final Collection<LaskentaPalvelukutsu> palvelukutsut = hakukohdeOids
				.parallelStream()
				.map(hakukohdeOid -> {

					UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(uuid, hakukohdeOid);
					return new ValintakoelaskentaPalvelukutsu(
							parametritDTO,
							erillishaku,
							uudiHk, valintalaskentaAsyncResource,
							new HakemuksetPalvelukutsu(hakuOid, uudiHk,
									applicationAsyncResource),
							new ValintaperusteetPalvelukutsu(uudiHk,
									valinnanvaihe, valintaperusteetAsyncResource),
							new SuoritusrekisteriPalvelukutsu(uudiHk,
									suoritusrekisteriAsyncResource),
							hakemuksetStrategia, valintaperusteetStrategia,
							suoritusrekisteriStrategia);
				})
		.collect(Collectors.toList());
		return new LaskentaActorImpl(laskentaSupervisor, uuid, hakuOid,
				palvelukutsut, strategiat, laskentaStrategia,
				laskentaSeurantaAsyncResource);
	}

	public LaskentaActor createValintalaskentaActor(final String uuid,
			final String hakuOid,
			ParametritDTO parametritDTO,
			boolean erillishaku,
			final Integer valinnanvaihe,
			final Collection<HakukohdeJaOrganisaatio> hakukohdeOids) {
		final PalvelukutsuStrategia laskentaStrategia = createStrategia();
		final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
		final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
		final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
		final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
		final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
				laskentaStrategia, valintaperusteetStrategia,
				hakemuksetStrategia, hakijaryhmatStrategia,
				suoritusrekisteriStrategia);
		final Collection<LaskentaPalvelukutsu> palvelukutsut = hakukohdeOids
				.parallelStream()
				.map(hakukohdeOid -> {

					UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(uuid, hakukohdeOid);
					return new ValintalaskentaPalvelukutsu(
							parametritDTO,
							erillishaku,
							uudiHk, valintalaskentaAsyncResource,
							new HakemuksetPalvelukutsu(hakuOid, uudiHk,
									applicationAsyncResource),
							new ValintaperusteetPalvelukutsu(uudiHk,
									valinnanvaihe, valintaperusteetAsyncResource),
							new HakijaryhmatPalvelukutsu(uudiHk,
									valintaperusteetAsyncResource),
							new SuoritusrekisteriPalvelukutsu(uudiHk,
									suoritusrekisteriAsyncResource),
							hakemuksetStrategia, valintaperusteetStrategia,
							hakijaryhmatStrategia, suoritusrekisteriStrategia);
				})
				.collect(Collectors.toList());
		return new LaskentaActorImpl(laskentaSupervisor, uuid, hakuOid,
				palvelukutsut, strategiat, laskentaStrategia,
				laskentaSeurantaAsyncResource);
	}

	public LaskentaActor createValintalaskentaJaValintakoelaskentaActor(
			final String uuid, final String hakuOid,
			ParametritDTO parametritDTO,
			boolean erillishaku,
			final Integer valinnanvaihe,
			final Collection<HakukohdeJaOrganisaatio> hakukohdeOids) {
		final PalvelukutsuStrategia laskentaStrategia = createStrategia();
		final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
		final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
		final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
		final PalvelukutsuStrategia suoritusrekisteriStrategia = createStrategia();
		final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
				laskentaStrategia, valintaperusteetStrategia,
				hakemuksetStrategia, hakijaryhmatStrategia,
				suoritusrekisteriStrategia);
		final Collection<LaskentaPalvelukutsu> palvelukutsut = hakukohdeOids
				.parallelStream()
				.map(hakukohdeOid -> {

							UuidHakukohdeJaOrganisaatio uudiHk = new UuidHakukohdeJaOrganisaatio(uuid, hakukohdeOid);
							return new ValintalaskentaJaValintakoelaskentaPalvelukutsu(
									parametritDTO,
									erillishaku,
									uudiHk, valintalaskentaAsyncResource,
									new HakemuksetPalvelukutsu(hakuOid, uudiHk,
											applicationAsyncResource),
									new ValintaperusteetPalvelukutsu(uudiHk,
											valinnanvaihe, valintaperusteetAsyncResource),
									new HakijaryhmatPalvelukutsu(uudiHk,
											valintaperusteetAsyncResource),
									new SuoritusrekisteriPalvelukutsu(uudiHk,
											suoritusrekisteriAsyncResource),
									hakemuksetStrategia, valintaperusteetStrategia,
									hakijaryhmatStrategia, suoritusrekisteriStrategia);
						}
				)
				.collect(Collectors.toList());
		return new LaskentaActorImpl(laskentaSupervisor, uuid, hakuOid,
				palvelukutsut, strategiat, laskentaStrategia,
				laskentaSeurantaAsyncResource);
	}

	private PalvelukutsuStrategia createStrategia() {
		return new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
	}
}
