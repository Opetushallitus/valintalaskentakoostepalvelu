package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;

@JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
public class HakemuksenHarkinnanvaraisuus {

  private String hakemusOid;
  private String henkiloOid;
  private List<HakutoiveenHarkinnanvaraisuus> hakutoiveet;

  public HakemuksenHarkinnanvaraisuus() {}

  public HakemuksenHarkinnanvaraisuus(
      String hakemusOid, List<HakutoiveenHarkinnanvaraisuus> hakutoiveet) {
    this.hakemusOid = hakemusOid;
    this.hakutoiveet = hakutoiveet;
  }

  public String getHakemusOid() {
    return hakemusOid;
  }

  public void setHakemusOid(String hakemusOid) {
    this.hakemusOid = hakemusOid;
  }

  public String getHenkiloOid() {
    return henkiloOid;
  }

  public void setHenkiloOid(String henkiloOid) {
    this.henkiloOid = henkiloOid;
  }

  public List<HakutoiveenHarkinnanvaraisuus> getHakutoiveet() {
    return hakutoiveet;
  }

  public void setHakutoiveet(List<HakutoiveenHarkinnanvaraisuus> hakutoiveet) {
    this.hakutoiveet = hakutoiveet;
  }
}