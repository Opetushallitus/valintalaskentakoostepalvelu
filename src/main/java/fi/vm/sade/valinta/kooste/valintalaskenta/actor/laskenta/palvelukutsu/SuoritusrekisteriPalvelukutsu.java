package fi.vm.sade.valinta.kooste.valintalaskenta.actor.laskenta.palvelukutsu;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
	private final String organisaatioOid;

	public SuoritusrekisteriPalvelukutsu(
			HakukohdeJaOrganisaatio hakukohdeJaOrganisaatio,
			SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
		super(hakukohdeJaOrganisaatio.getHakukohdeOid());
		this.organisaatioOid = hakukohdeJaOrganisaatio.getOrganisaatioOid();
		if (organisaatioOid == null) {
			LOG.error(
					"Suoritusrekisteripalvelukutsua ei voi alustaa ilman organisaation tunnistetta! Hakukohteella {} ei ole organisaation tunnistetta!",
					hakukohdeJaOrganisaatio.getHakukohdeOid());
			throw new NullPointerException(
					"Suoritusrekisteripalvelukutsua ei voi alustaa ilman organisaation tunnistetta! Hakukohteella "
							+ hakukohdeJaOrganisaatio.getHakukohdeOid()
							+ " ei ole organisaation tunnistetta!");
		}
		this.suoritusrekisteriAsyncResource = suoritusrekisteriAsyncResource;
		this.oppijat = new AtomicReference<>();
	}

	public Palvelukutsu teePalvelukutsu(Consumer<Palvelukutsu> takaisinkutsu) {
		aloitaPalvelukutsuJosPalvelukutsuaEiOlePeruutettu(new Supplier<Peruutettava>() {
			public Peruutettava get() {
				return suoritusrekisteriAsyncResource
						.getOppijatByOrganisaatio(
								organisaatioOid,
								oppijat -> {
									SuoritusrekisteriPalvelukutsu.this.oppijat
											.set(oppijat);
									takaisinkutsu
											.accept(SuoritusrekisteriPalvelukutsu.this);
								},
								failu -> {
									LOG.error(
											"Suoritusrekisterikutsu epaonnistui hakukohteelle {} ja organisaatiolle {}!",
											getHakukohdeOid(), organisaatioOid);
									SuoritusrekisteriPalvelukutsu.this.oppijat
											.set(Collections.emptyList());
									takaisinkutsu
											.accept(SuoritusrekisteriPalvelukutsu.this);
								});
			}
		});
		return this;
	}

	public List<Oppija> getOppijat() {
		return oppijat.get();
	}
}