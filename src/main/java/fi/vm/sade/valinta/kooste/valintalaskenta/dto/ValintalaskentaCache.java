package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
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
	private final ConcurrentHashMap<String, ValintaperusteetTyo<ValintalaskentaTyo>> valintaperusteetEsitiedot;
    private final ConcurrentHashMap<String, ValintaperusteetTyoRest<ValintalaskentaTyoRest>> valintaperusteetEsitiedotRest;
	private final ConcurrentHashMap<String, HakemusTyo<ValintalaskentaTyo>> hakemusTyyppiEsitiedot;
    private final ConcurrentHashMap<String, HakemusTyoRest<ValintalaskentaTyoRest>> hakemusTyyppiEsitiedotRest;

	// private final ConcurrentHashMap<String, Esitieto<?>>
	// valintalaskennanEsitiedot;

	public ValintalaskentaCache(Collection<String> hakukohteet) {
		this.valintaperusteetEsitiedot = new ConcurrentHashMap<String, ValintaperusteetTyo<ValintalaskentaTyo>>();
        this.valintaperusteetEsitiedotRest = new ConcurrentHashMap<String, ValintaperusteetTyoRest<ValintalaskentaTyoRest>>();
		this.hakemusTyyppiEsitiedot = new ConcurrentHashMap<String, HakemusTyo<ValintalaskentaTyo>>();
        this.hakemusTyyppiEsitiedotRest = new ConcurrentHashMap<String, HakemusTyoRest<ValintalaskentaTyoRest>>();
		this.hakukohteet = Collections.unmodifiableCollection(hakukohteet);
	}

    public Collection<? extends AbstraktiTyo> hakukohteenEsitiedotOnSelvitettyJaSeuraavaksiEsitiedotTyojonoihinRest(
            String hakukohdeOid, Collection<String> hakemusOids) {
        final Collection<AbstraktiTyo> tyot = Lists.newArrayList();
        final ValintalaskentaTyoRest hakukohdeTyo = new ValintalaskentaTyoRest(
                hakukohdeOid);
        ValintaperusteetTyoRest<ValintalaskentaTyoRest> valintaperusteetEsitieto = new ValintaperusteetTyoRest<ValintalaskentaTyoRest>(
                hakukohdeOid);
        if (valintaperusteetEsitiedotRest.putIfAbsent(hakukohdeOid,
                valintaperusteetEsitieto) != null) {
            throw new RuntimeException(
                    "Samalle hakukohteelle yritettiin työstää toistumiseen esitietoja!");
        }
        tyot.add(valintaperusteetEsitieto);
        Collection<HakemusTyoRest<ValintalaskentaTyoRest>> kaikkiHakemusTyyppiEsitiedot = Lists
                .newArrayList();
        for (String hakemusOid : hakemusOids) {
            HakemusTyoRest<ValintalaskentaTyoRest> uusiEsitieto = new HakemusTyoRest<ValintalaskentaTyoRest>(
                    hakemusOid);
            @SuppressWarnings("unchecked")
            HakemusTyoRest<ValintalaskentaTyoRest> aiempiTyosto = hakemusTyyppiEsitiedotRest
                    .putIfAbsent(hakemusOid, uusiEsitieto);
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
        ValintalaskentaTyoRest valmisValintalaskentaTyo = hakukohdeTyo
                .rekisteroiKuuntelijat(valintaperusteetEsitieto,
                        kaikkiHakemusTyyppiEsitiedot);

        if (valmisValintalaskentaTyo != null) {
            // Jos tyo valmistui niin voidaan olettaa tekemättömiä esitietoja ei
            // ollut ja siten ei myöskään muita palautettavia töitä kuin tämä
            // valmistunut työ
            return Lists.newArrayList(valmisValintalaskentaTyo);

        } else {
            // Palautetaan toistaiseksi hakemattomat esitiedot tyojonoon
            return tyot;
        }
    }

	public Collection<? extends AbstraktiTyo> hakukohteenEsitiedotOnSelvitettyJaSeuraavaksiEsitiedotTyojonoihin(
			String hakukohdeOid, Collection<String> hakemusOids) {
		final Collection<AbstraktiTyo> tyot = Lists.newArrayList();
		final ValintalaskentaTyo hakukohdeTyo = new ValintalaskentaTyo(
				hakukohdeOid);
		ValintaperusteetTyo<ValintalaskentaTyo> valintaperusteetEsitieto = new ValintaperusteetTyo<ValintalaskentaTyo>(
				hakukohdeOid);
		if (valintaperusteetEsitiedot.putIfAbsent(hakukohdeOid,
				valintaperusteetEsitieto) != null) {
			throw new RuntimeException(
					"Samalle hakukohteelle yritettiin työstää toistumiseen esitietoja!");
		}
		tyot.add(valintaperusteetEsitieto);
		Collection<HakemusTyo<ValintalaskentaTyo>> kaikkiHakemusTyyppiEsitiedot = Lists
				.newArrayList();
		for (String hakemusOid : hakemusOids) {
			HakemusTyo<ValintalaskentaTyo> uusiEsitieto = new HakemusTyo<ValintalaskentaTyo>(
					hakemusOid);
			@SuppressWarnings("unchecked")
			HakemusTyo<ValintalaskentaTyo> aiempiTyosto = hakemusTyyppiEsitiedot
					.putIfAbsent(hakemusOid, uusiEsitieto);
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
			// Jos tyo valmistui niin voidaan olettaa tekemättömiä esitietoja ei
			// ollut ja siten ei myöskään muita palautettavia töitä kuin tämä
			// valmistunut työ
			return Lists.newArrayList(valmisValintalaskentaTyo);

		} else {
			// Palautetaan toistaiseksi hakemattomat esitiedot tyojonoon
			return tyot;
		}
	}

	public Collection<ValintalaskentaTyo> esitietoHaettu(String oid,
			List<ValintaperusteetTyyppi> e) {
		return valintaperusteetEsitiedot.get(oid).setEsitieto(e);
	}

    public Collection<ValintalaskentaTyoRest> esitietoHaettuRest(String oid,
                                                         List<ValintaperusteetDTO> e) {
        return valintaperusteetEsitiedotRest.get(oid).setEsitieto(e);
    }

	public Collection<ValintalaskentaTyo> esitietoOhitettu(String oid) {
        return valintaperusteetEsitiedot.get(oid).setEsitieto(null);
    }

    public Collection<ValintalaskentaTyoRest> esitietoOhitettuRest(String oid) {
        return valintaperusteetEsitiedotRest.get(oid).setEsitieto(null);
    }

	public Collection<ValintalaskentaTyo> esitietoHaettu(String oid,
			HakemusTyyppi e) {
		return hakemusTyyppiEsitiedot.get(oid).setEsitieto(e);
	}

    public Collection<ValintalaskentaTyoRest> esitietoHaettuRest(String oid,
                                                         HakemusDTO e) {
        return hakemusTyyppiEsitiedotRest.get(oid).setEsitieto(e);
    }

	/**
	 * 
	 * @return hakukohteet joista maski on jo miinustettu
	 */
	public Collection<String> getHakukohteet() {
		return hakukohteet;
	}

}
