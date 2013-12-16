package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * User: wuoti Date: 27.5.2013 Time: 9.05
 */
public interface HaunValintalaskentaRoute {

    /**
     * Camel route description.
     */
    String DIRECT_VALINTALASKENTA_HAULLE = "direct:valintalaskenta_haulle";

    void aktivoiValintalaskenta(@Property(OPH.HAKUOID) String hakuOid);
}
