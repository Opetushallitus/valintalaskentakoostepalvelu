package fi.vm.sade.valinta.kooste.dokumentit;

import java.util.List;

public class ApiVirhe {
  public final String viesti;
  public final List<String> keys;

  public ApiVirhe(final String viesti, final List<String> keys) {
    this.viesti = viesti;
    this.keys = keys;
  }
}
