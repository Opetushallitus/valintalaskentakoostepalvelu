package fi.vm.sade.valinta.kooste.tarjonta.route;

import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.valinta.kooste.OPH;
import org.apache.camel.Property;

public interface TarjontaHakuRoute {
  HakuDTO haeHaku(@Property(OPH.HAKUOID) String hakuOid);
}
