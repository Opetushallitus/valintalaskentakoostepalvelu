package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import org.springframework.stereotype.Service;
import rx.Observable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.Future;
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
    public Future<Response> haeOrganisaatio(String organisaatioOid) {
        throw new NotImplementedException();
    }

    @Override
    public Observable<OrganisaatioTyyppiHierarkia> haeOrganisaationTyyppiHierarkiaSisaltaenLakkautetut(String organisaatioOid) {
        return Observable.just(hierarkiaRef.get());
    }

    @Override
    public Observable<Optional<HakutoimistoDTO>> haeHakutoimisto(String organisaatioId) {
        throw new NotImplementedException();
    }
}
