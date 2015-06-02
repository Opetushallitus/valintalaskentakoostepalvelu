package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.SuoritusrekisteriPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategiaImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintalaskentaPalvelukutsu extends AbstraktiLaskentaPalvelukutsu
		implements LaskentaPalvelukutsu {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintakoelaskentaPalvelukutsu.class);
	private final String uuid;
	private final ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu;
	private final HakemuksetPalvelukutsu hakemuksetPalvelukutsu;
	private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
	private final HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu;
	private final SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu;
	private final boolean erillishaku;
	@Override
	public void vapautaResurssit() {
		valintaperusteetPalvelukutsu.vapautaResurssit();
		hakemuksetPalvelukutsu.vapautaResurssit();
		hakijaryhmatPalvelukutsu.vapautaResurssit();
		suoritusrekisteriPalvelukutsu.vapautaResurssit();
	}

	public ValintalaskentaPalvelukutsu(
			HakuV1RDTO haku,
			ParametritDTO parametriDTO,
			boolean erillishaku,
			UuidHakukohdeJaOrganisaatio hakukohdeOid,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			HakemuksetPalvelukutsu hakemuksetPalvelukutsu,
			ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu,
			HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu,
			SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu,
			PalvelukutsuStrategia hakemuksetStrategia,
			PalvelukutsuStrategia valintaperusteetStrategia,
			PalvelukutsuStrategia hakijaryhmatStrategia,
			PalvelukutsuStrategia suoritusrekisteriStrategia) {
		super(haku, parametriDTO, hakukohdeOid,
				Arrays.asList(
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(
								hakemuksetPalvelukutsu, hakemuksetStrategia),
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(
								valintaperusteetPalvelukutsu,
								valintaperusteetStrategia),
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(
								hakijaryhmatPalvelukutsu, hakijaryhmatStrategia),
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(
								suoritusrekisteriPalvelukutsu,
								suoritusrekisteriStrategia)));
		this.uuid = hakukohdeOid.getUuid();
		this.erillishaku = erillishaku;
		this.hakijaryhmatPalvelukutsu = hakijaryhmatPalvelukutsu;
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
		this.valintaperusteetPalvelukutsu = valintaperusteetPalvelukutsu;
		this.hakemuksetPalvelukutsu = hakemuksetPalvelukutsu;
		this.suoritusrekisteriPalvelukutsu = suoritusrekisteriPalvelukutsu;
	}

	private LaskeDTO muodostaLaskeDTO() {
		LaskeDTO l = new LaskeDTO(
				uuid,
				erillishaku,
				getHakukohdeOid(),
				muodostaHakemuksetDTO(
						getHakukohdeOid(),
						hakemuksetPalvelukutsu.getHakemukset(),
						suoritusrekisteriPalvelukutsu.getOppijat()),
				valintaperusteetPalvelukutsu.getValintaperusteet(),
				hakijaryhmatPalvelukutsu.getHakijaryhmat());
		vapautaResurssit();
		return l;
	}

	@Override
	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		try {
			aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
				public Peruutettava get() {
					return valintalaskentaAsyncResource
							.laske(muodostaLaskeDTO(),
									laskentaCallback -> {
										takaisinkutsu
												.accept(ValintalaskentaPalvelukutsu.this);
									}, failureCallback(takaisinkutsu));
				}
			});
		} catch (Exception e) {
			LOG.error(
					"ValintalaskentaPalvelukutsu palvelukutsun muodostus epaonnistui virheeseen {}",
					e.getMessage());
			failureCallback(takaisinkutsu).accept(e);
		}
		return this;
	}

}