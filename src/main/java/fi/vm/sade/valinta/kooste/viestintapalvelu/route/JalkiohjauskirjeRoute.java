package fi.vm.sade.valinta.kooste.viestintapalvelu.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface JalkiohjauskirjeRoute {
    final String DIRECT_JALKIOHJAUSKIRJEET = "direct:jalkiohjauskirjeet";

    void jalkiohjauskirjeetAktivoi(@Property(OPH.HAKUOID) String hakuOid);
}
