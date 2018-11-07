package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RyhmasahkopostiAsyncResource;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RyhmasahkopostiAsyncResourceImpl extends UrlConfiguredResource implements RyhmasahkopostiAsyncResource {

    @Autowired
    public RyhmasahkopostiAsyncResourceImpl(
            @Qualifier("ryhmasahkopostiClientCasInterceptor") AbstractPhaseInterceptor casInterceptor
    ) {
        super(TimeUnit.HOURS.toMillis(20), casInterceptor);
    }

    @Override
    public Observable<Optional<Long>> haeRyhmasahkopostiIdByLetterObservable(Long letterId) {
        return getAsObservableLazily(
                getUrl("viestintapalvelu.reportMessages", letterId),
                (idAsString) -> Optional.of(Long.parseLong(idAsString)),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                }
        ).onErrorReturn(error -> {
            if (HttpExceptionWithResponse.isResponseWithStatus(Response.Status.NOT_FOUND, error)) {
                return Optional.empty();
            }
            if(error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new RuntimeException(error);
        });
    }

}
