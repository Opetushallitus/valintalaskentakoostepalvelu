package fi.vm.sade.valinta.kooste.tarjonta.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

public interface LinjakoodiRoute {
    String haeLinjakoodi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid);
}
