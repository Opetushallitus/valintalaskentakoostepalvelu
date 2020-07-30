package fi.vm.sade.valinta.kooste.tarjonta.route;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.OPH;
import org.apache.camel.Property;

public interface TarjontaNimiRoute {
  final String DIRECT_TARJONTA_NIMI = "direct:tarjontaNimiReitti";

  HakukohdeNimiRDTO haeHakukohdeNimi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid);
}
