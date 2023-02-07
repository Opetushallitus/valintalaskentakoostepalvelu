package fi.vm.sade.valinta.kooste.tarjonta.route;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;

public interface OrganisaatioRoute {
  OrganisaatioRDTO haeOrganisaatio(String tarjoajaOid);
}
