package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface HyvaksymiskirjeRoute {
    final String DIRECT_HYVAKSYMISKIRJEET = "direct:hyvaksymiskirjeet";

    void hyvaksymiskirjeetAktivointi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
            @Property(OPH.HAKUOID) String hakuOid, @Property(OPH.SIJOITTELUAJOID) Long sijoitteluajoId);
}
