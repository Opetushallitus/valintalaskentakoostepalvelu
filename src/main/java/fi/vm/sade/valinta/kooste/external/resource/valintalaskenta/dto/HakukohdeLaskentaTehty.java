package fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.dto;

import java.util.Date;

public class HakukohdeLaskentaTehty {

  public String hakukohdeOid;

  public Date lastModified;

  public HakukohdeLaskentaTehty(String hakukohdeOid, Date lastModified) {
    this.hakukohdeOid = hakukohdeOid;
    this.lastModified = lastModified;
  }
}
