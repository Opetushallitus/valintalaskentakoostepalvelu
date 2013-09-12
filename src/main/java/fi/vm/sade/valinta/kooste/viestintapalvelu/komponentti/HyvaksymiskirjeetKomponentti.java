package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.language.Simple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteluResource;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemuksenTila;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.HakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HakemuksenTilaUtil;
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
    private SijoitteluResource sijoitteluResource;

    @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}")
    private String sijoitteluResourceUrl;

    @Autowired
    private HakukohdeResource tarjontaResource;

    @Value("${valintalaskentakoostepalvelu.tarjonta.rest.url}")
    private String tarjontaResourceUrl;

    @Autowired
    private ApplicationResource applicationResource;

    @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}")
    private String hakuAppResourceUrl;

    // private static final String KIELIKOODI = "kieli_fi";

    public String teeHyvaksymiskirjeet(@Simple("${property.kielikoodi}") String kielikoodi,
            @Simple("${property.hakukohdeOid}") String hakukohdeOid, @Simple("${property.hakuOid}") String hakuOid,
            @Simple("${property.sijoitteluajoId}") Long sijoitteluajoId) {
        LOG.debug("Hyvaksymiskirjeet for hakukohde '{}' and haku '{}' and sijoitteluajo '{}'", new Object[] {
                hakukohdeOid, hakuOid, sijoitteluajoId });
        assert (hakukohdeOid != null);
        assert (hakuOid != null);
        assert (sijoitteluajoId != null);
        //
        // Cachetetaan kaikki ettei tarvittavia tietoja haeta palvelimilta
        // uudestaan jokaiselle hakemukselle!
        //
        Map<String, HakukohdeDTO> kohdeCache = new HashMap<String, HakukohdeDTO>();
        Map<String, HakukohdeNimiRDTO> nimiCache = new HashMap<String, HakukohdeNimiRDTO>();
        Map<String, Integer> hyvaksytytCache = new HashMap<String, Integer>();
        // Map<String, List<ValinnanvaiheDTO>> laskentaCache = new
        // HashMap<String, List<ValinnanvaiheDTO>>();
        // Map<String, BigDecimal> tulosCache = new HashMap<String,
        // BigDecimal>();
        //
        // Hyvaksymiskirje koskee vain kyseiselle hakukohteelle hyvaksyttyja!
        //
        List<Kirje> kirjeet = new ArrayList<Kirje>();
        Collection<HakemusDTO> hyvaksytytHakemukset;
        {
            LOG.debug("Haetaan hakukohde sijoittelulta!");
            HakukohdeDTO kohde = haeHakukohde(hakuOid, sijoitteluajoId, hakukohdeOid, kohdeCache);
            LOG.debug("Haetaan hakemukset sijoittelulta!");
            Collection<HakemusDTO> dtot = haeHakemukset(kohde);
            hyvaksytytHakemukset = filterHyvaksytytHakemukset(dtot);
            LOG.debug("Filtteröidään hyväksytyt! Löytyi {}", hyvaksytytHakemukset.size());
        }
        final String koulu = extractTarjoajaNimi(haeHakukohdeNimi(hakukohdeOid, nimiCache), kielikoodi);
        final String koulutus = extractHakukohdeNimi(haeHakukohdeNimi(hakukohdeOid, nimiCache), kielikoodi);
        for (HakemusDTO hakemus : hyvaksytytHakemukset) {
            String hakemusOid = hakemus.getHakemusOid();
            LOG.debug("Yhteys {}, ApplicationResource.getApplicationByOid({})", new Object[] { hakuAppResourceUrl,
                    hakemusOid });
            Osoite osoite;
            try {
                osoite = OsoiteHakemukseltaUtil.osoiteHakemuksesta(applicationResource.getApplicationByOid(hakemusOid));
            } catch (Exception e) {
                LOG.error("Ei voitu hakea osoitetta Haku-palvelusta hakemukselle {}! {}", new Object[] { hakemusOid,
                        hakuAppResourceUrl });
                throw new RuntimeException("Ei voitu hakea osoitetta Haku-palvelusta hakemukselle " + hakemusOid, e);
            }
            List<Map<String, String>> tulosList = new ArrayList<Map<String, String>>();
            LOG.debug("Yhteys {}, SijoitteluResource.getHakemusBySijoitteluajo({},{},{})", new Object[] {
                    sijoitteluResourceUrl, hakuOid, sijoitteluajoId, hakemus.getHakemusOid() });
            List<HakemusDTO> hakemuksetDTO = sijoitteluResource.getHakemusBySijoitteluajo(hakuOid,
                    sijoitteluajoId.toString(), hakemus.getHakemusOid());// getHakemusBySijoitteluajo(sijoitteluajoId,

            //
            // VOIKO SAMA HAKEMUS OLLA SAMASSA HAKUKOHTEESSA MUTTA ERI
            // VALINTATAPAJONOSSA?
            //
            for (HakemusDTO dto : hakemuksetDTO) {
                HakukohdeDTO hakukohde = haeHakukohde(hakuOid, sijoitteluajoId, dto.getHakukohdeOid(), kohdeCache);
                for (ValintatapajonoDTO jono : hakukohde.getValintatapajonot()) {
                    jono.getAlinHyvaksyttyPistemaara();
                    // List<ValinnanvaiheDTO> valinnanvaiheet =
                    // haeValinnanvaiheet(dto.getHakukohdeOid(), laskentaCache);
                    Collection<HakemusDTO> kaikkiHakemukset = haeHakemuksetJonolle(jono);
                    // Collection<Hakemus> tahanHyvaksytyt =
                    // filterHyvaksytytHakemukset(kaikkiHakemukset);

                    HakukohdeNimiRDTO tamanHakukohteenNimi = haeHakukohdeNimi(dto.getHakukohdeOid(), nimiCache);
                    Map<String, String> tulokset = new HashMap<String, String>();
                    tulokset.put("alinHyvaksyttyPistemaara",
                            Formatter.suomennaNumero(jono.getAlinHyvaksyttyPistemaara()));

                    tulokset.put("hakukohteenNimi", extractHakukohdeNimi(tamanHakukohteenNimi, kielikoodi));
                    tulokset.put("oppilaitoksenNimi", ""); // tieto on jo osana
                                                           // hakukohdenimea
                                                           // joten
                                                           // tuskin tarvii
                                                           // toistaa
                    tulokset.put("hylkayksenSyy", "");
                    tulokset.put("hyvaksytyt", "" + countHyvaksytytHakemukset(hakukohde, hyvaksytytCache));//

                    tulokset.put("kaikkiHakeneet", "" + kaikkiHakemukset.size());
                    tulokset.put("omatPisteet", Formatter.suomennaNumero(hakemus.getPisteet()));// countOmatPisteet(dto.getHakemusOid(),
                    // valinnanvaiheet));

                    tulokset.put("organisaationNimi", extractTarjoajaNimi(tamanHakukohteenNimi, kielikoodi));
                    tulokset.put("paasyJaSoveltuvuuskoe",
                            Formatter.suomennaNumero(hakemus.getPaasyJaSoveltuvuusKokeenTulos()));
                    tulokset.put("selite", "");
                    tulokset.put("valinnanTulos", HakemuksenTilaUtil.tilaConverter(dto.getTila().toString()));
                    tulosList.add(tulokset);
                }
            }
            kirjeet.add(new Kirje(osoite, "FI", koulu, koulutus, tulosList));
        }

        String hyvaksymiskirjeet = new Gson().toJson(new Kirjeet(kirjeet));
        LOG.debug("Hyvaksymiskirjeet {}", hyvaksymiskirjeet);
        return hyvaksymiskirjeet;
    }

    private int countHyvaksytytHakemukset(HakukohdeDTO hakukohde, Map<String, Integer> hyvaksytytCache) {
        if (hyvaksytytCache.containsKey(hakukohde.getOid())) {
            return hyvaksytytCache.get(hakukohde.getOid());
        } else {
            int hyvaksytyt = 0;
            for (fi.vm.sade.valinta.kooste.external.resource.sijoittelu.dto.ValintatapajonoDTO jono : hakukohde
                    .getValintatapajonot()) {
                for (HakemusDTO hakemus : jono.getHakemukset()) {
                    if (HakemuksenTila.HYVAKSYTTY.equals(hakemus.getTila())) {
                        ++hyvaksytyt;
                    }
                }
            }
            hyvaksytytCache.put(hakukohde.getOid(), hyvaksytyt);
            return hyvaksytyt;
        }
    }

    private Collection<HakemusDTO> haeHakemuksetJonolle(ValintatapajonoDTO jono) {// String
        // valintatapajonoOid,
        if (jono == null) {
            LOG.error("Ei hakukohdetta!");
            return Collections.emptyList();
        } else {
            Map<String, HakemusDTO> h = new HashMap<String, HakemusDTO>();
            for (HakemusDTO hakemus : jono.getHakemukset()) {
                h.put(hakemus.getHakemusOid(), hakemus);// .getHakemusOid(),
                // hakemus);
            }
            return h.values();
        }
    }

    private Collection<HakemusDTO> haeHakemukset(HakukohdeDTO hakukohde) {// String
        // valintatapajonoOid,
        if (hakukohde == null) {
            LOG.error("Ei hakukohdetta!");
            return Collections.emptyList();
        } else {
            Map<String, HakemusDTO> h = new HashMap<String, HakemusDTO>();
            for (ValintatapajonoDTO jono : hakukohde.getValintatapajonot()) {
                // if (valintatapajonoOid.equals(jono.getOid())) {
                for (HakemusDTO hakemus : jono.getHakemukset()) {
                    h.put(hakemus.getHakemusOid(), hakemus);// .getHakemusOid(),
                    // hakemus);
                }
                // }
            }
            return h.values();
        }
    }

    private Collection<HakemusDTO> filterHyvaksytytHakemukset(Collection<HakemusDTO> hakemukset) {// String
        if (hakemukset == null) {
            LOG.error("Ei hakemuksia!");
            return Collections.emptyList();
        }
        // valintatapajonoOid,
        List<HakemusDTO> h0 = new ArrayList<HakemusDTO>();
        for (HakemusDTO h : hakemukset) {
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

    private HakukohdeDTO haeHakukohde(String hakuOid, Long sijoitteluajoId, String hakukohdeOid,
            Map<String, HakukohdeDTO> cache) {
        if (cache.containsKey(hakukohdeOid)) {
            return cache.get(hakukohdeOid);
        } else {
            LOG.debug("Yhteys {}, SijoitteluResource.getHakukohdeBySijoitteluajo({},{},{})", new Object[] {
                    sijoitteluResourceUrl, hakuOid, sijoitteluajoId, hakukohdeOid });
            HakukohdeDTO kohde = sijoitteluResource.getHakukohdeBySijoitteluajo(hakuOid, sijoitteluajoId.toString(),
                    hakukohdeOid);// HakukohdeBySijoitteluajo(sijoitteluajoId,
                                  // hakukohdeOid);
            cache.put(hakukohdeOid, kohde);
            return kohde;
        }
    }

    private HakukohdeNimiRDTO haeHakukohdeNimi(String hakukohdeOid, Map<String, HakukohdeNimiRDTO> cache) {
        if (cache.containsKey(hakukohdeOid)) {
            return cache.get(hakukohdeOid);
        } else {
            LOG.debug("Yhteys {}, HakukohdeResource.getHakukohdeNimi({})", new Object[] { tarjontaResourceUrl,
                    hakukohdeOid });
            HakukohdeNimiRDTO nimi = tarjontaResource.getHakukohdeNimi(hakukohdeOid);
            cache.put(hakukohdeOid, nimi);
            return nimi;
        }
    }
}
