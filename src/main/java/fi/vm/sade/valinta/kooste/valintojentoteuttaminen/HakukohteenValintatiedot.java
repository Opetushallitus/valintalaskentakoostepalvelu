package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import java.util.Objects;

/** Esittää eri palveluista koostettuja hakukohteen valintatietoja. */
public class HakukohteenValintatiedot {
  public String hakukohdeOid;
  public Boolean hasValintakoe;
  // laskettu osittain tai kokonaan
  public Boolean laskettu;
  public Boolean sijoittelematta;
  // kokonaan tai osittain julkaisematta
  public Boolean julkaisematta;

  public HakukohteenValintatiedot(
      String hakukohdeOid,
      Boolean hasValintakoe,
      Boolean laskettu,
      Boolean sijoittelematta,
      Boolean julkaisematta) {
    this.hakukohdeOid = hakukohdeOid;
    this.hasValintakoe = hasValintakoe;
    this.laskettu = laskettu;
    this.sijoittelematta = sijoittelematta;
    this.julkaisematta = julkaisematta;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HakukohteenValintatiedot that = (HakukohteenValintatiedot) o;
    return Objects.equals(hakukohdeOid, that.hakukohdeOid)
        && Objects.equals(hasValintakoe, that.hasValintakoe)
        && Objects.equals(laskettu, that.laskettu)
        && Objects.equals(sijoittelematta, that.sijoittelematta)
        && Objects.equals(julkaisematta, that.julkaisematta);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hakukohdeOid, hasValintakoe, laskettu, sijoittelematta, julkaisematta);
  }
}
