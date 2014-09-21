package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.LaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintakoelaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintalaskentaJaValintakoelaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.ValintalaskentaPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.LisatiedotPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
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
	private final LaskentaSupervisor laskentaSupervisor;

	public LaskentaActorFactory(
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			ApplicationAsyncResource applicationAsyncResource,
			ValintaperusteetAsyncResource valintaperusteetAsyncResource,
			LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource,
			LaskentaSupervisor laskentaSupervisor) {
		this.laskentaSupervisor = laskentaSupervisor;
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
		this.applicationAsyncResource = applicationAsyncResource;
		this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
		this.laskentaSeurantaAsyncResource = laskentaSeurantaAsyncResource;
	}

	public LaskentaActor createValintakoelaskentaActor(final String uuid,
			final String hakuOid, final Integer valinnanvaihe,
			final Collection<String> hakukohdeOids) {
		final PalvelukutsuStrategia laskentaStrategia = createStrategia();
		final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
		final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
		final PalvelukutsuStrategia lisatiedotStrategia = createStrategia();
		final Collection<PalvelukutsuStrategia> strategiat = Arrays.asList(
				laskentaStrategia, valintaperusteetStrategia,
				hakemuksetStrategia, lisatiedotStrategia);
		final Collection<LaskentaPalvelukutsu> palvelukutsut = hakukohdeOids
				.parallelStream()
				.map(hakukohdeOid -> new ValintakoelaskentaPalvelukutsu(
						hakukohdeOid, valintalaskentaAsyncResource,
						new LisatiedotPalvelukutsu(hakuOid, hakukohdeOid,
								applicationAsyncResource),
						new HakemuksetPalvelukutsu(hakuOid, hakukohdeOid,
								applicationAsyncResource),
						new ValintaperusteetPalvelukutsu(hakukohdeOid,
								valinnanvaihe, valintaperusteetAsyncResource),
						lisatiedotStrategia, hakemuksetStrategia,
						valintaperusteetStrategia))
				.collect(Collectors.toList());
		return new LaskentaActorImpl(laskentaSupervisor, uuid, hakuOid,
				palvelukutsut, strategiat, laskentaStrategia,
				laskentaSeurantaAsyncResource);
	}

	public LaskentaActor createValintalaskentaActor(final String uuid,
			final String hakuOid, final Integer valinnanvaihe,
			final Collection<String> hakukohdeOids) {
		final PalvelukutsuStrategia laskentaStrategia = createStrategia();
		final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
		final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
		final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
		final PalvelukutsuStrategia lisatiedotStrategia = createStrategia();
		final Collection<PalvelukutsuStrategia> strategiat = Arrays
				.asList(laskentaStrategia, valintaperusteetStrategia,
						hakemuksetStrategia, lisatiedotStrategia,
						hakijaryhmatStrategia);
		final Collection<LaskentaPalvelukutsu> palvelukutsut = hakukohdeOids
				.parallelStream()
				.map(hakukohdeOid -> new ValintalaskentaPalvelukutsu(
						hakukohdeOid, valintalaskentaAsyncResource,
						new LisatiedotPalvelukutsu(hakuOid, hakukohdeOid,
								applicationAsyncResource),
						new HakemuksetPalvelukutsu(hakuOid, hakukohdeOid,
								applicationAsyncResource),
						new ValintaperusteetPalvelukutsu(hakukohdeOid,
								valinnanvaihe, valintaperusteetAsyncResource),
						new HakijaryhmatPalvelukutsu(hakukohdeOid,
								valintaperusteetAsyncResource),
						lisatiedotStrategia, hakemuksetStrategia,
						valintaperusteetStrategia, hakijaryhmatStrategia))
				.collect(Collectors.toList());
		return new LaskentaActorImpl(laskentaSupervisor, uuid, hakuOid,
				palvelukutsut, strategiat, laskentaStrategia,
				laskentaSeurantaAsyncResource);
	}

	public LaskentaActor createValintalaskentaJaValintakoelaskentaActor(
			final String uuid, final String hakuOid,
			final Integer valinnanvaihe, final Collection<String> hakukohdeOids) {
		final PalvelukutsuStrategia laskentaStrategia = createStrategia();
		final PalvelukutsuStrategia valintaperusteetStrategia = createStrategia();
		final PalvelukutsuStrategia hakemuksetStrategia = createStrategia();
		final PalvelukutsuStrategia hakijaryhmatStrategia = createStrategia();
		final PalvelukutsuStrategia lisatiedotStrategia = createStrategia();
		final Collection<PalvelukutsuStrategia> strategiat = Arrays
				.asList(laskentaStrategia, valintaperusteetStrategia,
						hakemuksetStrategia, lisatiedotStrategia,
						hakijaryhmatStrategia);
		final Collection<LaskentaPalvelukutsu> palvelukutsut = hakukohdeOids
				.parallelStream()
				.map(hakukohdeOid -> new ValintalaskentaJaValintakoelaskentaPalvelukutsu(
						hakukohdeOid, valintalaskentaAsyncResource,
						new LisatiedotPalvelukutsu(hakuOid, hakukohdeOid,
								applicationAsyncResource),
						new HakemuksetPalvelukutsu(hakuOid, hakukohdeOid,
								applicationAsyncResource),
						new ValintaperusteetPalvelukutsu(hakukohdeOid,
								valinnanvaihe, valintaperusteetAsyncResource),
						new HakijaryhmatPalvelukutsu(hakukohdeOid,
								valintaperusteetAsyncResource),
						lisatiedotStrategia, hakemuksetStrategia,
						valintaperusteetStrategia, hakijaryhmatStrategia))
				.collect(Collectors.toList());
		return new LaskentaActorImpl(laskentaSupervisor, uuid, hakuOid,
				palvelukutsut, strategiat, laskentaStrategia,
				laskentaSeurantaAsyncResource);
	}

	private PalvelukutsuStrategia createStrategia() {
		return new YksiPalvelukutsuKerrallaPalvelukutsuStrategia();
	}
}
