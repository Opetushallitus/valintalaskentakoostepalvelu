package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.LisatiedotPalvelukutsu;
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
public class ValintalaskentaJaValintakoelaskentaPalvelukutsu extends
		AbstraktiLaskentaPalvelukutsu implements LaskentaPalvelukutsu {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaJaValintakoelaskentaPalvelukutsu.class);
	private final ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu;
	private final HakemuksetPalvelukutsu hakemuksetPalvelukutsu;
	private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
	private final HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu;
	private final SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu;

	public ValintalaskentaJaValintakoelaskentaPalvelukutsu(
			HakukohdeJaOrganisaatio hakukohdeOid,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			HakemuksetPalvelukutsu hakemuksetPalvelukutsu,
			ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu,
			HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu,
			SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu,
			PalvelukutsuStrategia hakemuksetStrategia,
			PalvelukutsuStrategia valintaperusteetStrategia,
			PalvelukutsuStrategia hakijaryhmatStrategia,
			PalvelukutsuStrategia suoritusrekisteriStrategia) {
		super(
				hakukohdeOid.getHakukohdeOid(),
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
		this.hakijaryhmatPalvelukutsu = hakijaryhmatPalvelukutsu;
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
		this.valintaperusteetPalvelukutsu = valintaperusteetPalvelukutsu;
		this.hakemuksetPalvelukutsu = hakemuksetPalvelukutsu;
		this.suoritusrekisteriPalvelukutsu = suoritusrekisteriPalvelukutsu;
	}

	private LaskeDTO muodostaLaskeDTO() {
		try {
			return new LaskeDTO(getHakukohdeOid(),
					muodostaHakemuksetDTO(hakemuksetPalvelukutsu
							.getHakemukset()),
					valintaperusteetPalvelukutsu.getValintaperusteet(),
					hakijaryhmatPalvelukutsu.getHakijaryhmat());
		} catch (Exception e) {
			LOG.error("LaskeDTO:n muodostaminen epaonnistui {}", e.getMessage());
			throw e;
		}
	}

	@Override
	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		try {
			aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
				public Peruutettava get() {
					return valintalaskentaAsyncResource
							.laskeKaikki(
									muodostaLaskeDTO(),
									laskentaCallback -> {
										takaisinkutsu
												.accept(ValintalaskentaJaValintakoelaskentaPalvelukutsu.this);
									}, failureCallback(takaisinkutsu));
				}
			});
		} catch (Exception e) {
			LOG.error(
					"ValintalaskentaJaValintakoelaskentaPalvelukutsu palvelukutsun muodostus epaonnistui virheeseen {}",
					e.getMessage());
			failureCallback(takaisinkutsu).accept(e);
		}
		return this;
	}

}