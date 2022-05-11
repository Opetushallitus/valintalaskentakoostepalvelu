package fi.vm.sade.valinta.kooste.external.resource.kouta.dto;

import java.util.List;

public class HakukohderyhmaHakukohde {
  public String hakukohdeOid;
  public List<String> hakukohderyhmat;

  public HakukohderyhmaHakukohde(String hakukohdeOid, List<String> hakukohderyhmat) {
    this.hakukohdeOid = hakukohdeOid;
    this.hakukohderyhmat = hakukohderyhmat;
  }

  public String getHakukohdeOid() {
    return hakukohdeOid;
  }

  public List<String> getHakukohderyhmat() {
    return hakukohderyhmat;
  }
}
