package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class ValintaperusteetPalvelukutsu extends AbstraktiPalvelukutsu
		implements Palvelukutsu {
	private final Logger LOG = LoggerFactory
			.getLogger(ValintaperusteetPalvelukutsu.class);
	private final Integer valinnanVaiheJarjestysluku;
	private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
	private final AtomicReference<List<ValintaperusteetDTO>> valintaperusteet;

	public ValintaperusteetPalvelukutsu(HakukohdeJaOrganisaatio hakukohdeOid,
			Integer valinnanVaiheJarjestysluku,
			ValintaperusteetAsyncResource valintaperusteetAsyncResource) {
		super(hakukohdeOid.getHakukohdeOid());
		this.valinnanVaiheJarjestysluku = valinnanVaiheJarjestysluku;
		this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
		this.valintaperusteet = new AtomicReference<>();
	}

	@Override
	public void vapautaResurssit() {
		valintaperusteet.set(null);
	}

	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
			public Peruutettava get() {
				return valintaperusteetAsyncResource
						.haeValintaperusteet(
								getHakukohdeOid(),
								valinnanVaiheJarjestysluku,
								valintaperusteet -> {
									if (valintaperusteet == null) {
										LOG.error("Valintaperusteetpalvelu palautti null datajoukon!");
										failureCallback(takaisinkutsu);
										return;
									}
									ValintaperusteetPalvelukutsu.this.valintaperusteet
											.set(valintaperusteet);
									takaisinkutsu
											.accept(ValintaperusteetPalvelukutsu.this);
								}, failureCallback(takaisinkutsu));
			}
		});
		return this;
	}

	public List<ValintaperusteetDTO> getValintaperusteet() {
		return valintaperusteet.get();
	}

}