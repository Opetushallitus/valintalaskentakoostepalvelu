package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_EROTIN;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_VAKIO;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_VALI;
import static fi.vm.sade.valinta.kooste.util.Formatter.suomennaNumero;
import static fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HakemusUtil.ASIOINTIKIELI;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Property;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluIlmankoulutuspaikkaaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HakemusUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;

/**
 * @author Jussi Jartamo
 */
@Component
public class JalkiohjauskirjeetKomponentti {

    private static final Logger LOG = LoggerFactory.getLogger(JalkiohjauskirjeetKomponentti.class);
    private static final String TYHJA_TARJOAJANIMI = "Tuntematon koulu!";
    private static final String TYHJA_HAKUKOHDENIMI = "Tuntematon koulutus!";

    private SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluProxy;
    private HaeHakukohdeNimiTarjonnaltaKomponentti tarjontaProxy;
    private HaeOsoiteKomponentti osoiteKomponentti;
    private HaeHakemusKomponentti hakemusProxy;

    @Autowired
    public JalkiohjauskirjeetKomponentti(SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluProxy,
            HaeHakukohdeNimiTarjonnaltaKomponentti tarjontaProxy, HaeOsoiteKomponentti osoiteKomponentti) {
        this.sijoitteluProxy = sijoitteluProxy;
        this.tarjontaProxy = tarjontaProxy;
        this.osoiteKomponentti = osoiteKomponentti;
    }

    private String vakioHakukohteenNimi(String hakukohdeOid) {
        return new StringBuilder().append("Hakukohteella ").append(hakukohdeOid).append(" ei ole hakukohteennimeä")
                .toString();
    }

    private String vakioTarjoajanNimi(String hakukohdeOid) {
        return new StringBuilder().append("Hakukohteella ").append(hakukohdeOid).append(" ei ole tarjojannimeä")
                .toString();
    }

