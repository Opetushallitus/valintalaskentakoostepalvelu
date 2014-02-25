package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Threadsafe cache for valintalaskennat
 */
public class ValintalaskentaCache {
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaCache.class);
	// HakuOidilla voi tarkistaa ettei cachea käytetä useassa eri haussa
	// yhtäaikaa
	private final Collection<String> hakukohteet;
	private final ConcurrentHashMap<String, ValintaperusteetTyo> valintaperusteetEsitiedot;
	private final ConcurrentHashMap<String, HakemusTyo> hakemusTyyppiEsitiedot;

	// private final ConcurrentHashMap<String, Esitieto<?>>
	// valintalaskennanEsitiedot;

	public ValintalaskentaCache(Collection<String> hakukohteet) {
		this.valintaperusteetEsitiedot = new ConcurrentHashMap<String, ValintaperusteetTyo>();
		this.hakemusTyyppiEsitiedot = new ConcurrentHashMap<String, HakemusTyo>();
		this.hakukohteet = Collections.unmodifiableCollection(hakukohteet);
	}

	public Collection<? extends AbstraktiTyo> hakukohteenEsitiedotOnSelvitettyJaSeuraavaksiEsitiedotTyojonoihin(
			String hakukohdeOid, Collection<String> hakemusOids) {
		final Collection<AbstraktiTyo> tyot = Lists.newArrayList();
		final ValintalaskentaTyo hakukohdeTyo = new ValintalaskentaTyo();
		ValintaperusteetTyo valintaperusteetEsitieto = new ValintaperusteetTyo(
				hakukohdeOid);
		if (valintaperusteetEsitiedot.putIfAbsent(hakukohdeOid,
				valintaperusteetEsitieto) != null) {
			throw new RuntimeException(
					"Samalle hakukohteelle yritettiin työstää toistumiseen esitietoja!");
		}
		tyot.add(valintaperusteetEsitieto);
		Collection<HakemusTyo> kaikkiHakemusTyyppiEsitiedot = Lists
				.newArrayList();
		for (String hakemusOid : hakemusOids) {
			HakemusTyo uusiEsitieto = new HakemusTyo(hakemusOid);
			@SuppressWarnings("unchecked")
			HakemusTyo aiempiTyosto = hakemusTyyppiEsitiedot.putIfAbsent(
					hakemusOid, uusiEsitieto);
			if (aiempiTyosto != null) {
				// Jokin säie on jo hakemassa tätä hakemusta. Joten ei laiteta
				// sitä työjonoon mutta laitetaan se riippuvuudeksi tälle
				// hakukohdeTyölle
				kaikkiHakemusTyyppiEsitiedot.add(aiempiTyosto);
			} else {
				// Tämä säie ehti luoda esitiedon hakemukselle
				// Joten tehdään siitä työ myös työjonoon
				kaikkiHakemusTyyppiEsitiedot.add(uusiEsitieto);
				tyot.add(uusiEsitieto);
			}
		}
		ValintalaskentaTyo valmisValintalaskentaTyo = hakukohdeTyo
				.rekisteroiKuuntelijat(valintaperusteetEsitieto,
						kaikkiHakemusTyyppiEsitiedot);

		if (valmisValintalaskentaTyo != null) {
			return Lists.newArrayList(valmisValintalaskentaTyo);

		} else {
			return tyot;
		}
	}

	public Collection<ValintalaskentaTyo> esitietoHaettu(String oid,
			List<ValintaperusteetTyyppi> e) {
		return valintaperusteetEsitiedot.get(oid).setEsitieto(e);
	}

	public Collection<ValintalaskentaTyo> esitietoHaettu(String oid,
			HakemusTyyppi e) {
		return hakemusTyyppiEsitiedot.get(oid).setEsitieto(e);
	}

	/**
	 * 
	 * @return hakukohteet joista maski on jo miinustettu
	 */
	public Collection<String> getHakukohteet() {
		return hakukohteet;
	}

}
