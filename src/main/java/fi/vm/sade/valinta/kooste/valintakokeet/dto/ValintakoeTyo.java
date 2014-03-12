package fi.vm.sade.valinta.kooste.valintakokeet.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.AbstraktiTyo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.EsitiedonKuuntelija;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintaperusteetTyo;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Toisin kuin valintalaskenta tyossa niin valintakoetyon esitietoja
 *         varten joudutaan jo hakemaan hakemuksen tiedot. Hakemukselta
 *         saatavista hakutoiveista saadaan lopulliset esitiedot ja tyon
 *         prosessointi voidaan aloittaa.
 */
public class ValintakoeTyo extends AbstraktiTyo {

	private final HakemusTyyppi esitiedoiksiHaettuHakemus;
	private final List<List<ValintaperusteetTyyppi>> valintaperusteet;
	private final AtomicInteger laskuri;
	private final AtomicBoolean ohitettu;

	public ValintakoeTyo(HakemusTyyppi hakemus) {
		super();
		this.esitiedoiksiHaettuHakemus = hakemus;
		this.valintaperusteet = Collections.synchronizedList(Lists
				.<List<ValintaperusteetTyyppi>> newArrayList());
		this.laskuri = new AtomicInteger(-1);
		this.ohitettu = new AtomicBoolean(false);
	}

	public ValintakoeTyo rekisteroiKuuntelijat(
			Collection<ValintaperusteetTyo<ValintakoeTyo>> valintaperusteetEsitiedot) {
		laskuri.set(valintaperusteetEsitiedot.size());
		for (ValintaperusteetTyo<ValintakoeTyo> valintaperusteetTyo : valintaperusteetEsitiedot) {
			// Jos palauttaa not null:n niin tyo on valmistunut
			if (null != valintaperusteetTyo
					.rekisteroiKuuntelija(new EsitiedonKuuntelija<ValintakoeTyo, List<ValintaperusteetTyyppi>>() {

						@Override
						public ValintakoeTyo esitietoSaatavilla(
								List<ValintaperusteetTyyppi> esitieto) {
							ValintakoeTyo.this.valintaperusteet.add(esitieto);
							return dekrementoiJaTarkista();
						}

						@Override
						public ValintakoeTyo esitietoOhitettu() {
							ValintakoeTyo.this.ohitettu.set(true);
							return dekrementoiJaTarkista();
						}
					})) {
				return this;
			}

		}
		return null;
	}

	public HakemusTyyppi getHakemus() {
		return esitiedoiksiHaettuHakemus;
	}

	public List<ValintaperusteetTyyppi> getValintaperusteet() {
		// synchronized listia iteroidessa taytyy synkronoida tai tietojoukon
		// lukeminen ei valttamatta onnistu oikein
		synchronized (valintaperusteet) {
			return Lists.newArrayList(Iterables.concat(valintaperusteet));
		}
	}

	private ValintakoeTyo dekrementoiJaTarkista() {
		int tyotaJaljella = this.laskuri.decrementAndGet();
		if (tyotaJaljella == 0) {
			return ValintakoeTyo.this;
		} else if (tyotaJaljella < 0) {
			throw new RuntimeException(
					"Hakukohdetyölle on valmistunut enemmän töitä kuin odotettiin valmistuvaksi!");
		} else {
			return null;
		}
	}
}
