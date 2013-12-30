package fi.vm.sade.valinta.kooste.tarjonta.route;

import org.apache.camel.Property;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Proxy organisaation kutsumiseen, seamless retry 10 kertaa!
 */
public interface OrganisaatioRoute {

    OrganisaatioRDTO haeOrganisaatio(@Property("tarjoajaOid") String tarjoajaOid);
}
