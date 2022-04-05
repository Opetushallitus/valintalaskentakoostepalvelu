package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.ValintakoeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaHakukohde;

import java.math.BigDecimal;

public class Valintakoe {
  public final String id;
  public final String valintakokeentyyppiUri;
  public final BigDecimal vahimmaispisteet;

  public Valintakoe(ValintakoeV1RDTO dto) {
    this.id = dto.getOid();
    this.valintakokeentyyppiUri = dto.getValintakoetyyppi();
    this.vahimmaispisteet = dto.getPisterajat()
  }

  public Valintakoe(KoutaHakukohde.KoutaValintakoe dto) {
    this.id = dto.id;
    this.valintakokeentyyppiUri = dto.tyyppi;
    this.vahimmaispisteet = dto.vahimmaispisteet;
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
