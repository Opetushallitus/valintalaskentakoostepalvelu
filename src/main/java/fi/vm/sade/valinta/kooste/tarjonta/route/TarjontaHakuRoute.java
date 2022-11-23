package fi.vm.sade.valinta.kooste.tarjonta.route;

import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;

public interface TarjontaHakuRoute {
  HakuDTO haeHaku(String hakuOid);
}
