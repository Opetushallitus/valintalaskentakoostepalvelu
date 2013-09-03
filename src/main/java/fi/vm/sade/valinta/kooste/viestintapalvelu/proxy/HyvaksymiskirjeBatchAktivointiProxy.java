package fi.vm.sade.valinta.kooste.viestintapalvelu.proxy;

import java.util.List;

import org.apache.camel.Property;
import org.apache.camel.language.Simple;

/**
 * 
 * @author Jussi Jartamo
 * 
 */

public interface HyvaksymiskirjeBatchAktivointiProxy {

    // halutaanko riippuvuus viestintapalveluun? tassa voisi kayttaa oikeaa
    // hyvaksymiskirjebatch-tyyppia merkkijonon sijaan!
    String hyvaksymiskirjeetAktivointi(@Simple("property.hakukohdeOid") @Property("hakukohdeOid") String hakukohdeOid,
            @Simple("property.valintakoeOid") @Property("valintakoeOid") List<String> valintakoeOid);

}
