package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakemuksetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.HakijaryhmatPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.Palvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.SuoritusrekisteriPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu.ValintaperusteetPalvelukutsu;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.strategia.PalvelukutsuJaPalvelukutsuStrategiaImpl;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintaryhmatKatenoivaValintalaskentaPalvelukutsu extends
		AbstraktiLaskentaPalvelukutsu implements LaskentaPalvelukutsu {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintakoelaskentaPalvelukutsu.class);
	private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
	private final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakemuksetPalvelukutsu>> hakemuksetPalvelukutsut;
	private final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<ValintaperusteetPalvelukutsu>> valintaperusteetPalvelukutsut;
	private final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakijaryhmatPalvelukutsu>> hakijaryhmatPalvelukutsut;
	private final List<PalvelukutsuJaPalvelukutsuStrategiaImpl<SuoritusrekisteriPalvelukutsu>> suoritusrekisteriPalvelukutsut;
	private final List<ValintaryhmaPalvelukutsuYhdiste> valintaryhmaPalvelukutsuYhdiste;
	private final AtomicReference<Runnable> callback = new AtomicReference<>();

	@SuppressWarnings("unchecked")
	public ValintaryhmatKatenoivaValintalaskentaPalvelukutsu(
			HakukohdeJaOrganisaatio hakukohdeOid,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			List<ValintaryhmaPalvelukutsuYhdiste> valintaryhmaPalvelukutsuYhdiste,
			List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakemuksetPalvelukutsu>> hakemuksetPalvelukutsut,
			List<PalvelukutsuJaPalvelukutsuStrategiaImpl<ValintaperusteetPalvelukutsu>> valintaperusteetPalvelukutsut,
			List<PalvelukutsuJaPalvelukutsuStrategiaImpl<HakijaryhmatPalvelukutsu>> hakijaryhmatPalvelukutsut,
			List<PalvelukutsuJaPalvelukutsuStrategiaImpl<SuoritusrekisteriPalvelukutsu>> suoritusrekisteriPalvelukutsut) {
		super(hakukohdeOid.getHakukohdeOid(), Lists.newArrayList(Iterables
				.concat(hakemuksetPalvelukutsut, valintaperusteetPalvelukutsut,
						hakijaryhmatPalvelukutsut,
						suoritusrekisteriPalvelukutsut)));
		this.valintaryhmaPalvelukutsuYhdiste = valintaryhmaPalvelukutsuYhdiste;
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
		this.hakemuksetPalvelukutsut = hakemuksetPalvelukutsut;
		this.valintaperusteetPalvelukutsut = valintaperusteetPalvelukutsut;
		this.hakijaryhmatPalvelukutsut = hakijaryhmatPalvelukutsut;
		this.suoritusrekisteriPalvelukutsut = suoritusrekisteriPalvelukutsut;
	}

	public void setCallback(Runnable r) {
		callback.set(r);
	}

	protected void yksiVaiheValmistui() {
		callback.get().run();
	}

	private List<LaskeDTO> muodostaLaskeDTOs() {
		return valintaryhmaPalvelukutsuYhdiste
				.stream()
				.map(y -> {
					try {
						return new LaskeDTO(y.getHakukohdeOid(),
								muodostaHakemuksetDTO(y
										.getHakemuksetPalvelukutsu()
										.getHakemukset()), y
										.getValintaperusteetPalvelukutsu()
										.getValintaperusteet(), y
										.getHakijaryhmatPalvelukutsu()
										.getHakijaryhmat());
					} catch (Exception e) {
						LOG.error("LaskeDTO:n muodostaminen epaonnistui {}",
								e.getMessage());
						throw e;
					}
				}).collect(Collectors.toList());
	}

	@Override
	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		try {
			aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
				public Peruutettava get() {
					return valintalaskentaAsyncResource
							.laskeJaSijoittele(
									muodostaLaskeDTOs(),
									laskentaCallback -> {
										takaisinkutsu
												.accept(ValintaryhmatKatenoivaValintalaskentaPalvelukutsu.this);
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