package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.Organisaatio;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class MockOrganisaationAsyncResource implements OrganisaatioAsyncResource {

  private static AtomicReference<OrganisaatioTyyppiHierarkia> hierarkiaRef =
      new AtomicReference<>();

  public static void setOrganisaationTyyppiHierarkia(OrganisaatioTyyppiHierarkia hierarkia) {
    hierarkiaRef.set(hierarkia);
  }

  public static void clear() {
    hierarkiaRef.set(null);
  }

  @Override
  public CompletableFuture<Organisaatio> haeOrganisaatio(String organisaatioOid) {
    Organisaatio organisaatio = new Organisaatio();
    organisaatio.setOid(organisaatioOid);
    return CompletableFuture.completedFuture(organisaatio);
  }

  @Override
  public CompletableFuture<OrganisaatioTyyppiHierarkia> haeOrganisaationTyyppiHierarkia(
      String organisaatioOid) {
    return CompletableFuture.completedFuture(hierarkiaRef.get());
  }

  @Override
  public CompletableFuture<Optional<HakutoimistoDTO>> haeHakutoimisto(String organisaatioId) {
    return CompletableFuture.failedFuture(new UnsupportedOperationException());
  }
}
