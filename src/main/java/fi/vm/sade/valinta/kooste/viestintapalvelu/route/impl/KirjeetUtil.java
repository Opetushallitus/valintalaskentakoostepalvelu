package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HARKINNANVARAISESTI_HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYLATTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUUNTUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUUTETTU;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARASIJALTA_HYVAKSYTTY;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_EROTIN;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_VAKIO;
import static fi.vm.sade.valinta.kooste.util.Formatter.ARVO_VALI;
import static fi.vm.sade.valinta.kooste.util.Formatter.suomennaNumero;

import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.util.HakemusUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Pisteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.Sijoitus;
import fi.vm.sade.valintalaskenta.domain.dto.SyotettyArvoDTO;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KirjeetUtil {
  private static final Map<HakemuksenTila, Integer> tilaToPrioriteetti = Maps.newHashMap();
  private static final Logger LOG = LoggerFactory.getLogger(KirjeetUtil.class);

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

  public static void jononTulokset(
      Osoite osoite,
      HakutoiveDTO hakutoive,
      StringBuilder omatPisteet,
      StringBuilder hyvaksytyt,
      List<Sijoitus> jononTulokset,
      String preferoitukielikoodi) {
    for (HakutoiveenValintatapajonoDTO valintatapajono :
        hakutoive.getHakutoiveenValintatapajonot()) {
      try {
        BigDecimal numeerisetPisteet = valintatapajono.getPisteet();
        BigDecimal alinHyvaksyttyPistemaara = valintatapajono.getAlinHyvaksyttyPistemaara();
        BigDecimal ensikertalaisenMinimipisteet =
            hakutoive.getEnsikertalaisuusHakijaryhmanAlimmatHyvaksytytPisteet();
        Pisteet jononPisteet =
            KirjeetUtil.asPisteetData(
                numeerisetPisteet,
                alinHyvaksyttyPistemaara,
                ensikertalaisenMinimipisteet,
                valintatapajono.getJonosija());

        String varasijaTeksti =
            varasijanumeroTeksti(valintatapajono, preferoitukielikoodi).orElse(null);
        jononTulokset.add(
            new Sijoitus(valintatapajono, varasijaTeksti, jononPisteet, preferoitukielikoodi));

        KirjeetUtil.putNumeerisetPisteetAndAlinHyvaksyttyPistemaara(
            osoite,
            omatPisteet,
            numeerisetPisteet,
            alinHyvaksyttyPistemaara,
            valintatapajono.getJonosija());
        KirjeetUtil.putHyvaksyttyHakeneetData(hyvaksytyt, valintatapajono);
        if (valintatapajono.getHyvaksytty() == null) {
          throw new SijoittelupalveluException(
              "Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo hyväksytty.");
        }
        if (valintatapajono.getHakeneet() == null) {
          throw new SijoittelupalveluException(
              "Sijoittelu palautti puutteellisesti luodun valintatapajonon! Määrittelemätön arvo kaikki hakeneet.");
        }
      } catch (Exception e) {
        throw new RuntimeException(
            String.format(
                "Valintatapajonon %s datan käsittely epäonnistui",
                valintatapajono.getValintatapajonoOid()),
            e);
      }
    }
  }

  public static Comparator<HakutoiveenValintatapajonoDTO> sortByPrioriteetti() {
    // return Comparator.comparing(sortByPrioriteetti()).thenComparing(sortByTila());
    return Comparator.comparing(sortByPrioriteettiFunction());
  }

  private static Function<HakutoiveenValintatapajonoDTO, Integer> sortByPrioriteettiFunction() {
    return (jono) ->
        Optional.ofNullable(jono)
            .map(HakutoiveenValintatapajonoDTO::getValintatapajonoPrioriteetti)
            .orElse(0);
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

  public static StringBuilder createPaasyJaSoveltuvuuskoePisteet(
      List<SyotettyArvoDTO> syotetytArvot) {
    StringBuilder pisteet = new StringBuilder();
    for (SyotettyArvoDTO pistetieto : syotetytArvot) {
      try {
        BigDecimal numero =
            new BigDecimal(StringUtils.trimToEmpty(pistetieto.getArvo()).replace(",", "."));
        pisteet.append(suomennaNumero(numero)).append(ARVO_VALI);
      } catch (NumberFormatException notNumber) {
        // Lisätään vain lukutyyppiset arvot
      }
    }
    return pisteet;
  }

  public static String hylkaysPerusteText(
      String preferoituKielikoodi,
      List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot) {
    HakutoiveenValintatapajonoDTO lastJono =
        hakutoiveenValintatapajonot.get(hakutoiveenValintatapajonot.size() - 1);
    return HYLATTY.equals(hakutoiveenValintatapajonot.get(0).getTila())
        ? new Teksti(lastJono.getTilanKuvaukset()).getTeksti(preferoituKielikoodi, "")
        : "";
  }

  public static String peruuntumisenSyyText(
      String preferoituKielikoodi,
      List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot) {
    HakutoiveenValintatapajonoDTO hakutoiveenValintatapajono = hakutoiveenValintatapajonot.get(0);
    return PERUUNTUNUT.equals(hakutoiveenValintatapajono.getTila())
        ? new Teksti(hakutoiveenValintatapajono.getTilanKuvaukset())
            .getTeksti(preferoituKielikoodi, "")
        : "";
  }

  public static void putValinnanTulosHylkausPerusteAndVarasijaData(
      String preferoituKielikoodi,
      Map<String, Object> tulokset,
      List<HakutoiveenValintatapajonoDTO> hakutoiveenValintatapajonot) {
    if (!hakutoiveenValintatapajonot.isEmpty()) {
      HakutoiveenValintatapajonoDTO firstValintatapajono = hakutoiveenValintatapajonot.get(0);
      varasijanumeroTeksti(firstValintatapajono, preferoituKielikoodi)
          .ifPresent(varasijaTeksti -> tulokset.put("varasija", varasijaTeksti));
      String hyvaksymisenEhto =
          Teksti.ehdollisenHyvaksymisenEhto(firstValintatapajono)
              .getTeksti(preferoituKielikoodi, null);
      if (hyvaksymisenEhto != null) {
        tulokset.put("hyvaksymisenEhto", hyvaksymisenEhto);
      }
      tulokset.put(
          "hylkaysperuste",
          StringUtils.trimToNull(
              hylkaysPerusteText(preferoituKielikoodi, hakutoiveenValintatapajonot)));
      tulokset.put(
          "peruuntumisenSyy",
          StringUtils.trimToNull(
              peruuntumisenSyyText(preferoituKielikoodi, hakutoiveenValintatapajonot)));
      tulokset.put(
          "valinnanTulos", HakemusUtil.tilaConverter(firstValintatapajono, preferoituKielikoodi));
    }
  }

  public static void putHyvaksyttyHakeneetData(
      StringBuilder hyvaksytyt, HakutoiveenValintatapajonoDTO valintatapajono) {
    // Ylikirjoittuu viimeisella arvolla jos valintatapajonoja on useampi
    // Nykyinen PDF formaatti ei kykene esittamaan usean jonon selitteita jarkevasti
    hyvaksytyt
        .append(suomennaNumero(valintatapajono.getHyvaksytty(), ARVO_VAKIO))
        .append(ARVO_EROTIN)
        .append(suomennaNumero(valintatapajono.getHakeneet(), ARVO_VAKIO))
        .append(ARVO_VALI);
  }

  private static boolean pisteetOnNegatiivinenJonosija(
      BigDecimal numeerisetPisteet, Integer jonosija) {
    if (numeerisetPisteet == null || jonosija == null) return false;
    else return jonosija * (-1) == numeerisetPisteet.intValue();
  }

  public static void putNumeerisetPisteetAndAlinHyvaksyttyPistemaara(
      Osoite osoite,
      StringBuilder omatPisteet,
      BigDecimal numeerisetPisteet,
      BigDecimal alinHyvaksyttyPistemaara,
      Integer jonosija) {
    // OVT-6334 : Logiikka ei kuulu koostepalveluun!
    if (osoite.isUlkomaillaSuoritettuKoulutusTaiOppivelvollisuudenKeskeyttanyt()) {
      // ei pisteita
      omatPisteet
          .append(ARVO_VAKIO)
          .append(ARVO_EROTIN)
          .append(suomennaNumero(alinHyvaksyttyPistemaara, ARVO_VAKIO))
          .append(ARVO_VALI);
    } else if (pisteetOnNegatiivinenJonosija(numeerisetPisteet, jonosija)) {
      omatPisteet.append(ARVO_VAKIO).append(ARVO_EROTIN).append(ARVO_VAKIO).append(ARVO_VALI);
    } else {
      omatPisteet
          .append(suomennaNumero(numeerisetPisteet, ARVO_VAKIO))
          .append(ARVO_EROTIN)
          .append(suomennaNumero(alinHyvaksyttyPistemaara, ARVO_VAKIO))
          .append(ARVO_VALI);
    }
  }

  public static String vakioHakukohteenNimi(String hakukohdeOid) {
    return "Hakukohteella " + hakukohdeOid + " ei ole hakukohteennimeä";
  }

  public static String vakioTarjoajanNimi(String hakukohdeOid) {
    return "Hakukohteella " + hakukohdeOid + " ei ole tarjojannimeä";
  }

  public static Map<String, Object> getTuloksetMap(
      Map<String, MetaHakukohde> kirjeessaKaytetytHakukohteet,
      String hakukohdeOid,
      String preferoituKielikoodi,
      HakutoiveDTO hakutoive,
      List<SyotettyArvoDTO> syotetytArvot) {
    Map<String, Object> tulokset = new HashMap<>();
    MetaHakukohde metakohde = kirjeessaKaytetytHakukohteet.get(hakutoive.getHakukohdeOid());
    tulokset.put(
        "hakukohteenNimi",
        metakohde
            .getHakukohdeNimi()
            .getTeksti(preferoituKielikoodi, vakioHakukohteenNimi(hakukohdeOid)));
    tulokset.put(
        "organisaationNimi",
        metakohde.getTarjoajaNimet().stream()
            .map(
                t ->
                    t.getTeksti(preferoituKielikoodi, KirjeetUtil.vakioTarjoajanNimi(hakukohdeOid)))
            .collect(Collectors.joining(" - ")));
    tulokset.put(
        "oppilaitoksenNimi", ""); // tieto on jo osana hakukohdenimea joten tuskin tarvii toistaa
    tulokset.put("hylkayksenSyy", "");
    tulokset.put("alinHyvaksyttyPistemaara", "");
    tulokset.put("kaikkiHakeneet", "");
    tulokset.put(
        "paasyJaSoveltuvuuskoe",
        createPaasyJaSoveltuvuuskoePisteet(syotetytArvot).toString().trim());
    return tulokset;
  }

  private static Optional<String> varasijanumeroTeksti(
      HakutoiveenValintatapajonoDTO valintatapajono, String preferoituKielikoodi) {
    if (VARALLA.equals(valintatapajono.getTila())
        && valintatapajono.getVarasijanNumero() != null
        && !valintatapajono.isEiVarasijatayttoa()) {
      return Optional.of(
          HakemusUtil.varasijanNumeroConverter(
              valintatapajono.getVarasijanNumero(), preferoituKielikoodi));
    }
    return Optional.empty();
  }

  private static BigDecimal notNegative(BigDecimal bb) {
    return Optional.ofNullable(bb).filter(b -> b.signum() != -1).orElse(null);
  }

  public static Pisteet asPisteetData(
      BigDecimal numeerisetPisteet,
      BigDecimal alinHyvaksyttyPistemaara,
      BigDecimal ensikertalaisenMinimipisteet,
      Integer jonosija) {
    String kkPiste = null;
    String kkMinimi = suomennaNumero(notNegative(alinHyvaksyttyPistemaara), null);
    String kkEnskertMinimi = suomennaNumero(notNegative(ensikertalaisenMinimipisteet), null);
    if (numeerisetPisteet != null && jonosija != null) {
      if (!(jonosija * (-1) == numeerisetPisteet.intValue())) {
        kkPiste = suomennaNumero(numeerisetPisteet, null);
      }
    }
    Pisteet pisteetResult = new Pisteet(kkPiste, kkMinimi, kkEnskertMinimi);
    return pisteetResult;
  }
}
