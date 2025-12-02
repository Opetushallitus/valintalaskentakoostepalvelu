package fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto;

import java.util.List;

public class ValinnantulosRequest {
  private List<Valinnantulos> valinnantulokset;

  public ValinnantulosRequest(List<Valinnantulos> valinnantulokset) {
    this.valinnantulokset = valinnantulokset;
  }

  public List<Valinnantulos> getValinnantulokset() {
    return valinnantulokset;
  }

  public void setValinnantulokset(List<Valinnantulos> valinnantulokset) {
    this.valinnantulokset = valinnantulokset;
  }
}
