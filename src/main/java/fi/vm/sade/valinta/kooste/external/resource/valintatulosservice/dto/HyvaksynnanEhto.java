package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

public class HyvaksynnanEhto {

  public String koodi = "";
  public String fi = "";
  public String sv = "";
  public String en = "";

  public HyvaksynnanEhto() {}

  public HyvaksynnanEhto(String koodi, String fi, String sv, String en) {
    this.koodi = koodi;
    this.fi = fi;
    this.sv = sv;
    this.en = en;
  }
}
