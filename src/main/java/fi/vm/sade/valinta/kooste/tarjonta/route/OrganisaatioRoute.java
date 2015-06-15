package fi.vm.sade.valinta.kooste.tarjonta.route;

import org.apache.camel.Property;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;

public interface OrganisaatioRoute {
    OrganisaatioRDTO haeOrganisaatio(@Property("tarjoajaOid") String tarjoajaOid);
}
