package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class MockOrganisaationAsyncResource implements OrganisaatioAsyncResource {

    private static AtomicReference<OrganisaatioTyyppiHierarkia> hierarkiaRef = new AtomicReference<>();

    public static void setOrganisaationTyyppiHierarkia(OrganisaatioTyyppiHierarkia hierarkia) {
        hierarkiaRef.set(hierarkia);
    }

    public static void clear() {
        hierarkiaRef.set(null);
    }

    @Override
    public Observable<Response> haeOrganisaatio(String organisaatioOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<OrganisaatioTyyppiHierarkia> haeOrganisaationTyyppiHierarkiaSisaltaenLakkautetut(String organisaatioOid) {
        return CompletableFuture.completedFuture(hierarkiaRef.get());
    }

    @Override
    public CompletableFuture<Optional<HakutoimistoDTO>> haeHakutoimisto(String organisaatioId) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }
}
