package fi.vm.sade.valinta.kooste.sijoittelu.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface SijoitteluAktivointiRoute {

    final String DIRECT_SIJOITTELU_AKTIVOI = "direct:kaynnistaSijoitteluReitti";

    void aktivoiSijoittelu(@Property(OPH.HAKUOID) String hakuOid);
}
