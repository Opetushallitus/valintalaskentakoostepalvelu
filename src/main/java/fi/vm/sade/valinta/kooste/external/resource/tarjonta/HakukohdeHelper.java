package fi.vm.sade.valinta.kooste.external.resource.tarjonta;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import java.util.Collections;
import java.util.Optional;
import org.slf4j.LoggerFactory;

public class HakukohdeHelper {
  public static String tarjoajaOid(HakukohdeV1RDTO hakukohde) {
    if (Optional.ofNullable(hakukohde.getTarjoajaOids()).orElse(Collections.emptySet()).isEmpty()) {
      LoggerFactory.getLogger(HakukohdeHelper.class)
          .warn("Hakukohteella " + hakukohde.getOid() + " ei yhtään tarjoaja oidia");
      return null;
    }
    return hakukohde.getTarjoajaOids().iterator().next();
  }
}
