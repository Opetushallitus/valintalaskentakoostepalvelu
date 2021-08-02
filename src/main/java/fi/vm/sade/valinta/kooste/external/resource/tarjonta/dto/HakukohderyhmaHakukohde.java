package fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto;

import java.util.List;

public class HakukohderyhmaHakukohde {
  public String oid;
  public List<String> hakukohderyhmat;

  public HakukohderyhmaHakukohde(String oid, List<String> hakukohderyhmat) {
    this.oid = oid;
    this.hakukohderyhmat = hakukohderyhmat;
  }

  public String getOid() {
    return oid;
  }

  public List<String> getHakukohderyhmat() {
    return hakukohderyhmat;
  }
}
