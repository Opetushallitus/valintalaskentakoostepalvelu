package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.PistetietoDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.util.HakemusUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Pisteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Sijoitus;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.*;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.*;
import static fi.vm.sade.valinta.kooste.util.Formatter.*;
import static java.util.Optional.ofNullable;

public class KirjeetUtil {
    static final Map<HakemuksenTila, Integer> tilaToPrioriteetti = Maps.newHashMap();
    static {
        tilaToPrioriteetti.put(HARKINNANVARAISESTI_HYVAKSYTTY, 1);
        tilaToPrioriteetti.put(HYVAKSYTTY, 2);
        tilaToPrioriteetti.put(VARASIJALTA_HYVAKSYTTY, 3);
        tilaToPrioriteetti.put(VARALLA, 4);
        tilaToPrioriteetti.put(PERUNUT, 5);
        tilaToPrioriteetti.put(PERUUTETTU, 6);
        tilaToPrioriteetti.put(PERUUNTUNUT, 7);
        tilaToPrioriteetti.put(HYLATTY, 8);
    }

    public static Comparator<HakutoiveenValintatapajonoDTO> sortByTila() {
        return (o1, o2) -> {
            HakemuksenTila h1 = Optional.ofNullable(o1.getTila()).orElse(HYLATTY);
            HakemuksenTila h2 = Optional.ofNullable(o2.getTila()).orElse(HYLATTY);
            if (VARALLA.equals(h1) && VARALLA.equals(h2)) {
                Integer i1 = Optional.ofNullable(o1.getVarasijanNumero()).orElse(0);
                Integer i2 = Optional.ofNullable(o2.getVarasijanNumero()).orElse(0);
                return i1.compareTo(i2);
            }
            return tilaToPrioriteetti.get(h1).compareTo(tilaToPrioriteetti.get(h2));
        };
    }

    public static StringBuilder createPaasyJaSoveltuvuuskoePisteet(HakutoiveDTO hakutoive) {
        StringBuilder pisteet = new StringBuilder();
        for (PistetietoDTO pistetieto : hakutoive.getPistetiedot()) {
            if (pistetieto.getArvo() != null) {
                try {
                    String arvo = StringUtils.trimToEmpty(pistetieto.getArvo()).replace(",", ".");
                    BigDecimal ehkaNumeroEhkaTotuusarvo = new BigDecimal(arvo);
                    pisteet.append(suomennaNumero(ehkaNumeroEhkaTotuusarvo)).append(ARVO_VALI);
                } catch (NumberFormatException notNumber) {
                    // OVT-6340 filtteroidaan totuusarvot pois
                }
            }
        }
        return pisteet;
    }

    public static String hylkaysPerusteText(String preferoituKielikoodi, List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot) {
        HakutoiveenValintatapajonoDTO lastJono = hakutoiveenValintatapajonot.get(hakutoiveenValintatapajonot.size() - 1);
        return HYLATTY.equals(hakutoiveenValintatapajonot.get(0).getTila()) ? new Teksti(lastJono.getTilanKuvaukset()).getTeksti(preferoituKielikoodi, "") : "";
    }

    public static void putValinnanTulosHylkausPerusteAndVarasijaData(String preferoituKielikoodi, Map<String, Object> tulokset, List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot) {
        if (!hakutoiveenValintatapajonot.isEmpty()) {
            HakutoiveenValintatapajonoDTO firstValintatapajono = hakutoiveenValintatapajonot.get(0);
            if (VARALLA.equals(firstValintatapajono.getTila()) && firstValintatapajono.getVarasijanNumero() != null) {
                tulokset.put("varasija", HakemusUtil.varasijanNumeroConverter(firstValintatapajono.getVarasijanNumero(), preferoituKielikoodi));
            }
            tulokset.put("hylkaysperuste", hylkaysPerusteText(preferoituKielikoodi, hakutoiveenValintatapajonot));
            tulokset.put("valinnanTulos", HakemusUtil.tilaConverter(firstValintatapajono.getTila(), preferoituKielikoodi, firstValintatapajono.isHyvaksyttyHarkinnanvaraisesti()));
        }
    }

