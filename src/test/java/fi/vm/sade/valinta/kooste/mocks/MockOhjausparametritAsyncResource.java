package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.function.Consumer;

@Service
public class MockOhjausparametritAsyncResource implements OhjausparametritAsyncResource {
    @Override
    public Peruutettava haeHaunOhjausparametrit(String hakuOid, Consumer<ParametritDTO> callback, Consumer<Throwable> failureCallback) {
        return new Peruutettava() {
            @Override
            public void peruuta() {

            }
        };
    }

    @Override
    public Observable<ParametritDTO> haeHaunOhjausparametrit(String hakuOid) {
        return Observable.just(new ParametritDTO());
    }
}
