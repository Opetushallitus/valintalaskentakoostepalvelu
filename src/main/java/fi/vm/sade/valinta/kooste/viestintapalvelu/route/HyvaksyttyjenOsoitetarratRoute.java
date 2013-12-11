package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface HyvaksyttyjenOsoitetarratRoute {
    final String DIRECT_HYVAKSYTTYJEN_OSOITETARRAT = "direct:hyvaksyttyjen_osoitetarrat";

    void hyvaksyttyjenOsoitetarrojenAktivointi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
            @Property(OPH.HAKUOID) String hakuOid, @Property(OPH.SIJOITTELUAJOID) Long sijoitteluajoId);
}
