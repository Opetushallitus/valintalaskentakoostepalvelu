package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.ValintakoeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaHakukohde;

public class Valintakoe {
  public final String id;
  public final String valintakokeentyyppiUri;

  public Valintakoe(ValintakoeV1RDTO dto) {
    this.id = dto.getOid();
    this.valintakokeentyyppiUri = dto.getValintakoetyyppi();
  }

  public Valintakoe(KoutaHakukohde.KoutaValintakoe dto) {
    this.id = dto.id;
    this.valintakokeentyyppiUri = dto.tyyppi;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Valintakoe that = (Valintakoe) o;

    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
