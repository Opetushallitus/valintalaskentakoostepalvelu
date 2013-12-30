package fi.vm.sade.valinta.kooste.valintatieto.route;

import org.apache.camel.Property;

import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;
import fi.vm.sade.valinta.kooste.OPH;

public interface ValintatietoRoute {

    HakuTyyppi haeValintatiedot(@Property(OPH.HAKUOID) String hakuOid);
}
