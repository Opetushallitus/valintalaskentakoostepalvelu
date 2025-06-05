package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

public class HakukohdeTulosTiedot {

  public String oid;

  public Boolean sijoittelematta;

  public Boolean julkaisematta;

  public HakukohdeTulosTiedot(
      String oid, Boolean sijoittelematta, Boolean julkaisematta) {
    this.oid = oid;
    this.sijoittelematta = sijoittelematta;
    this.julkaisematta = julkaisematta;
  }
}
