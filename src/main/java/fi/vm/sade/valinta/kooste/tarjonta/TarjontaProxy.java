package fi.vm.sade.valinta.kooste.tarjonta;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         10 retries!
 */
public interface TarjontaProxy {

    HakukohdeDTO getByOID(String hakukohdeOid);
}
