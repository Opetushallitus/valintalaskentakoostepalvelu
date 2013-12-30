package fi.vm.sade.valinta.kooste.tarjonta.route;

import org.apache.camel.Property;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         10 retries!
 */
public interface TarjontaNimiRoute {

    HakukohdeNimiRDTO haeHakukohdeNimi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid);
}
