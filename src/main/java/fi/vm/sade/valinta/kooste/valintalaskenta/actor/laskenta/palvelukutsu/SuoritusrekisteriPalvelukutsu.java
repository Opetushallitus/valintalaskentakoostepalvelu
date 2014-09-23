package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;

public class SuoritusrekisteriPalvelukutsu extends AbstraktiPalvelukutsu
		implements Palvelukutsu {
	private final static Logger LOG = LoggerFactory
			.getLogger(HakijaryhmatPalvelukutsu.class);
	private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
	private final AtomicReference<List<ValintaperusteetHakijaryhmaDTO>> hakijaryhmat;

	public SuoritusrekisteriPalvelukutsu(String hakukohdeOid,
			ValintaperusteetAsyncResource valintaperusteetAsyncResource) {
		super(hakukohdeOid);
		this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
		this.hakijaryhmat = new AtomicReference<>();
	}

	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
			public Peruutettava get() {
				return valintaperusteetAsyncResource
						.haeHakijaryhmat(
								getHakukohdeOid(),
								hakijaryhmat -> {
									SuoritusrekisteriPalvelukutsu.this.hakijaryhmat
											.set(hakijaryhmat);
									takaisinkutsu
											.accept(SuoritusrekisteriPalvelukutsu.this);
								}, failureCallback(takaisinkutsu));
			}
		});
		return this;
	}

	public List<ValintaperusteetHakijaryhmaDTO> getHakijaryhmat() {
		return hakijaryhmat.get();
	}
}