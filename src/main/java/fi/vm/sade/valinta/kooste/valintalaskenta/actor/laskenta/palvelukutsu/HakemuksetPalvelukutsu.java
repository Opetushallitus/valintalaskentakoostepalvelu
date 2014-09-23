package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class HakemuksetPalvelukutsu extends AbstraktiPalvelukutsu implements
		Palvelukutsu {
	private final static Logger LOG = LoggerFactory
			.getLogger(HakemuksetPalvelukutsu.class);
	private final String hakuOid;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final AtomicReference<List<Hakemus>> hakemukset;

	public HakemuksetPalvelukutsu(String hakuOid,
			HakukohdeJaOrganisaatio hakukohdeOid,
			ApplicationAsyncResource applicationAsyncResource) {
		super(hakukohdeOid.getHakukohdeOid());
		this.hakuOid = hakuOid;
		this.applicationAsyncResource = applicationAsyncResource;
		this.hakemukset = new AtomicReference<>();
	}

	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
			public Peruutettava get() {
				return applicationAsyncResource
						.getApplicationsByOid(
								hakuOid,
								getHakukohdeOid(),
								hakemukset -> {
									if (hakemukset == null) {
										LOG.error("Hakemuksetpalvelu palautti null datajoukon!");
										failureCallback(takaisinkutsu);
										return;
									}
									HakemuksetPalvelukutsu.this.hakemukset
											.set(hakemukset);
									takaisinkutsu
											.accept(HakemuksetPalvelukutsu.this);
								}, failureCallback(takaisinkutsu));
			}
		});
		return this;
	}

	public List<Hakemus> getHakemukset() {
		return hakemukset.get();
	}

}
