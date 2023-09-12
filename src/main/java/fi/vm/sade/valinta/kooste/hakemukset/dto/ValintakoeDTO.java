package fi.vm.sade.valinta.kooste.hakemukset.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "valinta.kooste.hakemukset.dto.ValintakoeDTO", description = "Valintakoe")
public class ValintakoeDTO {

  private String tunniste;

  public ValintakoeDTO(String tunniste) {
    this.tunniste = tunniste;
  }

  public String getTunniste() {
    return tunniste;
  }

  public void setTunniste(String tunniste) {
    this.tunniste = tunniste;
  }

  @Override
  public String toString() {
    return "ValintakoeDTO{" + "tunniste='" + tunniste + '\'' + '}';
  }
}