    public Kirjeet teeJalkiohjauskirjeet(@Property(OPH.HAKUOID) String hakuOid) {
        LOG.debug("Jalkiohjauskirjeet for haku '{}'", new Object[] { hakuOid });
        final List<HakijaDTO> hyvaksymattomatHakijat = sijoitteluProxy.ilmankoulutuspaikkaa(hakuOid,
                SijoitteluResource.LATEST);
        final int kaikkiHyvaksymattomat = hyvaksymattomatHakijat.size();
        if (kaikkiHyvaksymattomat == 0) {
            LOG.error("Jälkiohjauskirjeitä yritetään luoda haulle jolla kaikki hakijat on hyväksytty koulutukseen!");
            viestintapalveluLogi("Sijoittelupalvelun mukaan kaikki hakijat on hyväksytty johonkin koulutukseen!");
            throw new SijoittelupalveluException(
                    "Sijoittelupalvelun mukaan kaikki hakijat on hyväksytty johonkin koulutukseen!");
        }
        final Map<String, MetaHakukohde> jalkiohjauskirjeessaKaytetytHakukohteet = haeKiinnostavatHakukohteet(hyvaksymattomatHakijat);
        final List<Kirje> kirjeet = new ArrayList<Kirje>();
        for (HakijaDTO hakija : hyvaksymattomatHakijat) {
            final String hakemusOid = hakija.getHakemusOid();
            final Hakemus hakemus = hakemusProxy.haeHakemus(hakemusOid);
            final Osoite osoite = osoiteKomponentti.haeOsoite(hakemus);
            final List<Map<String, String>> tulosList = new ArrayList<Map<String, String>>();

            String preferoituKielikoodi;
            try {
                preferoituKielikoodi = KieliUtil.normalisoiKielikoodi(hakemus.getAnswers().getLisatiedot()
                        .get(ASIOINTIKIELI));
            } catch (Exception e) {
                LOG.error("Hakemuksella {} ei ollut asiointikielta!", hakemusOid);
                preferoituKielikoodi = KieliUtil.SUOMI;
            }

            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                String hakukohdeOid = hakutoive.getHakukohdeOid();
                MetaHakukohde metakohde = jalkiohjauskirjeessaKaytetytHakukohteet.get(hakukohdeOid);
                Map<String, String> tulokset = new HashMap<String, String>();

                tulokset.put("hakukohteenNimi",
                        metakohde.getHakukohdeNimi()
                                .getTeksti(preferoituKielikoodi, vakioHakukohteenNimi(hakukohdeOid)));
                tulokset.put("oppilaitoksenNimi", ""); // tieto on jo osana
                                                       // hakukohdenimea
                                                       // joten
                                                       // tuskin tarvii
                                                       // toistaa
                tulokset.put("hylkayksenSyy", StringUtils.EMPTY);

                StringBuilder pisteet = new StringBuilder();
                for (PistetietoDTO pistetieto : hakutoive.getPistetiedot()) {
                    if (pistetieto.getArvo() != null) {
                        try {
                            BigDecimal ehkaNumeroEhkaTotuusarvo = new BigDecimal(pistetieto.getArvo());
                            pisteet.append(suomennaNumero(ehkaNumeroEhkaTotuusarvo)).append(ARVO_VALI);
                        } catch (NumberFormatException notNumber) {
                            // OVT-6340 filtteroidaan totuusarvot pois
                        }
                    }
                }
                tulokset.put("paasyJaSoveltuvuuskoe", pisteet.toString().trim());
                tulokset.put("organisaationNimi",
                        metakohde.getTarjoajaNimi().getTeksti(preferoituKielikoodi, vakioTarjoajanNimi(hakukohdeOid)));
                StringBuilder omatPisteet = new StringBuilder();
                StringBuilder hyvaksytyt = new StringBuilder();
                for (HakutoiveenValintatapajonoDTO valintatapajono : hakutoive.getHakutoiveenValintatapajonot()) {
                    //
                    // OVT-6334 : Logiikka ei kuulu koostepalveluun!
                    //
                    if (osoite.isUlkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt()) { // ei
                                                                                                    // pisteita
                        omatPisteet.append(ARVO_VAKIO).append(ARVO_EROTIN)
                                .append(suomennaNumero(valintatapajono.getAlinHyvaksyttyPistemaara(), ARVO_VAKIO))
                                .append(ARVO_VALI);
                    } else {
                        omatPisteet.append(suomennaNumero(valintatapajono.getPisteet(), ARVO_VAKIO))
                                .append(ARVO_EROTIN)
                                .append(suomennaNumero(valintatapajono.getAlinHyvaksyttyPistemaara(), ARVO_VAKIO))
                                .append(ARVO_VALI);
                    }
                    hyvaksytyt.append(suomennaNumero(valintatapajono.getHyvaksytty(), ARVO_VAKIO)).append(ARVO_EROTIN)
                            .append(suomennaNumero(valintatapajono.getHakeneet(), ARVO_VAKIO)).append(ARVO_VALI);
                    // Ylikirjoittuu viimeisella arvolla jos valintatapajonoja
                    // on useampi
                    // Nykyinen PDF formaatti ei kykene esittamaan usean jonon
                    // selitteita jarkevasti
                    if (VARALLA.equals(valintatapajono.getTila()) && valintatapajono.getVarasijanNumero() != null) {
                        tulokset.put("selite", "Varasijan numero on " + valintatapajono.getVarasijanNumero());
                    } else {
                        tulokset.put("selite", StringUtils.EMPTY);
                    }
                    tulokset.put(
                            "valinnanTulos",
                            HakemusUtil.tilaConverter(valintatapajono.getTila(),
                                    valintatapajono.isHyvaksyttyHarkinnanvaraisesti()));
                    if (valintatapajono.getHyvaksytty() == null) {
                        viestintapalveluLogi("Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo hyväksyt.");
                        throw new SijoittelupalveluException(
                                "Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo hyväksyt.");
                    }
                    if (valintatapajono.getHakeneet() == null) {
                        viestintapalveluLogi("Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo kaikki hakeneet.");
                        throw new SijoittelupalveluException(
                                "Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo kaikki hakeneet.");
                    }
                }
                tulokset.put("omatPisteet", omatPisteet.toString());
                tulokset.put("hyvaksytyt", hyvaksytyt.toString());
                tulokset.put("alinHyvaksyttyPistemaara", StringUtils.EMPTY);
                tulokset.put("kaikkiHakeneet", StringUtils.EMPTY);
                tulosList.add(tulokset);
            }
            kirjeet.add(new Kirje(osoite, preferoituKielikoodi, tulosList));
        }

