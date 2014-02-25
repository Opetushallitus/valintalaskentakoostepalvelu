package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Lists;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Työ voi valmistua kuuntelijoita rekisteröidessä tapauksessa jossa
 *         kaikki resurssit on jo ehditty hakemaan. Käytännössä näin ei
 *         kuitenkaan koskaan tapahdu sillä vähintäänkin valintaperusteet on
 *         haettava.
 */
public class ValintalaskentaTyo extends AbstraktiTyo {

	private final AtomicReference<List<ValintaperusteetTyyppi>> valintaperusteet;
	private final List<HakemusTyyppi> hakemustyypit;

	public ValintalaskentaTyo() {
		this.hakemustyypit = Collections.synchronizedList(Lists
				.<HakemusTyyppi> newArrayList());
		this.valintaperusteet = new AtomicReference<List<ValintaperusteetTyyppi>>(
				null);

	}

	public List<ValintaperusteetTyyppi> getValintaperusteet() {
		return valintaperusteet.get();
	}

	public List<HakemusTyyppi> getHakemustyypit() {
		return hakemustyypit;
	}

	public ValintalaskentaTyo rekisteroiKuuntelijat(
			ValintaperusteetTyo valintaperusteetEsitieto,
			Collection<HakemusTyo> hakemustyyppiEsitiedot) {
		if (hakemustyyppiEsitiedot == null || hakemustyyppiEsitiedot.isEmpty()) {
			throw new RuntimeException(
					"Hakukohdetyotä ei tarvitse luoda jos hakukohteella ei ole hakemuksia!");
		}
		final AtomicInteger laskuri = new AtomicInteger(
				1 + hakemustyyppiEsitiedot.size());
		if (null != valintaperusteetEsitieto
				.rekisteroiKuuntelija(new EsitiedonKuuntelija<List<ValintaperusteetTyyppi>>() {
					public ValintalaskentaTyo esitietoSaatavilla(
							List<ValintaperusteetTyyppi> v) {
						if (!ValintalaskentaTyo.this.valintaperusteet
								.compareAndSet(null, v)) {
							throw new RuntimeException(
									"Valintaperusteet merkittiin kahdesti valmistuneeksi esitiedoksi hakukohdetyölle!");
						}
						int tyotaJaljella = laskuri.decrementAndGet();
						if (tyotaJaljella == 0) {
							return ValintalaskentaTyo.this;
						} else if (tyotaJaljella < 0) {
							throw new RuntimeException(
									"Hakukohdetyölle on valmistunut enemmän töitä kuin odotettiin valmistuvaksi!");
						} else {
							return null;
						}
					}
				})) {
			return this;
		}
		for (HakemusTyo h : hakemustyyppiEsitiedot) {
			if (null != h
					.rekisteroiKuuntelija(new EsitiedonKuuntelija<HakemusTyyppi>() {
						public ValintalaskentaTyo esitietoSaatavilla(
								HakemusTyyppi h) {
							ValintalaskentaTyo.this.hakemustyypit.add(h);
							int tyotaJaljella = laskuri.decrementAndGet();
							if (tyotaJaljella == 0) {
								return ValintalaskentaTyo.this;
							} else if (tyotaJaljella < 0) {
								throw new RuntimeException(
										"Hakukohdetyölle on valmistunut enemmän töitä kuin odotettiin valmistuvaksi!");
							} else {
								return null;
							}
						}
					})) {
				return this;
			}
		}
		return null;
	}
}
