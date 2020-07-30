package fi.vm.sade.valinta.kooste.sijoittelu.dto;

import java.util.concurrent.atomic.AtomicInteger;

public class Sijoittelu {
  private String hakuOid;
  private AtomicInteger tila;

  public Sijoittelu(String hakuOid) {
    this.hakuOid = hakuOid;
    this.tila = new AtomicInteger(0);
  }

  public void setValmis() {
    if (!tila.compareAndSet(0, 1)) {
      throw new RuntimeException(
          "Sijoittelu tyon tila oli jo asetettu! Tilaa yritettiin asettaa valmiiksi.");
    }
  }

  public void setOhitettu() {
    if (!tila.compareAndSet(0, -1)) {
      throw new RuntimeException(
          "Sijoittelu tyon tila oli jo asetettu! Tilaa yritettiin asettaa ohitetuksi.");
    }
  }

  public boolean isValmis() {
    return tila.get() == 1;
  }

  public boolean isTekeillaan() {
    return tila.get() == 0;
  }

  public boolean isOhitettu() {
    return tila.get() == -1;
  }

  public String getHakuOid() {
    return hakuOid;
  }
}
