package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.ValintakoeV1RDTO;

public class Valintakoe {
  public final String oid;
  public final String valintakokeentyyppiUri;

  public Valintakoe(String oid, String valintakokeentyyppiUri) {
    this.oid = oid;
    this.valintakokeentyyppiUri = valintakokeentyyppiUri;
  }

  public Valintakoe(ValintakoeV1RDTO dto) {
    this.oid = dto.getOid();
    this.valintakokeentyyppiUri = dto.getValintakoetyyppi();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Valintakoe that = (Valintakoe) o;

    return oid.equals(that.oid);
  }

  @Override
  public int hashCode() {
    return oid.hashCode();
  }
}
