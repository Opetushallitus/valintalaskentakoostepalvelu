package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Property;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HakemuksenTilaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.OsoiteHakemukseltaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.exception.NoContentException;
import fi.vm.sade.valinta.kooste.viestintapalvelu.exception.NoReplyException;

/**
 * @author Jussi Jartamo
 */
@Component("jalkiohjauskirjeetKomponentti")
public class JalkiohjauskirjeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetKomponentti.class);
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

    public String teeJalkiohjauskirjeet(@Property("kielikoodi") String kielikoodi,
            @Simple("${property.hakukohdeOid}") String hakukohdeOid, @Simple("${property.hakuOid}") String hakuOid,
            @Simple("${property.sijoitteluajoId}") Long sijoitteluajoId,
            @Simple("${property.hakemukset}") List<SuppeaHakemus> hakemukset) {
        LOG.debug("Jalkiohjauskirjeet for hakukohde '{}' and haku '{}'", new Object[] { hakukohdeOid, hakuOid });
        final List<HakijaDTO> haunHakijat = sijoitteluResource.koulutuspaikalliset(hakuOid, sijoitteluajoId.toString());
        final List<HakijaDTO> hyvaksymattomatHakijat = sijoitteluResource.ilmankoulutuspaikkaa(hakuOid,
                sijoitteluajoId.toString());
        final int kaikkiHyvaksymattomat = hyvaksymattomatHakijat.size();
        if (kaikkiHyvaksymattomat == 0) {
            LOG.error("Hyväksymiskirjeitä yritetään luoda hakukohteelle {} millä ei ole hyväksyttyjä hakijoita!",
                    hakukohdeOid);
            throw new NoContentException(
                    "Hakukohteella on oltava vähintään yksi hyväksytty hakija että hyväksymiskirjeet voidaan luoda!");
        }
        final Map<String, MetaHakukohde> jalkiohjauskirjeessaKaytetytHakukohteet = haeKiinnostavatHakukohteet(
                haunHakijat, hyvaksymattomatHakijat, kielikoodi);
        final List<Kirje> kirjeet = new ArrayList<Kirje>();
        for (HakijaDTO hakija : hyvaksymattomatHakijat) {
            final String hakemusOid = hakija.getHakemusOid();
            final Osoite osoite = haeOsoite(hakemusOid);
            final List<Map<String, String>> tulosList = new ArrayList<Map<String, String>>();
            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive.getHakutoiveenValintatapajonot()) {
                    MetaHakukohde metakohde = jalkiohjauskirjeessaKaytetytHakukohteet.get(hakutoive.getHakukohdeOid());
                    Map<String, String> tulokset = new HashMap<String, String>();
                    tulokset.put("alinHyvaksyttyPistemaara",
                            Formatter.suomennaNumero(valintatapajono.getAlinHyvaksyttyPistemaara()));

                    tulokset.put("hakukohteenNimi", metakohde.getHakukohdeNimi());
                    tulokset.put("oppilaitoksenNimi", ""); // tieto on jo osana
                                                           // hakukohdenimea
                                                           // joten
                                                           // tuskin tarvii
                                                           // toistaa
                    tulokset.put("hylkayksenSyy", StringUtils.EMPTY);
                    if (valintatapajono.getHyvaksytty() == null) {
                        throw new NoContentException(
                                "Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo hyväksyt.");
                    }
                    tulokset.put("hyvaksytyt", Formatter.suomennaNumero(valintatapajono.getHyvaksytty()));
                    if (valintatapajono.getHakeneet() == null) {
                        throw new NoContentException(
                                "Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo kaikki hakeneet.");
                    }
                    tulokset.put("kaikkiHakeneet", Formatter.suomennaNumero(valintatapajono.getHakeneet()));
                    tulokset.put("omatPisteet", Formatter.suomennaNumero(valintatapajono.getPisteet()));

                    tulokset.put("organisaationNimi", metakohde.getTarjoajaNimi());
                    tulokset.put("paasyJaSoveltuvuuskoe",
                            Formatter.suomennaNumero(valintatapajono.getPaasyJaSoveltuvuusKokeenTulos()));
                    tulokset.put("selite", StringUtils.EMPTY);
                    tulokset.put("valinnanTulos",
                            HakemuksenTilaUtil.tilaConverter(valintatapajono.getTila().toString()));
                    tulosList.add(tulokset);
                }
            }
            kirjeet.add(new Kirje(osoite, "FI", tulosList));
        }

        LOG.info("Yritetään luoda viestintapalvelulta jälkiohjauskirjeitä {} kappaletta!", kirjeet.size());
        return new Gson().toJson(new Kirjeet(kirjeet));
    }

    //
    // Hakee kaikki hyvaksymiskirjeen kohteena olevan hakukohteen hakijat ja
    // niihin liittyvat hakukohteet - eli myos hakijoiden hylatyt hakukohteet!
    // Metahakukohteille haetaan muun muassa tarjoajanimi!
    //
    private Map<String, MetaHakukohde> haeKiinnostavatHakukohteet(List<HakijaDTO> haunHakijat,
            List<HakijaDTO> hakukohteenHakijat, String kielikoodi) {
        Map<String, MetaHakukohde> metaKohteet = new HashMap<String, MetaHakukohde>();
        for (HakijaDTO hakija : hakukohteenHakijat) {
            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                String hakukohdeOid = hakutoive.getHakukohdeOid();
                if (!metaKohteet.containsKey(hakukohdeOid)) { // lisataan
                                                              // puuttuva
                                                              // hakukohde
                    HakukohdeNimiRDTO nimi = haeHakukohdeNimi(hakukohdeOid);
                    String hakukohdeNimi = extractHakukohdeNimi(nimi, kielikoodi);
                    String tarjoajaNimi = extractTarjoajaNimi(nimi, kielikoodi);
                    metaKohteet.put(hakukohdeOid, new MetaHakukohde(hakukohdeNimi, tarjoajaNimi));
                }
            }
        }
        return metaKohteet;
    }

    private Osoite haeOsoite(String hakemusOid) {
        try {
            return OsoiteHakemukseltaUtil.osoiteHakemuksesta(applicationResource.getApplicationByOid(hakemusOid));
        } catch (Exception e) {
            LOG.error("Ei voitu hakea osoitetta Haku-palvelusta hakemukselle {}! {}", new Object[] { hakemusOid,
                    hakuAppResourceUrl });
            throw new NoReplyException(
                    "Hakemuspalvelu ei anna hakijoille osoitteita! Tarkista palvelun käyttöoikeudet.");
        }
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

    private HakukohdeNimiRDTO haeHakukohdeNimi(String hakukohdeOid) {
        if (hakukohdeOid == null) {
            throw new NoContentException(
                    "Sijoittelu palautti puutteellisesti luodun hakutoiveen! Hakukohteen tunniste puuttuu!");
        } else {
            LOG.debug("Yhteys {}, HakukohdeResource.getHakukohdeNimi({})", new Object[] { tarjontaResourceUrl,
                    hakukohdeOid });
            HakukohdeNimiRDTO nimi = tarjontaResource.getHakukohdeNimi(hakukohdeOid);
            return nimi;
        }
    }
}
