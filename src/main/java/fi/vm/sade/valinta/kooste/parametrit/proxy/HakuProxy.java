package fi.vm.sade.valinta.kooste.parametrit.proxy;

import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;

/**
 * User: tommiha
 * Date: 8/21/13
 * Time: 1:28 PM
 */
public interface HakuProxy {

    /**
     * Hae haku oidilla.
     * @param hakuOid
     * @return
     */
    HakuTyyppi getHakuByOid(String hakuOid);
}
