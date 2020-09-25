package fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto;

import java.util.Map;

public class KoutaHaku {
  public final String oid;
  public final Map<String, String> nimi;
  public final String organisaatioOid;
  public final String kohdejoukkoKoodiUri;
  public final String hakulomakeAtaruId;

  private KoutaHaku() {
    this.oid = null;
    this.nimi = null;
    this.organisaatioOid = null;
    this.kohdejoukkoKoodiUri = null;
    this.hakulomakeAtaruId = null;
  }
}
