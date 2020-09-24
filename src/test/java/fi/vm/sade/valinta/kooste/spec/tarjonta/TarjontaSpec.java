package fi.vm.sade.valinta.kooste.spec.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;

public class TarjontaSpec {

  public static class HakukohdeBuilder {
    private final HakukohdeV1RDTO hakukohde;

    public HakukohdeBuilder(String hakuOid) {
      hakukohde = new HakukohdeV1RDTO();
      hakukohde.setHakuOid(hakuOid);
    }

    public HakukohdeV1RDTO build() {
      return hakukohde;
    }
  }
}
