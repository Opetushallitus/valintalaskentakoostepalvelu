package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import com.google.common.collect.Sets;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;

public class DiskreettiDataArvo extends TilaDataArvo {
  private final Set<String> arvot;
  private final Map<Double, String> konversio;
  private final String tunniste;
  private final String osallistuminenTunniste;

  public DiskreettiDataArvo(Collection<String> arvot, Map<String, String> tilaKonvertteri, String tunniste,
      String asetettuTila, String osallistuminenTunniste) {
    super(tilaKonvertteri, asetettuTila);
    this.arvot = Sets.newHashSet(arvot);
    this.konversio = arvot.stream().filter(a -> {
      try {
        Double.parseDouble(a);
        return true;
      } catch (NumberFormatException e) {
        return false;
      }
    }).collect(Collectors.toMap(a -> Double.parseDouble(a), a -> a));
    this.tunniste = tunniste;
    this.osallistuminenTunniste = osallistuminenTunniste;
  }

  protected boolean isValidi(String arvo) {
    return StringUtils.isBlank(arvo) || tarkistaArvo(arvo);
  }

  private boolean tarkistaArvo(String arvo) {
    if (arvot.contains(arvo)) {
      return true;
    } else {
      try {
        double d = Double.parseDouble(arvo);
        return konversio.containsKey(d);
      } catch (Exception e) {
        return false;
      }
    }
  }

  private boolean isAsetettu(String arvo) {
    return tarkistaArvo(arvo);
  }

  public PistesyottoArvo asPistesyottoArvo(String raakaarvo, String tila) {
    final String arvo = Optional.ofNullable(raakaarvo).orElse("").replaceAll(",", ".");
    String lopullinenTila;
    if (isAsetettu(arvo)) {
      lopullinenTila = getAsetettuTila();
    } else {
      lopullinenTila = konvertoiTila(tila);
      if (getAsetettuTila().equals(lopullinenTila)) {
        if (!(lopullinenTila.equals(PistesyottoExcel.VAKIO_OSALLISTUI) && !isAsetettu(arvo))) {
          lopullinenTila = PistesyottoExcel.VAKIO_MERKITSEMATTA;
        }
      }
    }
    return new PistesyottoArvo(konvertoi(arvo), lopullinenTila, isValidi(arvo) && isValidiTila(tila), tunniste,
        osallistuminenTunniste);
  }

  private String konvertoi(String arvo) {
    try {
      double d = Double.parseDouble(arvo);
      if (konversio.containsKey(d)) {
        return konversio.get(d);
      }
    } catch (Exception e) {
    }
    return arvo;
  }
}
