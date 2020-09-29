package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KomoV1RDTO;

public class Koulutus {
  public final String oid;

  public Koulutus(String oid) {
    this.oid = oid;
  }

  public Koulutus(KomoV1RDTO dto) {
    this.oid = dto.getOid();
  }
}
