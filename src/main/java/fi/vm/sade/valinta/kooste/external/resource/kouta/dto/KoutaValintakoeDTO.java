package fi.vm.sade.valinta.kooste.external.resource.kouta.dto;

import java.math.BigDecimal;

public class KoutaValintakoeDTO {
  private String oid;
  private String tyyppiUri;
  private BigDecimal vahimmaispisteet;

  public String getOid() {
    return oid;
  }

  public void setOid(String oid) {
    this.oid = oid;
  }

  public String getTyyppiUri() {
    return tyyppiUri;
  }

  public void setTyyppiUri(String tyyppiUri) {
    this.tyyppiUri = tyyppiUri;
  }

  public BigDecimal getVahimmaispisteet() {
    return vahimmaispisteet;
  }

  public void setVahimmaispisteet(BigDecimal vahimmaispisteet) {
    this.vahimmaispisteet = vahimmaispisteet;
  }
}
