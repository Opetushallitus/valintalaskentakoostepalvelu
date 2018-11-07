package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.impl;

import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

@Service
public class OhjausparametritAsyncResourceImpl extends UrlConfiguredResource implements OhjausparametritAsyncResource {

    @Autowired
    public OhjausparametritAsyncResourceImpl(
            @Value("${valintalaskentakoostepalvelu.ohjausparametrit.request.timeout.seconds:20}") int requestTimeoutSeconds) {
        super(TimeUnit.SECONDS.toMillis(requestTimeoutSeconds));
    }

    @Override
    public Observable<ParametritDTO> haeHaunOhjausparametrit(String hakuOid) {
        Observable<ParametritDTO> intermediate = getAsObservableLazily(
                getUrl("ohjausparametrit-service.parametri", hakuOid),
                ParametritDTO.class
        );
        return intermediate.onErrorReturn(error -> {
            if (HttpExceptionWithResponse.isResponseWithStatus(Response.Status.NOT_FOUND, error)) {
                return new ParametritDTO();
            }
            if(error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new RuntimeException(error);
        });
    }
}