        LOG.info("Yritetään luoda viestintapalvelulta jälkiohjauskirjeitä {} kappaletta!", kirjeet.size());
        Kirjeet viesti = new Kirjeet(kirjeet);
        LOG.debug("\r\n{}", new ViestiWrapper(viesti));
        // Response response =
        // viestintapalveluProxy.haeJalkiohjauskirjeet(viesti);
        viestintapalveluLogi("Tiedot jälkiohjauskirjeen luontiin on välitetty viestintäpalvelulle.");
        return viesti; // response.getEntity();
    }

    private void viestintapalveluLogi(String logiViesti) {
        try {
            // messageProxy.message(logiViesti);
        } catch (Exception ex) {
            LOG.error("Viestintäpalvelun message rajapinta ei ole käytettävissä! {}", logiViesti);
        }
    }

    //
    // Hakee kaikki hyvaksymiskirjeen kohteena olevan hakukohteen hakijat ja
    // niihin liittyvat hakukohteet - eli myos hakijoiden hylatyt hakukohteet!
    // Metahakukohteille haetaan muun muassa tarjoajanimi!
    //
    private Map<String, MetaHakukohde> haeKiinnostavatHakukohteet(List<HakijaDTO> hakukohteenHakijat) {
        Map<String, MetaHakukohde> metaKohteet = new HashMap<String, MetaHakukohde>();
        for (HakijaDTO hakija : hakukohteenHakijat) {
            for (HakutoiveDTO hakutoive : hakija.getHakutoiveet()) {
                String hakukohdeOid = hakutoive.getHakukohdeOid();
                if (!metaKohteet.containsKey(hakukohdeOid)) { // lisataan
                                                              // puuttuva
                                                              // hakukohde
                    try {
                        HakukohdeNimiRDTO nimi = tarjontaProxy.haeHakukohdeNimi(hakukohdeOid);
                        Teksti hakukohdeNimi = new Teksti(nimi.getHakukohdeNimi());// extractHakukohdeNimi(nimi,
                                                                                   // kielikoodi);
                        Teksti tarjoajaNimi = new Teksti(nimi.getTarjoajaNimi());// extractTarjoajaNimi(nimi,
                                                                                 // kielikoodi);
                        metaKohteet.put(hakukohdeOid, new MetaHakukohde(hakukohdeNimi, tarjoajaNimi));
                    } catch (Exception e) {
                        e.printStackTrace();
                        LOG.error("Tarjonnasta ei saatu hakukohdetta {}: {}",
                                new Object[] { hakukohdeOid, e.getMessage() });
                        viestintapalveluLogi("Tarjonnasta ei löytynyt hakukohdetta oid:lla " + hakukohdeOid);
                        metaKohteet.put(
                                hakukohdeOid,
                                new MetaHakukohde(new Teksti(new StringBuilder().append("Hakukohde ")
                                        .append(hakukohdeOid).append(" ei löydy tarjonnasta!").toString()), new Teksti(
                                        TYHJA_TARJOAJANIMI)));
                    }

                }
            }
        }
        return metaKohteet;
    }

}
