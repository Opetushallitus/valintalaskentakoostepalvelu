package fi.vm.sade.valinta.kooste.tarjonta.route;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         10 retries!
 */
public interface TarjontaNimiRoute {

    HakukohdeNimiRDTO haeHakukohdeNimi(String hakukohdeOid);
}
