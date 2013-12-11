package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import java.util.List;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface OsoitetarratRoute {
    final String DIRECT_OSOITETARRAT = "direct:osoitetarrat";

    void osoitetarratAktivointi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
    // pitaisikohan olla vain bodyssa ja kasitella yksi oid kerrallaan?
            @Property("valintakoeOid") List<String> valintakoeOid);
}
