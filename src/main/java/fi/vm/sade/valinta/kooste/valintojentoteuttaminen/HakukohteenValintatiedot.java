package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import java.util.Date;

/** Esittää eri palveluista koostettuja hakukohteen valintatietoja. */
public class HakukohteenValintatiedot {
  public String hakukohdeOid;
  public Boolean hasValintakoe;
  public Date varasijatayttoPaattyy;

  public HakukohteenValintatiedot(
      String hakukohdeOid, Boolean hasValintakoe, Date varasijatayttoPaattyy) {
    this.hakukohdeOid = hakukohdeOid;
    this.hasValintakoe = hasValintakoe;
    this.varasijatayttoPaattyy = varasijatayttoPaattyy;
  }
}
