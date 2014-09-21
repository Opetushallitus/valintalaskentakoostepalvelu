package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.LisatiedotPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategia;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuStrategia;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintakoelaskentaPalvelukutsu extends
		AbstraktiLaskentaPalvelukutsu implements LaskentaPalvelukutsu {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintakoelaskentaPalvelukutsu.class);
	private final ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu;
	private final HakemuksetPalvelukutsu hakemuksetPalvelukutsu;
	private final LisatiedotPalvelukutsu lisatiedotPalvelukutsu;
	private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;

	public ValintakoelaskentaPalvelukutsu(String hakukohdeOid,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			LisatiedotPalvelukutsu lisatiedotPalvelukutsu,
			HakemuksetPalvelukutsu hakemuksetPalvelukutsu,
			ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu,
			PalvelukutsuStrategia lisatiedotStrategia,
			PalvelukutsuStrategia hakemuksetStrategia,
			PalvelukutsuStrategia valintaperusteetStrategia) {
		super(hakukohdeOid, Arrays
				.asList(new PalvelukutsuJaPalvelukutsuStrategia(
						lisatiedotPalvelukutsu, lisatiedotStrategia),
						new PalvelukutsuJaPalvelukutsuStrategia(
								hakemuksetPalvelukutsu, hakemuksetStrategia),
						new PalvelukutsuJaPalvelukutsuStrategia(
								valintaperusteetPalvelukutsu,
								valintaperusteetStrategia)));
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
		this.lisatiedotPalvelukutsu = lisatiedotPalvelukutsu;
		this.valintaperusteetPalvelukutsu = valintaperusteetPalvelukutsu;
		this.hakemuksetPalvelukutsu = hakemuksetPalvelukutsu;
	}

	private LaskeDTO muodostaLaskeDTO() {
		return new LaskeDTO(getHakukohdeOid(), muodostaHakemuksetDTO(
				hakemuksetPalvelukutsu.getHakemukset(),
				lisatiedotPalvelukutsu.getLisatiedot()),
				valintaperusteetPalvelukutsu.getValintaperusteet());
	}

	@Override
	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		try {
			aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
				public Peruutettava get() {
					return valintalaskentaAsyncResource
							.valintakokeet(
									muodostaLaskeDTO(),
									laskentaCallback -> {
										takaisinkutsu
												.accept(ValintakoelaskentaPalvelukutsu.this);
									}, failureCallback(takaisinkutsu));
				}
			});
		} catch (Exception e) {
			LOG.error(
					"Valintakoelaskennan palvelukutsun muodostus epaonnistui virheeseen {}",
					e.getMessage());
			failureCallback(takaisinkutsu).accept(e);
		}
		return this;
	}

}
