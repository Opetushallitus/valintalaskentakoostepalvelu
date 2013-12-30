package fi.vm.sade.valinta.kooste.tarjonta.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         10 retries!
 */
public interface LinjakoodiRoute {

    String haeLinjakoodi(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid);
}
