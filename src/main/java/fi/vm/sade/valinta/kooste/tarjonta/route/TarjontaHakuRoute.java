package fi.vm.sade.valinta.kooste.tarjonta.route;

import org.apache.camel.Property;

import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface TarjontaHakuRoute {

    HakuDTO haeHaku(@Property(OPH.HAKUOID) String hakuOid);
}
