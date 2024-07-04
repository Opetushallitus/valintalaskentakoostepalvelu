package fi.vm.sade.valinta.kooste.dokumentit.dto;

import java.util.List;

public class DokumenttiDto {
  private final String uuid;
  private final String kuvaus;
  private final boolean valmis;
  private final List<VirheilmoitusDto> virheilmoitukset;

  public DokumenttiDto() {
    this.uuid = null;
    this.kuvaus = null;
    this.virheilmoitukset = null;
    this.valmis = false;
  }

  public DokumenttiDto(
      String uuid, String kuvaus, boolean valmis, List<VirheilmoitusDto> virheilmoitukset) {
    this.uuid = uuid;
    this.kuvaus = kuvaus;
    this.valmis = valmis;
    this.virheilmoitukset = virheilmoitukset;
  }

  public String getUuid() {
    return uuid;
  }

  public String getKuvaus() {
    return kuvaus;
  }

  public String getDokumenttiId() {
    return this.valmis ? "valmis" : null;
  }

  public boolean isValmis() {
    return this.valmis;
  }

  public boolean isVirheita() {
    return virheilmoitukset != null && !virheilmoitukset.isEmpty();
  }

  public List<VirheilmoitusDto> getVirheilmoitukset() {
    return virheilmoitukset;
  }
}
