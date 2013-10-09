package fi.vm.sade.valinta.kooste.haku;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         10 retries!
 */
public interface HakemusProxy {

    Hakemus haeHakemus(String hakemusOid);

}