    public static void putHyvaksyttyHakeneetData(StringBuilder hyvaksytyt, HakutoiveenValintatapajonoDTO valintatapajono) {
        // Ylikirjoittuu viimeisella arvolla jos valintatapajonoja on useampi
        // Nykyinen PDF formaatti ei kykene esittamaan usean jonon selitteita jarkevasti
        hyvaksytyt.append(suomennaNumero(valintatapajono.getHyvaksytty(), ARVO_VAKIO))
                .append(ARVO_EROTIN)
                .append(suomennaNumero(valintatapajono.getHakeneet(), ARVO_VAKIO))
                .append(ARVO_VALI);
    }

    public static void putNumeerisetPisteetAndAlinHyvaksyttyPistemaara(Osoite osoite, StringBuilder omatPisteet, BigDecimal numeerisetPisteet, BigDecimal alinHyvaksyttyPistemaara) {
        // OVT-6334 : Logiikka ei kuulu koostepalveluun!
        if (osoite.isUlkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt()) {
            // ei pisteita
            omatPisteet.append(ARVO_VAKIO).append(ARVO_EROTIN)
                    .append(suomennaNumero(alinHyvaksyttyPistemaara, ARVO_VAKIO)).append(ARVO_VALI);
        } else {
            omatPisteet.append(suomennaNumero(numeerisetPisteet, ARVO_VAKIO))
                    .append(ARVO_EROTIN)
                    .append(suomennaNumero(alinHyvaksyttyPistemaara, ARVO_VAKIO)).append(ARVO_VALI);
        }
    }

    public static String vakioHakukohteenNimi(String hakukohdeOid) {
        return "Hakukohteella " + hakukohdeOid + " ei ole hakukohteennimeä";
    }

    public static String vakioTarjoajanNimi(String hakukohdeOid) {
        return "Hakukohteella " + hakukohdeOid + " ei ole tarjojannimeä";
    }

    public static Map<String, Object> getTuloksetMap(Map<String, MetaHakukohde> kirjeessaKaytetytHakukohteet, String hakukohdeOid, String preferoituKielikoodi, HakutoiveDTO hakutoive) {
        Map<String, Object> tulokset = new HashMap<>();
        MetaHakukohde metakohde = kirjeessaKaytetytHakukohteet.get(hakutoive.getHakukohdeOid());
        tulokset.put("hakukohteenNimi", metakohde.getHakukohdeNimi().getTeksti(preferoituKielikoodi, vakioHakukohteenNimi(hakukohdeOid)));
        tulokset.put("organisaationNimi", metakohde.getTarjoajaNimi().getTeksti(preferoituKielikoodi, vakioTarjoajanNimi(hakukohdeOid)));
        tulokset.put("oppilaitoksenNimi", "");  // tieto on jo osana hakukohdenimea joten tuskin tarvii toistaa
        tulokset.put("hylkayksenSyy", "");
        tulokset.put("alinHyvaksyttyPistemaara", "");
        tulokset.put("kaikkiHakeneet", "");
        tulokset.put("paasyJaSoveltuvuuskoe", createPaasyJaSoveltuvuuskoePisteet(hakutoive).toString().trim());
        return tulokset;
    }

    public static void putSijoituksetData(List<Sijoitus> kkSijoitukset, boolean valittuHakukohteeseen, HakutoiveenValintatapajonoDTO valintatapajono, String kkNimi) {
        int kkHyvaksytyt = ofNullable(valintatapajono.getHyvaksytty()).orElse(0);
        Integer varasijanumero = (!valittuHakukohteeseen && valintatapajono.getTila().isVaralla()) ? valintatapajono.getVarasijanNumero() : null;
        kkSijoitukset.add(new Sijoitus(kkNimi, kkHyvaksytyt, varasijanumero));
    }

    public static void putPisteetData(List<Pisteet> kkPisteet, String kkNimi, BigDecimal numeerisetPisteet, BigDecimal alinHyvaksyttyPistemaara) {
        String kkPiste = suomennaNumero(ofNullable(numeerisetPisteet).orElse(BigDecimal.ZERO));
        String kkMinimi = suomennaNumero(ofNullable(alinHyvaksyttyPistemaara).orElse(BigDecimal.ZERO));
        // Negatiivisia pisteitä ei lähetetä eteenpäin. Oikea tarkastus olisi jättää
        // pisteet pois jos jono ei käytä laskentaa, tietoa ei kuitenkaan ole käsillä
        if (numeerisetPisteet != null
                && numeerisetPisteet.signum() != -1
                && alinHyvaksyttyPistemaara != null
                && alinHyvaksyttyPistemaara.signum() != -1) {
            kkPisteet.add(new Pisteet(kkNimi, kkPiste, kkMinimi));
        }
    }
}
