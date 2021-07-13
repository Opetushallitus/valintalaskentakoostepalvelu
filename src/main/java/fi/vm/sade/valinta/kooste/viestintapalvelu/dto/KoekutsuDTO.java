package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;

public class KoekutsuDTO {
  private final String letterBodyText;
  private final String tarjoajaOid;
  private final String tag;
  private final String hakukohdeOid;
  private final Haku haku;
  private final String templateName;

  public KoekutsuDTO() {
    this.letterBodyText = null;
    this.tarjoajaOid = null;
    this.tag = null;
    this.hakukohdeOid = null;
    this.haku = null;
    this.templateName = null;
  }

  public KoekutsuDTO(
      String letterBodyText,
      String tarjoajaOid,
      String tag,
      String hakukohdeOid,
      Haku haku,
      String templateName) {
    this.letterBodyText = letterBodyText;
    this.tarjoajaOid = tarjoajaOid;
    this.tag = tag;
    this.hakukohdeOid = hakukohdeOid;
    this.haku = haku;
    this.templateName = templateName;
  }

  public String getTemplateName() {
    return templateName;
  }

  public String getHakukohdeOid() {
    return hakukohdeOid;
  }

  public Haku getHaku() {
    return haku;
  }

  public String getLetterBodyText() {
    return letterBodyText;
  }

  public String getTag() {
    return tag;
  }

  public String getTarjoajaOid() {
    return tarjoajaOid;
  }
}
