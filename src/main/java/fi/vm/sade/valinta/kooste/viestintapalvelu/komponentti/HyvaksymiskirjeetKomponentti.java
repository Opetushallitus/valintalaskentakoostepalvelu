package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.JonosijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValinnanvaiheDTO;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluajoResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemuksenTila;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.Valintatapajono;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteHakemukseltaUtil;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         OLETTAA ETTA KAIKILLE VALINTATAPAJONOILLE TEHDAAN HYVAKSYMISKIRJE JOS
 *         HAKEMUS ON HYVAKSYTTY YHDESSAKIN!
 */
@Component("hyvaksymiskirjeetKomponentti")
public class HyvaksymiskirjeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(HyvaksymiskirjeetKomponentti.class);
    private static final String TYHJA_TARJOAJANIMI = "Tuntematon koulu!";
    private static final String TYHJA_HAKUKOHDENIMI = "Tuntematon koulutus!";

    @Autowired
    private SijoitteluajoResource sijoitteluajoResource;

    @Autowired
    private HakukohdeResource tarjontaResource;

    @Autowired
    fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource laskentaResource;

    private static final String KIELIKOODI = "kieli_fi";

    public String teeHyvaksymiskirjeet(@Simple("${property.hakukohdeOid}") String hakukohdeOid,
            @Simple("${property.hakuOid}") String hakuOid, @Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
            @Simple("${property.hakemukset}") List<HakemusTyyppi> hakemukset) {
        LOG.debug("Hyvaksymiskirjeet for hakukohde '{}' and haku '{}'", new Object[] { hakukohdeOid, hakuOid });

        //
        // Cachetetaan kaikki ettei tarvittavia tietoja haeta palvelimilta
        // uudestaan jokaiselle hakemukselle!
        //
        Map<String, Hakukohde> kohdeCache = new HashMap<String, Hakukohde>();
        Map<String, HakukohdeNimiRDTO> nimiCache = new HashMap<String, HakukohdeNimiRDTO>();
        Map<String, Integer> hyvaksytytCache = new HashMap<String, Integer>();
        Map<String, List<ValinnanvaiheDTO>> laskentaCache = new HashMap<String, List<ValinnanvaiheDTO>>();
        Map<String, BigDecimal> tulosCache = new HashMap<String, BigDecimal>();
        //
        // Hyvaksymiskirje koskee vain kyseiselle hakukohteelle hyvaksyttyja!
        // Ainoastaan naista ollaan kiinnostuneita!
        //
        List<Kirje> kirjeet = new ArrayList<Kirje>();
        Map<String, HakemusTyyppi> hakuAppHakemukset = convertToHakemusOidMap(hakemukset);
        Collection<Hakemus> hyvaksytytHakemukset = filterHyvaksytytHakemukset(haeHakemukset(haeHakukohde(
                sijoitteluajoId, hakukohdeOid, kohdeCache)));
        final String koulu = extractTarjoajaNimi(haeHakukohdeNimi(hakukohdeOid, nimiCache), KIELIKOODI);
        final String koulutus = extractHakukohdeNimi(haeHakukohdeNimi(hakukohdeOid, nimiCache), KIELIKOODI);
        for (Hakemus hakemus : hyvaksytytHakemukset) {
            Osoite osoite = OsoiteHakemukseltaUtil.osoiteHakemuksesta(hakuAppHakemukset.get(hakemus.getHakemusOid()));

            List<Map<String, String>> tulosList = new ArrayList<Map<String, String>>();
            List<HakemusDTO> hakemuksetDTO = sijoitteluajoResource.getHakemusBySijoitteluajo(sijoitteluajoId,
                    hakemus.getHakemusOid());
            //
            // VOIKO SAMA HAKEMUS OLLA SAMASSA HAKUKOHTEESSA MUTTA ERI
            // VALINTATAPAJONOSSA?
            //
            for (HakemusDTO dto : hakemuksetDTO) {
                Hakukohde hakukohde = haeHakukohde(sijoitteluajoId, dto.getHakukohdeOid(), kohdeCache);
                // List<ValinnanvaiheDTO> valinnanvaiheet =
                // haeValinnanvaiheet(dto.getHakukohdeOid(), laskentaCache);
                Collection<Hakemus> kaikkiHakemukset = haeHakemukset(hakukohde);
                Collection<Hakemus> tahanHyvaksytyt = filterHyvaksytytHakemukset(kaikkiHakemukset);

                HakukohdeNimiRDTO tamanHakukohteenNimi = haeHakukohdeNimi(dto.getHakukohdeOid(), nimiCache);
                Map<String, String> tulokset = new HashMap<String, String>();
                tulokset.put("alinHyvaksyttyPistemaara", "--");
                tulokset.put("hakukohteenNimi", extractHakukohdeNimi(tamanHakukohteenNimi, KIELIKOODI));
                tulokset.put("hylkayksenSyy", "");
                tulokset.put("hyvaksytyt", "" + countHyvaksytytHakemukset(hakukohde, hyvaksytytCache));//

                tulokset.put("kaikkiHakeneet", "" + kaikkiHakemukset.size());
                tulokset.put("omatPisteet", "--");// countOmatPisteet(dto.getHakemusOid(),
                                                  // valinnanvaiheet));

                tulokset.put("oppilaitoksenNimi", "lukio");
                tulokset.put("organisaationNimi", extractTarjoajaNimi(tamanHakukohteenNimi, KIELIKOODI));
                tulokset.put("paasyJaSoveltuvuuskoe", "--");
                tulokset.put("selite", "");
                tulokset.put("valinnanTulos", dto.getTila().toString());
                tulosList.add(tulokset);

            }
            kirjeet.add(new Kirje(osoite, "FI", koulu, koulutus, tulosList));
        }

        String hyvaksymiskirjeet = new Gson().toJson(new Kirjeet(kirjeet));
        LOG.debug("Hyvaksymiskirjeet {}", hyvaksymiskirjeet);
        return hyvaksymiskirjeet;
    }

    private String countOmatPisteet(String hakemusOid, // String
                                                       // valintatapajonoOid,
            List<ValinnanvaiheDTO> valinnanvaiheet) {
        List<BigDecimal> pisteet = new ArrayList<BigDecimal>();
        for (ValinnanvaiheDTO v : valinnanvaiheet) {
            for (ValintatapajonoDTO j : v.getValintatapajono()) {

                for (JonosijaDTO sija : j.getJonosijat()) {
                    if (hakemusOid.equals(sija.getHakemusOid())) {
                        try {
                            // Kopioidaan ensimmäisen järjestyskriteerin tulos!
                            // Tämä on lopullinen pistesaldo tästä
                            // valintatapajonosta!
                            pisteet.add(sija.getJarjestyskriteerit().get(0).getArvo()
                                    .round(new MathContext(0, RoundingMode.HALF_UP)));
                        } catch (Exception e) {
                            LOG.error("Ei järjestyskriteeriä hakemukselle {}", hakemusOid);
                        }

                    }
                }

            }
        }
        // LOG.error("Hakemukselle {} ei loytynyt tuloksia!", new Object[] {
        // hakemusOid });
        StringBuilder arvot = new StringBuilder();
        for (BigDecimal d : pisteet) {
            arvot.append(d.toString());
            arvot.append(" ");
        }
        return arvot.toString();
    }

    private int countHyvaksytytHakemukset(Hakukohde hakukohde, Map<String, Integer> hyvaksytytCache) {
        if (hyvaksytytCache.containsKey(hakukohde.getOid())) {
            return hyvaksytytCache.get(hakukohde.getOid());
        } else {
            int hyvaksytyt = 0;
            for (Valintatapajono jono : hakukohde.getValintatapajonot()) {
                for (Hakemus hakemus : jono.getHakemukset()) {
                    if (HakemuksenTila.HYVAKSYTTY.equals(hakemus.getTila())) {
                        ++hyvaksytyt;
                    }
                }
            }
            hyvaksytytCache.put(hakukohde.getOid(), hyvaksytyt);
            return hyvaksytyt;
        }
    }

    private Collection<Hakemus> haeHakemukset(Hakukohde hakukohde) {// String
        // valintatapajonoOid,
        Map<String, Hakemus> h = new HashMap<String, Hakemus>();
        for (Valintatapajono jono : hakukohde.getValintatapajonot()) {
            // if (valintatapajonoOid.equals(jono.getOid())) {
            for (Hakemus hakemus : jono.getHakemukset()) {
                h.put(hakemus.getHakemusOid(), hakemus);// .getHakemusOid(),
                // hakemus);
            }
            // }
        }
        return h.values();
    }

    private Collection<Hakemus> filterHyvaksytytHakemukset(Collection<Hakemus> hakemukset) {// String
        // valintatapajonoOid,
        List<Hakemus> h0 = new ArrayList<Hakemus>();
        for (Hakemus h : hakemukset) {
            if (HakemuksenTila.HYVAKSYTTY.equals(h.getTila())) {
                h0.add(h);
            }
        }
        return h0;
    }

    private String extractHakukohdeNimi(HakukohdeNimiRDTO nimi, String kielikoodi) {
        if (nimi.getHakukohdeNimi().containsKey(kielikoodi)) {
            return nimi.getHakukohdeNimi().get(kielikoodi);
        }
        if (nimi.getHakukohdeNimi().isEmpty()) { // <- Ei tarjoaja nimia!
            return TYHJA_HAKUKOHDENIMI;
        }
        return nimi.getHakukohdeNimi().values().iterator().next(); // <-
                                                                   // Palautetaan
                                                                   // joku!
    }

    private String extractTarjoajaNimi(HakukohdeNimiRDTO nimi, String kielikoodi) {
        if (nimi.getTarjoajaNimi().containsKey(kielikoodi)) {
            return nimi.getTarjoajaNimi().get(kielikoodi);
        }
        if (nimi.getTarjoajaNimi().isEmpty()) { // <- Ei tarjoaja nimia!
            return TYHJA_TARJOAJANIMI;
        }
        return nimi.getTarjoajaNimi().values().iterator().next(); // <-
                                                                  // Palautetaan
                                                                  // joku!
    }

    private List<ValinnanvaiheDTO> haeValinnanvaiheet(String hakukohdeOid, Map<String, List<ValinnanvaiheDTO>> cache) {
        if (cache.containsKey(hakukohdeOid)) {
            return cache.get(hakukohdeOid);
        } else {
            LOG.debug("Haetaan hakukohde '{}' valintalaskennan tulospalvelusta!", hakukohdeOid);
            List<ValinnanvaiheDTO> valinnanvaiheet = laskentaResource.hakukohde(hakukohdeOid);
            cache.put(hakukohdeOid, valinnanvaiheet);
            return valinnanvaiheet;
        }
    }

    private Hakukohde haeHakukohde(long sijoitteluajoId, String hakukohdeOid, Map<String, Hakukohde> cache) {
        if (cache.containsKey(hakukohdeOid)) {
            return cache.get(hakukohdeOid);
        } else {
            LOG.debug("Haetaan hakukohde '{}' sijoittelulta!", hakukohdeOid);
            Hakukohde kohde = sijoitteluajoResource.getHakukohdeBySijoitteluajo(sijoitteluajoId, hakukohdeOid);
            cache.put(hakukohdeOid, kohde);
            return kohde;
        }
    }

    private HakukohdeNimiRDTO haeHakukohdeNimi(String hakukohdeOid, Map<String, HakukohdeNimiRDTO> cache) {
        if (cache.containsKey(hakukohdeOid)) {
            return cache.get(hakukohdeOid);
        } else {
            LOG.debug("Haetaan hakukohteen '{}' todellinen nimi tarjonnalta!", hakukohdeOid);
            HakukohdeNimiRDTO nimi = tarjontaResource.getHakukohdeNimi(hakukohdeOid);
            cache.put(hakukohdeOid, nimi);
            return nimi;
        }
    }

    private Map<String, HakemusTyyppi> convertToHakemusOidMap(List<HakemusTyyppi> hakemukset) {
        Map<String, HakemusTyyppi> tmp = new HashMap<String, HakemusTyyppi>();
        for (HakemusTyyppi h : hakemukset) {
            tmp.put(h.getHakemusOid(), h);
        }
        return tmp;
    }
}
