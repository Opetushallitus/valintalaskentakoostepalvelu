package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

@Service
public class MockOhjausparametritAsyncResource implements OhjausparametritAsyncResource {
    @Override
    public Observable<ParametritDTO> haeHaunOhjausparametrit(String hakuOid) {
        return Observable.just(new ParametritDTO());
    }
}
