package fi.vm.sade.valinta.kooste.valvomo.dto;

import java.util.Collection;

public abstract class Tyo {

  public double getProsentteina() {
    int kokonaismaara = getKokonaismaara();
    if (kokonaismaara == 0) {
      return 0d;
    }
    return ((double) getTehty()) / ((double) kokonaismaara);
  }

  public abstract Collection<Exception> getPoikkeukset();

  public abstract int getTehty();

  public abstract int getKokonaismaara();

  public abstract String getNimi();
}
