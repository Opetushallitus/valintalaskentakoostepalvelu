package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.camel.Property;
import org.apache.camel.language.Simple;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.exception.HakemuspalveluException;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.sijoittelu.proxy.SijoitteluKoulutuspaikallisetProxy;
import fi.vm.sade.valinta.kooste.tarjonta.TarjontaNimiProxy;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HakemuksenTilaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.ViestintapalveluHyvaksymiskirjeetProxy;

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
    private SijoitteluKoulutuspaikallisetProxy sijoitteluProxy;

    @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}")
    private String sijoitteluResourceUrl;

    @Autowired
    private TarjontaNimiProxy tarjontaProxy;

    @Autowired
    private HaeOsoiteKomponentti osoiteKomponentti;

    @Autowired
    private ViestintapalveluHyvaksymiskirjeetProxy viestintapalveluProxy;

    public Object teeHyvaksymiskirjeet(@Property("kielikoodi") String kielikoodi,
            @Simple("${property.hakukohdeOid}") String hakukohdeOid, @Simple("${property.hakuOid}") String hakuOid,
            @Simple("${property.sijoitteluajoId}") Long sijoitteluajoId) {

        LOG.debug("Hyvaksymiskirjeet for hakukohde '{}' and haku '{}' and sijoitteluajo '{}'", new Object[] {
                hakukohdeOid, hakuOid, sijoitteluajoId });
        assert (hakukohdeOid != null);
        assert (hakuOid != null);
        assert (sijoitteluajoId != null);
        //
        //
        //
        final Collection<HakijaDTO> hakukohteenHakijat = sijoitteluProxy.koulutuspaikalliset(hakuOid, hakukohdeOid,
                sijoitteluajoId.toString());
        final int kaikkiHakukohteenHyvaksytyt = hakukohteenHakijat.size();
        if (kaikkiHakukohteenHyvaksytyt == 0) {
            LOG.error("Hyväksymiskirjeitä yritetään luoda hakukohteelle {} millä ei ole hyväksyttyjä hakijoita!",
                    hakukohdeOid);
            throw new HakemuspalveluException(
                    "Hakukohteella on oltava vähintään yksi hyväksytty hakija että hyväksymiskirjeet voidaan luoda!");
        }
        final Map<String, MetaHakukohde> hyvaksymiskirjeessaKaytetytHakukohteet = haeKiinnostavatHakukohteet(
                hakukohteenHakijat, kielikoodi);
        final List<Kirje> kirjeet = new ArrayList<Kirje>();
        final String koulu;
        final String koulutus;
        {
            MetaHakukohde metakohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakukohdeOid);
            koulu = metakohde.getTarjoajaNimi();
            koulutus = metakohde.getHakukohdeNimi();
        }
        for (HakijaDTO hakija : hakukohteenHakijat) {
            final String hakemusOid = hakija.getHakemusOid();
            final Osoite osoite = osoiteKomponentti.haeOsoite(hakemusOid);
            final List<Map<String, String>> tulosList = new ArrayList<Map<String, String>>();
            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive.getHakutoiveenValintatapajonot()) {

                    MetaHakukohde metakohde = hyvaksymiskirjeessaKaytetytHakukohteet.get(hakutoive.getHakukohdeOid());
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
                        throw new SijoittelupalveluException(
                                "Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo hyväksyt.");
                    }
                    tulokset.put("hyvaksytyt", Formatter.suomennaNumero(valintatapajono.getHyvaksytty()));
                    if (valintatapajono.getHakeneet() == null) {
                        throw new SijoittelupalveluException(
                                "Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo kaikki hakeneet.");
                    }
                    tulokset.put("kaikkiHakeneet", Formatter.suomennaNumero(valintatapajono.getHakeneet()));
                    StringBuilder pisteet = new StringBuilder();
                    for (PistetietoDTO pistetieto : hakutoive.getPistetiedot()) {
                        if (pistetieto.getArvo() != null) {
                            pisteet.append(pistetieto.getArvo()).append(" ");
                        }
                    }
                    tulokset.put("omatPisteet", pisteet.toString().trim());
                    tulokset.put("organisaationNimi", metakohde.getTarjoajaNimi());
                    tulokset.put("paasyJaSoveltuvuuskoe",
                            Formatter.suomennaNumero(valintatapajono.getPaasyJaSoveltuvuusKokeenTulos()));
                    if (VARALLA.equals(valintatapajono.getTila()) && valintatapajono.getVarasijanNumero() != null) {
                        tulokset.put("selite", "Varasijan numero on " + valintatapajono.getVarasijanNumero());
                    } else {
                        tulokset.put("selite", StringUtils.EMPTY);
                    }
                    tulokset.put(
                            "valinnanTulos",
                            HakemuksenTilaUtil.tilaConverter(valintatapajono.getTila(),
                                    valintatapajono.isHyvaksyttyHarkinnanvaraisesti()));
                    tulosList.add(tulokset);
                }
            }
            kirjeet.add(new Kirje(osoite, "FI", koulu, koulutus, tulosList));
        }

        LOG.info("Yritetään luoda viestintapalvelulta hyvaksymiskirjeitä {} kappaletta!", kirjeet.size());
        Kirjeet viesti = new Kirjeet(kirjeet);
        LOG.debug("\r\n{}", new ViestiWrapper(viesti));
        Response response = viestintapalveluProxy.haeHyvaksymiskirjeet(viesti);
        return response.getEntity();
    }

    //
    // Hakee kaikki hyvaksymiskirjeen kohteena olevan hakukohteen hakijat ja
    // niihin liittyvat hakukohteet - eli myos hakijoiden hylatyt hakukohteet!
    // Metahakukohteille haetaan muun muassa tarjoajanimi!
    //
    private Map<String, MetaHakukohde> haeKiinnostavatHakukohteet(Collection<HakijaDTO> hakukohteenHakijat,
            String kielikoodi) {
        Map<String, MetaHakukohde> metaKohteet = new HashMap<String, MetaHakukohde>();
        for (HakijaDTO hakija : hakukohteenHakijat) {
            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                String hakukohdeOid = hakutoive.getHakukohdeOid();
                if (!metaKohteet.containsKey(hakukohdeOid)) { // lisataan
                                                              // puuttuva
                                                              // hakukohde
                    HakukohdeNimiRDTO nimi = tarjontaProxy.haeHakukohdeNimi(hakukohdeOid);
                    String hakukohdeNimi = extractHakukohdeNimi(nimi, kielikoodi);
                    String tarjoajaNimi = extractTarjoajaNimi(nimi, kielikoodi);
                    metaKohteet.put(hakukohdeOid, new MetaHakukohde(hakukohdeNimi, tarjoajaNimi));
                }
            }
        }
        return metaKohteet;
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

}
