package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.camel.Property;
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
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.sijoittelu.proxy.SijoitteluIlmankoulutuspaikkaaProxy;
import fi.vm.sade.valinta.kooste.tarjonta.TarjontaNimiProxy;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HakemuksenTilaUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.ViestintapalveluJalkiohjauskirjeetProxy;

/**
 * @author Jussi Jartamo
 */
@Component("jalkiohjauskirjeetKomponentti")
public class JalkiohjauskirjeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetKomponentti.class);
    private static final String TYHJA_TARJOAJANIMI = "Tuntematon koulu!";
    private static final String TYHJA_HAKUKOHDENIMI = "Tuntematon koulutus!";

    @Autowired
    private SijoitteluIlmankoulutuspaikkaaProxy sijoitteluProxy;
    // private SijoitteluResource sijoitteluResource;

    @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}")
    private String sijoitteluResourceUrl;

    @Autowired
    private TarjontaNimiProxy tarjontaProxy;

    @Autowired
    private HaeOsoiteKomponentti osoiteKomponentti;

    @Autowired
    private ViestintapalveluJalkiohjauskirjeetProxy viestintapalveluProxy;

    public Object teeJalkiohjauskirjeet(@Property("kielikoodi") String kielikoodi, @Property("hakuOid") String hakuOid) {
        LOG.debug("Jalkiohjauskirjeet for haku '{}'", new Object[] { hakuOid });
        final List<HakijaDTO> hyvaksymattomatHakijat = sijoitteluProxy.ilmankoulutuspaikkaa(hakuOid,
                SijoitteluResource.LATEST);
        final int kaikkiHyvaksymattomat = hyvaksymattomatHakijat.size();
        if (kaikkiHyvaksymattomat == 0) {
            LOG.error("Jälkiohjauskirjeitä yritetään luoda haulle jolla kaikki hakijat on hyväksytty koulutukseen!");
            throw new SijoittelupalveluException(
                    "Sijoittelupalvelun mukaan kaikki hakijat on hyväksytty johonkin koulutukseen!");
        }
        final Map<String, MetaHakukohde> jalkiohjauskirjeessaKaytetytHakukohteet = haeKiinnostavatHakukohteet(
                hyvaksymattomatHakijat, kielikoodi);
        final List<Kirje> kirjeet = new ArrayList<Kirje>();
        for (HakijaDTO hakija : hyvaksymattomatHakijat) {
            final String hakemusOid = hakija.getHakemusOid();
            final Osoite osoite = osoiteKomponentti.haeOsoite(hakemusOid);
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
            kirjeet.add(new Kirje(osoite, "FI", tulosList));
        }

        LOG.info("Yritetään luoda viestintapalvelulta jälkiohjauskirjeitä {} kappaletta!", kirjeet.size());
        Kirjeet viesti = new Kirjeet(kirjeet);
        LOG.debug("\r\n{}", new ViestiWrapper(viesti));
        Response response = viestintapalveluProxy.haeJalkiohjauskirjeet(viesti);
        return response.getEntity();
    }

    //
    // Hakee kaikki hyvaksymiskirjeen kohteena olevan hakukohteen hakijat ja
    // niihin liittyvat hakukohteet - eli myos hakijoiden hylatyt hakukohteet!
    // Metahakukohteille haetaan muun muassa tarjoajanimi!
    //
    private Map<String, MetaHakukohde> haeKiinnostavatHakukohteet(List<HakijaDTO> hakukohteenHakijat, String kielikoodi) {
        Map<String, MetaHakukohde> metaKohteet = new HashMap<String, MetaHakukohde>();
        for (HakijaDTO hakija : hakukohteenHakijat) {
            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                String hakukohdeOid = hakutoive.getHakukohdeOid();
                if (!metaKohteet.containsKey(hakukohdeOid)) { // lisataan
                                                              // puuttuva
                                                              // hakukohde
                    try {
                        HakukohdeNimiRDTO nimi = tarjontaProxy.haeHakukohdeNimi(hakukohdeOid);
                        String hakukohdeNimi = extractHakukohdeNimi(nimi, kielikoodi);
                        String tarjoajaNimi = extractTarjoajaNimi(nimi, kielikoodi);
                        metaKohteet.put(hakukohdeOid, new MetaHakukohde(hakukohdeNimi, tarjoajaNimi));
                    } catch (Exception e) {
                        e.printStackTrace();
                        LOG.error("Tarjonnasta ei saatu hakukohdetta {}: {}",
                                new Object[] { hakukohdeOid, e.getMessage() });
                        metaKohteet.put(hakukohdeOid, new MetaHakukohde(new StringBuilder().append("Hakukohde ")
                                .append(hakukohdeOid).append(" ei löydy tarjonnasta!").toString(), TYHJA_TARJOAJANIMI));
                    }

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
