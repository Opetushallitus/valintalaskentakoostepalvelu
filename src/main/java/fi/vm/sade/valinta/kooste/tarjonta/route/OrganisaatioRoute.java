package fi.vm.sade.valinta.kooste.tarjonta.route;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import org.apache.camel.Property;

public interface OrganisaatioRoute {
  OrganisaatioRDTO haeOrganisaatio(@Property("tarjoajaOid") String tarjoajaOid);
}
