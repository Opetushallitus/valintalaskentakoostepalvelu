package fi.vm.sade.valinta.kooste.kela.route;

import static fi.vm.sade.valinta.kooste.kela.route.KelaRoute.PROPERTY_DOKUMENTTI_ID;

import org.apache.camel.Property;

/**
 *         Taman voisi yhdistaa KelaRouteen kun proxyyn tekisi produce
 *         annotaatiotuen.
 */
public interface KelaFtpRoute {
    void aloitaKelaSiirto(@Property(PROPERTY_DOKUMENTTI_ID) String dokumenttiId);
}
