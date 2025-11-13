package fi.vm.sade.valinta.kooste.util;

import static fi.vm.sade.sijoittelu.domain.ValintatuloksenTila.*;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HARKINNANVARAISESTI_HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYLATTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.HYVAKSYTTY;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.PERUUNTUNUT;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARALLA;
import static fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila.VARASIJALTA_HYVAKSYTTY;

import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.domain.IlmoittautumisTila;
import fi.vm.sade.sijoittelu.domain.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HakemusUtil {
  private static final Logger LOG = LoggerFactory.getLogger(HakemusUtil.class);
  public static final Map<String, Map<HakemuksenTila, String>> TILAT = valmistaTilat(false);
  public static final Map<String, Map<HakemuksenTila, String>> TILAT_FOR_LETTER =
      valmistaTilat(true);
  private static final Map<String, String> VARASIJALLA = varasijallaTilat();
  private static final Map<String, String> VARASIJAT = varasijaTekstinTilat();
  private static final Map<String, String> EHDOLLINEN = ehdollinenTekstinTilat();
  private static final Map<String, Map<IlmoittautumisTila, String>> ILMOITTAUTUMISTILAT =
      ilmoittautumisTilat();
  private static final Map<String, Map<ValintatuloksenTila, String>> VALINTATULOKSEN_TILAT =
      valintatulostenTilat();

  private static Map<String, String> varasijallaTilat() {
    Map<String, String> m = Maps.newHashMap();
    m.put(KieliUtil.SUOMI, "varasijalla");
    m.put(KieliUtil.RUOTSI, "På reservplats");
    m.put(KieliUtil.ENGLANTI, "on the waiting list");
    return m;
  }

  private static Map<String, String> varasijaTekstinTilat() {
    Map<String, String> varasijaTekstinTilat = Maps.newHashMap();
    varasijaTekstinTilat.put(KieliUtil.SUOMI, "Varasijan numero on ");
    varasijaTekstinTilat.put(KieliUtil.RUOTSI, "Reservplatsens nummer är ");
    varasijaTekstinTilat.put(KieliUtil.ENGLANTI, "Waiting list number is ");
    return varasijaTekstinTilat;
  }

  private static Map<String, String> ehdollinenTekstinTilat() {
    Map<String, String> ehdollinenTekstinTilat = Maps.newHashMap();
    ehdollinenTekstinTilat.put(KieliUtil.SUOMI, " (Ehdollinen)");
    ehdollinenTekstinTilat.put(KieliUtil.RUOTSI, " (Villkorlig)");
    ehdollinenTekstinTilat.put(KieliUtil.ENGLANTI, " (Conditionally)");
    return ehdollinenTekstinTilat;
  }

  private static Map<String, Map<ValintatuloksenTila, String>> valintatulostenTilat() {
    Map<String, Map<ValintatuloksenTila, String>> kielet =
        new HashMap<String, Map<ValintatuloksenTila, String>>();
    Map<ValintatuloksenTila, String> fi = new HashMap<>();
    fi.put(VASTAANOTTANUT_SITOVASTI, "Vastaanottanut sitovasti");
    fi.put(EI_VASTAANOTETTU_MAARA_AIKANA, "Ei vastaanotettu määräaikana");
    fi.put(KESKEN, "Kesken");
    fi.put(ValintatuloksenTila.PERUNUT, "Perunut");
    fi.put(PERUUTETTU, "Peruutettu");
    fi.put(EHDOLLISESTI_VASTAANOTTANUT, "Ehdollisesti vastaanottanut");

    Map<ValintatuloksenTila, String> sv = new HashMap<>(fi);
    Map<ValintatuloksenTila, String> en = new HashMap<>(fi);

    kielet.put(KieliUtil.SUOMI, Collections.unmodifiableMap(fi));
    kielet.put(KieliUtil.RUOTSI, Collections.unmodifiableMap(sv));
    kielet.put(KieliUtil.ENGLANTI, Collections.unmodifiableMap(en));
    return Collections.unmodifiableMap(kielet);
  }

  private static Map<String, Map<IlmoittautumisTila, String>> ilmoittautumisTilat() {
    Map<String, Map<IlmoittautumisTila, String>> kielet =
        new HashMap<String, Map<IlmoittautumisTila, String>>();

    Map<IlmoittautumisTila, String> fi = new HashMap<IlmoittautumisTila, String>();
    fi.put(IlmoittautumisTila.EI_TEHTY, "Ei tehty");
    fi.put(IlmoittautumisTila.LASNA_KOKO_LUKUVUOSI, "Läsnä (koko lukuvuosi)");
    fi.put(IlmoittautumisTila.POISSA_KOKO_LUKUVUOSI, "Poissa koko lukuvuosi");
    fi.put(IlmoittautumisTila.EI_ILMOITTAUTUNUT, "Ei ilmoittautunut");
    fi.put(IlmoittautumisTila.LASNA_SYKSY, "Läsnä syksy, poissa kevät");
    fi.put(IlmoittautumisTila.POISSA_SYKSY, "Poissa syksy, läsnä kevät");
    fi.put(IlmoittautumisTila.LASNA, "Läsnä keväällä alkava koulutus");
    fi.put(IlmoittautumisTila.POISSA, "Poissa keväällä alkava koulutus");

    Map<IlmoittautumisTila, String> sv = new HashMap<>(fi);
    Map<IlmoittautumisTila, String> en = new HashMap<>(fi);

    kielet.put(KieliUtil.SUOMI, Collections.unmodifiableMap(fi));
    kielet.put(KieliUtil.RUOTSI, Collections.unmodifiableMap(sv));
    kielet.put(KieliUtil.ENGLANTI, Collections.unmodifiableMap(en));
    return Collections.unmodifiableMap(kielet);
  }

  private static Map<String, Map<HakemuksenTila, String>> valmistaTilat(final boolean forLetter) {
    Map<String, Map<HakemuksenTila, String>> kielet =
        new HashMap<String, Map<HakemuksenTila, String>>();
    Map<HakemuksenTila, String> fi = new HashMap<>();
    fi.put(HYLATTY, forLetter ? "Et saanut opiskelupaikkaa" : "Hylätty");
    fi.put(VARALLA, "Varalla");
    fi.put(VARASIJALTA_HYVAKSYTTY, "Varasijalta hyväksytty");
    fi.put(PERUUNTUNUT, "Peruuntunut");
    fi.put(HYVAKSYTTY, "Hyväksytty");
    fi.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Harkinnanvaraisesti hyväksytty");
    fi.put(HakemuksenTila.PERUUTETTU, "Peruutettu");
    fi.put(PERUNUT, "Perunut");

    Map<HakemuksenTila, String> sv = new HashMap<>();
    sv.put(HYLATTY, forLetter ? "Du fick inte studieplats" : "Underkänd");
    sv.put(VARALLA, "På reservplats");
    sv.put(VARASIJALTA_HYVAKSYTTY, "Godkänd från reservplats");
    sv.put(PERUUNTUNUT, "Annullerad");
    sv.put(HYVAKSYTTY, "Godkänd");
    sv.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Godkänd enligt prövning");
    sv.put(HakemuksenTila.PERUUTETTU, "Annullerad");
    sv.put(PERUNUT, "Annullerad");

    Map<HakemuksenTila, String> en = new HashMap<HakemuksenTila, String>();
    en.put(HYLATTY, forLetter ? "You were not offered admission" : "Rejected");
    en.put(VARALLA, "On a waiting list");
    en.put(PERUUNTUNUT, "Cancelled");
    en.put(HYVAKSYTTY, "Accepted");
    en.put(VARASIJALTA_HYVAKSYTTY, "Accepted from a waiting list");
    en.put(HARKINNANVARAISESTI_HYVAKSYTTY, "Accepted");
    en.put(HakemuksenTila.PERUUTETTU, "Cancelled");
    en.put(PERUNUT, "Cancelled");

    kielet.put(KieliUtil.SUOMI, Collections.unmodifiableMap(fi));
    kielet.put(KieliUtil.RUOTSI, Collections.unmodifiableMap(sv));
    kielet.put(KieliUtil.ENGLANTI, Collections.unmodifiableMap(en));
    return Collections.unmodifiableMap(kielet);
  }

  public static String varasijallaConverter(Integer numero, String preferoitukielikoodi) {
    switch (preferoitukielikoodi) {
      case KieliUtil.SUOMI:
        return numero.toString() + ". " + VARASIJALLA.get(KieliUtil.SUOMI);
      case KieliUtil.RUOTSI:
        return VARASIJALLA.get(KieliUtil.RUOTSI) + " " + numero.toString();
      case KieliUtil.ENGLANTI:
        switch (numero % 10) {
          case 1:
            return numero.toString() + "st " + VARASIJALLA.get(KieliUtil.ENGLANTI);
          case 2:
            return numero.toString() + "nd " + VARASIJALLA.get(KieliUtil.ENGLANTI);
          case 3:
            return numero.toString() + "rd " + VARASIJALLA.get(KieliUtil.ENGLANTI);
          default:
            return numero.toString() + "th " + VARASIJALLA.get(KieliUtil.ENGLANTI);
        }
      default:
        throw new IllegalArgumentException("Tuntematon kielikoodi " + preferoitukielikoodi);
    }
  }

  public static String varasijanNumeroConverter(Integer numero, String preferoitukielikoodi) {
    return VARASIJAT.get(preferoitukielikoodi) + numero;
  }

  public static String tilaConverter(
      HakutoiveenValintatapajonoDTO valintatapajono, String preferoitukielikoodi) {
    if (valintatapajono.getTila() == HakemuksenTila.VARALLA) {
      return HakemusUtil.varasijallaConverter(
          valintatapajono.getVarasijanNumero(), preferoitukielikoodi);
    } else {
      return HakemusUtil.TILAT_FOR_LETTER.get(preferoitukielikoodi).get(valintatapajono.getTila());
    }
  }

  public static String tilaConverter(IlmoittautumisTila tila, String preferoitukielikoodi) {
    if (tila == null) {
      return StringUtils.EMPTY;
    }
    try {
      return ILMOITTAUTUMISTILAT.get(preferoitukielikoodi).get(tila);
    } catch (Exception e) {
      LOG.error("Hakemuksen tila utiliteetilla ei ole konversiota ilmoittautumistilalle: {}", tila);
      return tila.toString();
    }
  }

  public static String lupaJulkaisuun(boolean lupa) {
    if (lupa) {
      return "Lupa julkaisuun";
    }
    return StringUtils.EMPTY;
  }

  public static String lupaSahkoiseenAsiointiin(boolean lupa) {
    if (lupa) {
      return "Lupa sähköiseen asiointiin";
    }
    return StringUtils.EMPTY;
  }

  public static String tilaConverter(ValintatuloksenTila tila, String preferoitukielikoodi) {
    if (tila == null) {
      return StringUtils.EMPTY;
    }
    try {
      return VALINTATULOKSEN_TILAT.get(preferoitukielikoodi).get(tila);
    } catch (Exception e) {
      LOG.error("Hakemuksen tila utiliteetilla ei ole konversiota ilmoittautumistilalle: {}", tila);
      return tila.toString();
    }
  }

  public static String tilaConverter(
      HakemuksenTila tila,
      String preferoitukielikoodi,
      boolean harkinnanvarainen,
      boolean ehdollinen,
      boolean lisaaVarasijanNumero,
      Integer varasijanNumero,
      String ehdollisenHyvaksymisenEhto) {
    if (tila == null) {
      return StringUtils.EMPTY;
    }
    HakemuksenTila lopullinenTila = tila;
    if (HYVAKSYTTY.equals(tila) && harkinnanvarainen) {
      lopullinenTila = HARKINNANVARAISESTI_HYVAKSYTTY;
    }
    try {
      String baseTila = TILAT.get(preferoitukielikoodi).get(lopullinenTila);
      if (lisaaVarasijanNumero && VARALLA.equals(lopullinenTila) && varasijanNumero != null) {
        return new StringBuilder()
            .append(baseTila)
            .append(" (")
            .append(varasijanNumero)
            .append(")")
            .toString();
      }
      if (lopullinenTila.isHyvaksytty() && ehdollinen) {
        if (!ehdollisenHyvaksymisenEhto.equals("")) {
          return baseTila + " (" + ehdollisenHyvaksymisenEhto + ")";
        }
        return baseTila + EHDOLLINEN.get(preferoitukielikoodi);
      }
      return baseTila;
    } catch (Exception e) {
      LOG.error("Hakemuksen tila utiliteetilla ei ole konversiota hakemustilalle: {}", tila);
      return tila.toString();
    }
  }

  public static String ehdollinenValinta(boolean ehdollisestiHyvaksytty) {
    return ehdollisestiHyvaksytty ? "Kyllä" : "Ei";
  }
}
