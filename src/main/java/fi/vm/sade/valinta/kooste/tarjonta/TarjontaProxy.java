package fi.vm.sade.valinta.kooste.tarjonta;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         10 retries!
 */
public interface TarjontaProxy {

    HakukohdeNimiRDTO haeHakukohdeNimi(String hakukohdeOid);
}
