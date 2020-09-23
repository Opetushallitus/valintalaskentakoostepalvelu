package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;

public class NimiJaOpetuskieli {
  private final HakukohdeV1RDTO nimi;
  private final String opetuskieli;

  public NimiJaOpetuskieli(HakukohdeV1RDTO nimi, String opetuskieli) {
    this.nimi = nimi;
    this.opetuskieli = opetuskieli;
  }

  public String getOpetuskieli() {
    return opetuskieli;
  }

  public HakukohdeV1RDTO getNimi() {
    return nimi;
  }

  public Teksti getHakukohdeNimi() {
    return new Teksti(nimi.getHakukohteenNimet());
  }

  public Teksti getTarjoajaNimi() {
    return new Teksti(nimi.getTarjoajaNimet());
  }
}
