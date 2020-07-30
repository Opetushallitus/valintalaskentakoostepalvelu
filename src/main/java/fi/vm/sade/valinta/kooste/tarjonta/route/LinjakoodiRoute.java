package fi.vm.sade.valinta.kooste.tarjonta.route;

import fi.vm.sade.valinta.kooste.OPH;
import org.apache.camel.Property;

public interface LinjakoodiRoute {
  String haeLinjakoodi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid);
}
