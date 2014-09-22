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
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class LisatiedotPalvelukutsu extends AbstraktiPalvelukutsu implements
		Palvelukutsu {
	private final static Logger LOG = LoggerFactory
			.getLogger(LisatiedotPalvelukutsu.class);
	private final String hakuOid;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final AtomicReference<List<ApplicationAdditionalDataDTO>> lisatiedot;

	public LisatiedotPalvelukutsu(String hakuOid, String hakukohdeOid,
			ApplicationAsyncResource applicationAsyncResource) {
		super(hakukohdeOid);
		this.hakuOid = hakuOid;
		this.applicationAsyncResource = applicationAsyncResource;
		this.lisatiedot = new AtomicReference<>();
	}

	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
			public Peruutettava get() {
				return applicationAsyncResource
						.getApplicationAdditionalData(
								hakuOid,
								getHakukohdeOid(),
								lisatiedot -> {
									try {
										if (lisatiedot == null) {
											LOG.error("Lisatiedotpalvelu palautti null datajoukon!");
											failureCallback(takaisinkutsu);
											return;
										}
										LisatiedotPalvelukutsu.this.lisatiedot
												.set(lisatiedot);
										takaisinkutsu
												.accept(LisatiedotPalvelukutsu.this);
									} catch (Exception e) {
										LOG.error(
												"Takaisinkutsu epaonnistui lisatietojen palvelukutsussa! {}",
												e.getMessage());
									}
								}, failureCallback(takaisinkutsu));
			}
		});
		return this;
	}

	public List<ApplicationAdditionalDataDTO> getLisatiedot() {
		return lisatiedot.get();
	}

}
