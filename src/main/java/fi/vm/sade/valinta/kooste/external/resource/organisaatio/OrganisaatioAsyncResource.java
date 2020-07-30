package fi.vm.sade.valinta.kooste.external.resource.organisaatio;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import io.reactivex.Observable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.ws.rs.core.Response;

public interface OrganisaatioAsyncResource {
  /** @param organisaatioOid == tarjoajaOid */
  Observable<Response> haeOrganisaatio(String organisaatioOid);

  CompletableFuture<OrganisaatioTyyppiHierarkia>
      haeOrganisaationTyyppiHierarkiaSisaltaenLakkautetut(String organisaatioOid);

  CompletableFuture<Optional<HakutoimistoDTO>> haeHakutoimisto(String organisaatioId);
}
