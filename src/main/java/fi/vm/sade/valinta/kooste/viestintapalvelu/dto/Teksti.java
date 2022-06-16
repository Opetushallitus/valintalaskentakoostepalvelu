package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import static fi.vm.sade.valinta.kooste.util.KieliUtil.ENGLANTI;
import static fi.vm.sade.valinta.kooste.util.KieliUtil.RUOTSI;
import static fi.vm.sade.valinta.kooste.util.KieliUtil.SUOMI;
import static fi.vm.sade.valinta.kooste.util.KieliUtil.normalisoiKielikoodi;

import com.google.common.collect.Maps;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

public class Teksti {
  private static final String EI_ARVOA = "<< ei arvoa >>";
  private final TreeMap<String, String> normalisoituKieliJaKoodi;

  public Teksti() {
    this.normalisoituKieliJaKoodi = Maps.newTreeMap();
  }

  public Teksti(Map<String, String> kieletJaKoodit) {
    this.normalisoituKieliJaKoodi = Maps.newTreeMap();
    if (kieletJaKoodit != null && !kieletJaKoodit.isEmpty()) {
      for (Entry<String, String> kk : kieletJaKoodit.entrySet()) {
        if (!StringUtils.isBlank(kk.getKey()) && !StringUtils.isBlank(kk.getValue())) {
          normalisoituKieliJaKoodi.put(normalisoiKielikoodi(kk.getKey()), kk.getValue());
        }
      }
    }
  }

  public boolean isArvoton() {
    return normalisoituKieliJaKoodi.isEmpty();
  }

  public Teksti(String suomenkielinenTeksti) {
    this(asMap(SUOMI, suomenkielinenTeksti));
  }

  public String getKieli() {
    if (normalisoituKieliJaKoodi.containsKey(SUOMI)) {
      return SUOMI;
    } else if (normalisoituKieliJaKoodi.containsKey(RUOTSI)) {
      return RUOTSI;
    } else {
      return ENGLANTI;
    }
  }

  public String getNonEmptyKieli() {
    if (normalisoituKieliJaKoodi.containsKey(SUOMI) && !StringUtils.isEmpty(normalisoituKieliJaKoodi.get(SUOMI))) {
      return SUOMI;
    } else if (normalisoituKieliJaKoodi.containsKey(RUOTSI)
        && !StringUtils.isEmpty(normalisoituKieliJaKoodi.get(RUOTSI))) {
      return RUOTSI;
    } else {
      return ENGLANTI;
    }
  }

  public String getTeksti(String normalisoituKielikoodi, String oletusarvo) {
    if (isArvoton()) {
      return oletusarvo;
    }
    if (normalisoituKieliJaKoodi.containsKey(normalisoituKielikoodi)) {
      return normalisoituKieliJaKoodi.get(normalisoituKielikoodi);
    }
    return getTeksti();
  }

  public String getTeksti(String normalisoituKielikoodi) {
    return getTeksti(normalisoituKielikoodi, EI_ARVOA);
  }

  public String getTeksti() {
    if (normalisoituKieliJaKoodi.isEmpty()) {
      return StringUtils.EMPTY;
    }
    if (normalisoituKieliJaKoodi.containsKey(SUOMI)) {
      return normalisoituKieliJaKoodi.get(SUOMI);
    } else if (normalisoituKieliJaKoodi.containsKey(RUOTSI)) {
      return normalisoituKieliJaKoodi.get(RUOTSI);
    } else {
      return normalisoituKieliJaKoodi.firstEntry().getValue();
    }
  }

  private static TreeMap<String, String> asMap(String key, String value) {
    TreeMap<String, String> m = Maps.newTreeMap();
    m.put(key, value);
    return m;
  }

  public static String getTeksti(final Map<String, String> n) {
    return new Teksti(n).getTeksti();
  }

  public static String getTeksti(final Map<String, String> n, String normalisoituKielikoodi) {
    return new Teksti(n).getTeksti(normalisoituKielikoodi);
  }

  public static String getTeksti(List<Map<String, String>> ns, String sep) {
    return ns.stream().map(Teksti::getTeksti).collect(Collectors.joining(sep));
  }

  public static String getTeksti(List<Map<String, String>> ns, String sep, String normalisoituKielikoodi) {
    return ns.stream().map(n -> getTeksti(n, normalisoituKielikoodi)).collect(Collectors.joining(sep));
  }

  public static Teksti ehdollisenHyvaksymisenEhto(HakutoiveenValintatapajonoDTO valintatapajono) {
    Map<String, String> m = new HashMap<>();
    if (valintatapajono.getTila().isHyvaksytty() && valintatapajono.isEhdollisestiHyvaksyttavissa()) {
      m.put(SUOMI, valintatapajono.getEhdollisenHyvaksymisenEhtoFI());
      m.put(RUOTSI, valintatapajono.getEhdollisenHyvaksymisenEhtoSV());
      m.put(ENGLANTI, valintatapajono.getEhdollisenHyvaksymisenEhtoEN());
    }
    return new Teksti(m);
  }
}
