package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
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
public class ValintakoelaskentaPalvelukutsu extends
		AbstraktiLaskentaPalvelukutsu implements LaskentaPalvelukutsu {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintakoelaskentaPalvelukutsu.class);
	private final String uuid;
	private final ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu;
	private final HakemuksetPalvelukutsu hakemuksetPalvelukutsu;
	private final SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu;
	private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
	private final boolean erillishaku;
	
	public ValintakoelaskentaPalvelukutsu(
			HakuV1RDTO haku,
			ParametritDTO parametritDTO,
			boolean erillishaku,
			UuidHakukohdeJaOrganisaatio hakukohdeOid,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			HakemuksetPalvelukutsu hakemuksetPalvelukutsu,
			ValintaperusteetPalvelukutsu valintaperusteetPalvelukutsu,
			SuoritusrekisteriPalvelukutsu suoritusrekisteriPalvelukutsu,
			PalvelukutsuStrategia hakemuksetStrategia,
			PalvelukutsuStrategia valintaperusteetStrategia,
			PalvelukutsuStrategia suoritusrekisteriStrategia) {
		super(haku, parametritDTO, hakukohdeOid, Arrays
				.asList(new PalvelukutsuJaPalvelukutsuStrategiaImpl(
						hakemuksetPalvelukutsu, hakemuksetStrategia),
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(
								valintaperusteetPalvelukutsu,
								valintaperusteetStrategia),
						new PalvelukutsuJaPalvelukutsuStrategiaImpl(
								suoritusrekisteriPalvelukutsu,
								suoritusrekisteriStrategia)));
		this.uuid = hakukohdeOid.getUuid();
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
		this.valintaperusteetPalvelukutsu = valintaperusteetPalvelukutsu;
		this.hakemuksetPalvelukutsu = hakemuksetPalvelukutsu;
		this.suoritusrekisteriPalvelukutsu = suoritusrekisteriPalvelukutsu;
		this.erillishaku = erillishaku;
	}

	@Override
	public void vapautaResurssit() {
		valintaperusteetPalvelukutsu.vapautaResurssit();
		hakemuksetPalvelukutsu.vapautaResurssit();
		suoritusrekisteriPalvelukutsu.vapautaResurssit();
	}

	private LaskeDTO muodostaLaskeDTO() {
		List<Hakemus> hakemukset = hakemuksetPalvelukutsu.getHakemukset();
		List<ValintaperusteetDTO> valintaperusteet = valintaperusteetPalvelukutsu
				.getValintaperusteet();
		if (hakemukset == null) {
			throw new NullPointerException("Hakemukset oli null dataa!");
		}
		if (valintaperusteet == null) {
			throw new NullPointerException("Valintaperusteet oli null dataa!");
		}
		return new LaskeDTO(
				uuid,
				erillishaku,
				getHakukohdeOid(),
				muodostaHakemuksetDTO(getHakukohdeOid(), hakemukset, suoritusrekisteriPalvelukutsu.getOppijat()),
				valintaperusteet);
	}

	@Override
	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		try {
			final LaskeDTO laskeDTO = muodostaLaskeDTO();
			try {
				aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
					public Peruutettava get() {
						return valintalaskentaAsyncResource
								.valintakokeet(
										laskeDTO,
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
		} catch (Exception e) {
			LOG.error(
					"LaskeDTO:n muodostus epaonnistui ValintakoelaskentaPalvelukutsulle: {}",
					e.getMessage());
			failureCallback(takaisinkutsu).accept(e);
		}
		return this;
	}

}
