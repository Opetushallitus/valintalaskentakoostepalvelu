package fi.vm.sade.valinta.kooste.dto;

import java.util.Date;

public class HakukohdeKoosteTieto {
  public final String hakukohdeOid;
  public final Boolean hasValintakoe;
  public final Date varasijatayttoPaattyy;

  public HakukohdeKoosteTieto(
      String hakukohdeOid, Boolean hasValintakoe, Date varasijatayttoPaattyy) {
    this.hakukohdeOid = hakukohdeOid;
    this.hasValintakoe = hasValintakoe;
    this.varasijatayttoPaattyy = varasijatayttoPaattyy;
  }
}
