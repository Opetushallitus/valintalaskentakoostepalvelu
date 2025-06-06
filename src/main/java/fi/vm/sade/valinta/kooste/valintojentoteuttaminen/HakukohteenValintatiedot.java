package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import java.util.Date;
import java.util.Objects;

/** Esittää eri palveluista koostettuja hakukohteen valintatietoja. */
public class HakukohteenValintatiedot {
  public String hakukohdeOid;
  public Boolean hasValintakoe;
  // laskettu osittain tai kokonaan
  public Boolean laskettu;
  public Date varasijatayttoPaattyy;

  public HakukohteenValintatiedot(
      String hakukohdeOid, Boolean hasValintakoe, Boolean laskettu, Date varasijatayttoPaattyy) {
    this.hakukohdeOid = hakukohdeOid;
    this.hasValintakoe = hasValintakoe;
    this.laskettu = laskettu;
    this.varasijatayttoPaattyy = varasijatayttoPaattyy;
  }

  public HakukohteenValintatiedot(String hakukohdeOid) {
    this.hakukohdeOid = hakukohdeOid;
    this.hasValintakoe = false;
    this.laskettu = false;
    this.varasijatayttoPaattyy = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HakukohteenValintatiedot that = (HakukohteenValintatiedot) o;
    return Objects.equals(hakukohdeOid, that.hakukohdeOid)
        && Objects.equals(hasValintakoe, that.hasValintakoe)
        && Objects.equals(laskettu, that.laskettu)
        && Objects.equals(varasijatayttoPaattyy, that.varasijatayttoPaattyy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hakukohdeOid, hasValintakoe, laskettu, varasijatayttoPaattyy);
  }
}
