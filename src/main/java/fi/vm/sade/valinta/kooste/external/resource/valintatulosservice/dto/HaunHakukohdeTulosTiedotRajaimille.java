package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import java.util.Set;

public class HaunHakukohdeTulosTiedotRajaimille {

  public String oid;

  public Set<TulosTiedotHakukohdeRajaimille> hakukohteet;

  public HaunHakukohdeTulosTiedotRajaimille(
      String oid, Set<TulosTiedotHakukohdeRajaimille> hakukohteet) {
    this.oid = oid;
    this.hakukohteet = hakukohteet;
  }
}
