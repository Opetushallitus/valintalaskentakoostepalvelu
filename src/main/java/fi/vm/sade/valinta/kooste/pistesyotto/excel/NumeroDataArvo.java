package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;

public class NumeroDataArvo extends TilaDataArvo {
  private final double min;
  private final double max;
  private final String tunniste;
  private final String osallistuminenTunniste;

  public NumeroDataArvo(double min, double max, Map<String, String> tilaKonvertteri, String tunniste,
      String asetettuTila, String osallistuminenTunniste) {
    super(tilaKonvertteri, asetettuTila);
    this.min = min;
    this.max = max;
    this.tunniste = tunniste;
    this.osallistuminenTunniste = osallistuminenTunniste;
  }

  protected boolean isValidi(String arvo) {
    return StringUtils.isBlank(arvo) || tarkistaRajat(arvo);
  }

  private boolean tarkistaRajat(String arvo) {
    try {
      double d = Double.parseDouble(arvo);
      return min <= d && max >= d;
    } catch (Exception e) {

    }
    return false;
  }

  private boolean isAsetettu(String arvo) {
    return tarkistaRajat(arvo);
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
    return arvo;
  }

  public Number getMax() {
    return max;
  }

  public Number getMin() {
    return min;
  }
}
