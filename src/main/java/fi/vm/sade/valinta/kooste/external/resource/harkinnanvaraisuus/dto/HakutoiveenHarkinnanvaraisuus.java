package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto;

import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuudenSyy;

public class HakutoiveenHarkinnanvaraisuus {
  private String hakukohdeOid;
  private HarkinnanvaraisuudenSyy harkinnanvaraisuudenSyy;

  public HakutoiveenHarkinnanvaraisuus(
      String hakukohdeOid, HarkinnanvaraisuudenSyy harkinnanvaraisuudenSyy) {
    this.hakukohdeOid = hakukohdeOid;
    this.harkinnanvaraisuudenSyy = harkinnanvaraisuudenSyy;
  }

  public String getHakukohdeOid() {
    return hakukohdeOid;
  }

  public void setHakukohdeOid(String hakukohdeOid) {
    this.hakukohdeOid = hakukohdeOid;
  }

  public HarkinnanvaraisuudenSyy getHarkinnanvaraisuudenSyy() {
    return harkinnanvaraisuudenSyy;
  }

  public void setHarkinnanvaraisuudenSyy(HarkinnanvaraisuudenSyy harkinnanvaraisuudenSyy) {
    this.harkinnanvaraisuudenSyy = harkinnanvaraisuudenSyy;
  }

  public Boolean isHarkinnanvarainenHakutoive() {
    return HarkinnanvaraisuudenSyy.NONE.equals(harkinnanvaraisuudenSyy);
  }
}
