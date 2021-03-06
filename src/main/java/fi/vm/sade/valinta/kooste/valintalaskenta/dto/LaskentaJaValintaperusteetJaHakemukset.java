package fi.vm.sade.valinta.kooste.valintalaskenta.dto;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import java.util.List;

public class LaskentaJaValintaperusteetJaHakemukset {
  private final LaskentaStartParams laskenta;
  private final List<ValintaperusteetDTO> valintaperusteet;
  private final List<Hakemus> hakemukset;
  private final List<ApplicationAdditionalDataDTO> lisatiedot;
  private final List<ValintaperusteetHakijaryhmaDTO> hakijaryhmat;
  private final String hakukohdeOid;

  public LaskentaJaValintaperusteetJaHakemukset(
      LaskentaStartParams laskenta,
      String hakukohdeOid,
      List<ValintaperusteetDTO> valintaperusteet,
      List<Hakemus> hakemukset,
      List<ApplicationAdditionalDataDTO> lisatiedot,
      List<ValintaperusteetHakijaryhmaDTO> hakijaryhmat) {
    this.laskenta = laskenta;
    this.hakukohdeOid = hakukohdeOid;
    this.valintaperusteet = valintaperusteet;
    this.hakemukset = hakemukset;
    this.lisatiedot = lisatiedot;
    this.hakijaryhmat = hakijaryhmat;
  }

  public List<ValintaperusteetHakijaryhmaDTO> getHakijaryhmat() {
    return hakijaryhmat;
  }

  public boolean isValmisLaskettavaksi() {
    return valintaperusteet != null && hakemukset != null;
  }

  public List<ApplicationAdditionalDataDTO> getLisatiedot() {
    return lisatiedot;
  }

  public List<Hakemus> getHakemukset() {
    return hakemukset;
  }

  public String getHakukohdeOid() {
    return hakukohdeOid;
  }

  public LaskentaStartParams getLaskenta() {
    return laskenta;
  }

  public List<ValintaperusteetDTO> getValintaperusteet() {
    return valintaperusteet;
  }
}
