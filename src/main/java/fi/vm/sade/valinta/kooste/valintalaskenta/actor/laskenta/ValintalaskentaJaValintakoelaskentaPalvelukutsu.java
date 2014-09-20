package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.LisatiedotPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategia;
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
			.getLogger(ValintakoelaskentaPalvelukutsu.class);
	private final ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu;
	private final HakemuksetPalvelukutsu hakemuksetPalvelukutsu;
	private final LisatiedotPalvelukutsu lisatiedotPalvelukutsu;
	private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
	private final HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu;

	public ValintalaskentaJaValintakoelaskentaPalvelukutsu(String hakukohdeOid,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			LisatiedotPalvelukutsu lisatiedotPalvelukutsu,
			HakemuksetPalvelukutsu hakemuksetPalvelukutsu,
			ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu,
			HakijaryhmatPalvelukutsu hakijaryhmatPalvelukutsu,
			PalvelukutsuStrategia lisatiedotStrategia,
			PalvelukutsuStrategia hakemuksetStrategia,
			PalvelukutsuStrategia valintaperusteetStrategia,
			PalvelukutsuStrategia hakijaryhmatStrategia) {
		super(
				hakukohdeOid,
				Arrays.asList(
						new PalvelukutsuJaPalvelukutsuStrategia(
								lisatiedotPalvelukutsu, lisatiedotStrategia),
						new PalvelukutsuJaPalvelukutsuStrategia(
								hakemuksetPalvelukutsu, hakemuksetStrategia),
						new PalvelukutsuJaPalvelukutsuStrategia(
								valintaperusteetPalvelukutsu,
								valintaperusteetStrategia),
						new PalvelukutsuJaPalvelukutsuStrategia(
								hakijaryhmatPalvelukutsu, hakijaryhmatStrategia)));
		this.hakijaryhmatPalvelukutsu = hakijaryhmatPalvelukutsu;
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
		this.lisatiedotPalvelukutsu = lisatiedotPalvelukutsu;
		this.valintaperusteetPalvelukutsu = valintaperusteetPalvelukutsu;
		this.hakemuksetPalvelukutsu = hakemuksetPalvelukutsu;
	}

	private LaskeDTO muodostaLaskeDTO() {
		return new LaskeDTO(getHakukohdeOid(), muodostaHakemuksetDTO(
				hakemuksetPalvelukutsu.getHakemukset(),
				lisatiedotPalvelukutsu.getLisatiedot()),
				valintaperusteetPalvelukutsu.getValintaperusteet(),
				hakijaryhmatPalvelukutsu.getHakijaryhmat());
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