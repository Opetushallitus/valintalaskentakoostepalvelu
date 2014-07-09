package fi.vm.sade.valinta.kooste.valintakokeet.dto;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.AbstraktiTyo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.EsitiedonKuuntelija;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintaperusteetTyoRest;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Toisin kuin valintalaskenta tyossa niin valintakoetyon esitietoja
 *         varten joudutaan jo hakemaan hakemuksen tiedot. Hakemukselta
 *         saatavista hakutoiveista saadaan lopulliset esitiedot ja tyon
 *         prosessointi voidaan aloittaa.
 */
public class ValintakoeTyoRest extends AbstraktiTyo {

	private final HakemusDTO esitiedoiksiHaettuHakemus;
	private final List<List<ValintaperusteetDTO>> valintaperusteet;
	private final AtomicInteger laskuri;
	private final AtomicBoolean ohitettu;

	public ValintakoeTyoRest(HakemusDTO hakemus) {
		super();
		this.esitiedoiksiHaettuHakemus = hakemus;
		this.valintaperusteet = Collections.synchronizedList(Lists
				.<List<ValintaperusteetDTO>> newArrayList());
		this.laskuri = new AtomicInteger(-1);
		this.ohitettu = new AtomicBoolean(false);
	}

	public ValintakoeTyoRest rekisteroiKuuntelijat(
			Collection<ValintaperusteetTyoRest<ValintakoeTyoRest>> valintaperusteetEsitiedot) {
		laskuri.set(valintaperusteetEsitiedot.size());
		for (ValintaperusteetTyoRest<ValintakoeTyoRest> valintaperusteetTyo : valintaperusteetEsitiedot) {
			// Jos palauttaa not null:n niin tyo on valmistunut
			if (null != valintaperusteetTyo
					.rekisteroiKuuntelija(new EsitiedonKuuntelija<ValintakoeTyoRest, List<ValintaperusteetDTO>>() {

						@Override
						public ValintakoeTyoRest esitietoSaatavilla(
								List<ValintaperusteetDTO> esitieto) {
							ValintakoeTyoRest.this.valintaperusteet.add(esitieto);
							return dekrementoiJaTarkista();
						}

						@Override
						public ValintakoeTyoRest esitietoOhitettu() {
							ValintakoeTyoRest.this.ohitettu.set(true);
							return dekrementoiJaTarkista();
						}
					})) {
				return this;
			}

		}
		return null;
	}

	public HakemusDTO getHakemus() {
		return esitiedoiksiHaettuHakemus;
	}

	public List<ValintaperusteetDTO> getValintaperusteet() {
		// synchronized listia iteroidessa taytyy synkronoida tai tietojoukon
		// lukeminen ei valttamatta onnistu oikein
		synchronized (valintaperusteet) {
			return Lists.newArrayList(Iterables.concat(valintaperusteet));
		}
	}

	private ValintakoeTyoRest dekrementoiJaTarkista() {
		int tyotaJaljella = this.laskuri.decrementAndGet();
		if (tyotaJaljella == 0) {
			return ValintakoeTyoRest.this;
		} else if (tyotaJaljella < 0) {
			throw new RuntimeException(
					"Hakukohdetyölle on valmistunut enemmän töitä kuin odotettiin valmistuvaksi!");
		} else {
			return null;
		}
	}
}
