package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import java.util.Set;

public class HaunHakukohdeTulosTiedot {

  public String oid;

  public Set<HakukohdeTulosTiedot> hakukohteet;

  public HaunHakukohdeTulosTiedot(
      String oid, Set<HakukohdeTulosTiedot> hakukohteet) {
    this.oid = oid;
    this.hakukohteet = hakukohteet;
  }
}
