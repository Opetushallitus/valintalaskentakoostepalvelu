package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private final AtomicBoolean ohitettu;
	private final AtomicInteger laskuri;
	private final String hakukohdeOid;

	public ValintalaskentaTyo(String hakukohdeOid) {
		this.ohitettu = new AtomicBoolean(false);
		this.laskuri = new AtomicInteger(-1);
		this.hakemustyypit = Collections.synchronizedList(Lists
				.<HakemusTyyppi> newArrayList());
		this.valintaperusteet = new AtomicReference<List<ValintaperusteetTyyppi>>(
				null);
		this.hakukohdeOid = hakukohdeOid;
	}

	public String getHakukohdeOid() {
		return hakukohdeOid;
	}

	public boolean isValmis() {
		return this.laskuri.get() == 0;
	}

	public boolean isOhitettu() {
		return this.ohitettu.get();
	}

	public List<ValintaperusteetTyyppi> getValintaperusteet() {
		if (this.ohitettu.get()) {
			throw new RuntimeException(
					"Yritetään hakea ohitetulle valintalaskentatyölle valintaperusteita. Tarkista ettei työtä ole ohitettu ennen valintaperusteiden hakua!");
		}
		return valintaperusteet.get();
	}

	public List<HakemusTyyppi> getHakemustyypit() {
		if (this.ohitettu.get()) {
			throw new RuntimeException(
					"Yritetään hakea ohitetulle valintalaskentatyölle hakemustyyppejä. Tarkista ettei työtä ole ohitettu ennen hakemustyyppien pyyntöä!");
		}
		return hakemustyypit;
	}

	public ValintalaskentaTyo rekisteroiKuuntelijat(
			ValintaperusteetTyo valintaperusteetEsitieto,
			Collection<HakemusTyo> hakemustyyppiEsitiedot) {
		if (hakemustyyppiEsitiedot == null || hakemustyyppiEsitiedot.isEmpty()) {
			throw new RuntimeException(
					"Hakukohdetyotä ei tarvitse luoda jos hakukohteella ei ole hakemuksia!");
		}
		this.laskuri.set(1 + hakemustyyppiEsitiedot.size());
		if (null != valintaperusteetEsitieto
				.rekisteroiKuuntelija(new EsitiedonKuuntelija<List<ValintaperusteetTyyppi>>() {
					public ValintalaskentaTyo esitietoOhitettu() {
						ohitettu.set(true); // Voi olla ohitettu kahdenkin eri
											// esitiedon puuttumisen vuoksi
						return dekrementoiJaTarkista();
					}

					public ValintalaskentaTyo esitietoSaatavilla(
							List<ValintaperusteetTyyppi> v) {
						// Tuskin tarpeen mutta varmistaa että reitityksessä ei
						// ole tehty kirjoitusvirheitä
						if (!ValintalaskentaTyo.this.valintaperusteet
								.compareAndSet(null, v)) {
							throw new RuntimeException(
									"Valintaperusteet merkittiin kahdesti valmistuneeksi esitiedoksi valintalaskentatyölle!");
						}
						return dekrementoiJaTarkista();
					}
				})) {
			return this;
		}
		for (HakemusTyo h : hakemustyyppiEsitiedot) {
			if (null != h
					.rekisteroiKuuntelija(new EsitiedonKuuntelija<HakemusTyyppi>() {
						public ValintalaskentaTyo esitietoOhitettu() {
							ohitettu.set(true); // Voi olla ohitettu kahdenkin
												// eri esitiedon puuttumisen
												// vuoksi
							return dekrementoiJaTarkista();
						}

						public ValintalaskentaTyo esitietoSaatavilla(
								HakemusTyyppi h) {
							ValintalaskentaTyo.this.hakemustyypit.add(h);
							return dekrementoiJaTarkista();
						}
					})) {
				return this;
			}
		}
		return null;
	}

	private ValintalaskentaTyo dekrementoiJaTarkista() {
		int tyotaJaljella = this.laskuri.decrementAndGet();
		if (tyotaJaljella == 0) {
			return ValintalaskentaTyo.this;
		} else if (tyotaJaljella < 0) {
			throw new RuntimeException(
					"Hakukohdetyölle on valmistunut enemmän töitä kuin odotettiin valmistuvaksi!");
		} else {
			return null;
		}
	}
}
