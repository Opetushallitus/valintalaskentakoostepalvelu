package fi.vm.sade.valinta.kooste.proxy.resource.erillishaku.util;

import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public class PseudoSatunnainenOID {
    public static String trimToNull(String valintatapajonoOid) {
        if ("null".equals(valintatapajonoOid) || StringUtils.trimToNull(valintatapajonoOid) == null) {
            return null;
        } else {
            return valintatapajonoOid;
        }
    }

    public static String oidHaustaJaHakukohteesta(String hakuOID, String hakukohdeOID) {
        String hakukohdeOIDIlmanPisteita = Optional.ofNullable(hakukohdeOID).orElse("").replace(".", "");
        String hakuOIDReversedIlmanPisteita = new StringBuilder(Optional.ofNullable(hakuOID).orElse("")).reverse().toString().replace(".", "");
        return StringUtils.substring(hakukohdeOIDIlmanPisteita + hakuOIDReversedIlmanPisteita, 0, 32);
    }
}
