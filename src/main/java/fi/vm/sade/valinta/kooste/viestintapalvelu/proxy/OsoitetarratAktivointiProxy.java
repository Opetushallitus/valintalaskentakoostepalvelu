package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import java.util.List;

import org.apache.camel.Property;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface OsoitetarratAktivointiProxy {

    String osoitetarratAktivointi(@Property("hakukohdeOid") String hakukohdeOid,
            @Property("valintakoeOid") List<String> valintakoeOid);
}
