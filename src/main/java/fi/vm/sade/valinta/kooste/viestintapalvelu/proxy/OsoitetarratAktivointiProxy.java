package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface OsoitetarratAktivointiProxy {

    String osoitetarratAktivointi(String hakukohdeOid, List<String> valintakoeOid);
}
