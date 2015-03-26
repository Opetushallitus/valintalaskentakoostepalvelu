package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.UuidHakukohdeJaOrganisaatio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class SuoritusrekisteriPalvelukutsu extends AbstraktiPalvelukutsu
		implements Palvelukutsu {
	private final static Logger LOG = LoggerFactory
			.getLogger(HakijaryhmatPalvelukutsu.class);
	private final SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;
	private final AtomicReference<List<Oppija>> oppijat;

	public SuoritusrekisteriPalvelukutsu(
			UuidHakukohdeJaOrganisaatio hakukohdeJaOrganisaatio,
			SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
		super(hakukohdeJaOrganisaatio);
		this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
		this.oppijat = new AtomicReference<>();
	}

	@Override
	public void vapautaResurssit() {
		oppijat.set(null);
	}

	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(() ->
			suoritusrekisteriAsyncResource
					.getOppijatByHakukohde(
							getHakukohdeOid(),
							null, // referenssiPvm ensikertalaisuutta varten
							oppijat -> {
								SuoritusrekisteriPalvelukutsu.this.oppijat
										.set(oppijat);
								takaisinkutsu
										.accept(SuoritusrekisteriPalvelukutsu.this);
							}, failureCallback(takaisinkutsu))
		);
		return this;
	}

	public List<Oppija> getOppijat() {
		return oppijat.get();
	}
}