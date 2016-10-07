package fi.vm.sade.valinta.kooste.external.resource.organisaatio;

import java.util.Optional;
import java.util.concurrent.Future;

import javax.ws.rs.core.Response;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import rx.Observable;

public interface OrganisaatioAsyncResource {
    /**
     * @param organisaatioOid == tarjoajaOid
     */
    Future<Response> haeOrganisaatio(String organisaatioOid);

    Observable<OrganisaatioTyyppiHierarkia> haeOrganisaationTyyppiHierarkia(String organisaatioOid);

    Observable<OrganisaatioTyyppiHierarkia> haeOrganisaationTyyppiHierarkiaSisaltaenLakkautetut(String organisaatioOid);

    Observable<Optional<HakutoimistoDTO>> haeHakutoimisto(String organisaatioId);

}
