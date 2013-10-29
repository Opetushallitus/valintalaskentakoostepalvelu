package fi.vm.sade.valinta.kooste.tarjonta;

import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface TarjontaHakuProxy {

    HakuDTO haeHaku(String hakuOid);
}
