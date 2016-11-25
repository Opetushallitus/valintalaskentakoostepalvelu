package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RyhmasahkopostiAsyncResource;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class RyhmasahkopostiAsyncResourceImpl extends AsyncResourceWithCas implements RyhmasahkopostiAsyncResource {

    @Autowired
    public RyhmasahkopostiAsyncResourceImpl(
            @Qualifier("ryhmasahkopostiClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            @Value("${valintalaskentakoostepalvelu.ryhmasahkoposti.url}") String address,
            ApplicationContext context
    ) {
        super(casInterceptor, address, context, TimeUnit.HOURS.toMillis(20));
    }

    @Override
    public Observable<Optional<Long>> haeRyhmasahkopostiIdByLetterObservable(Long letterId) {
        return getAsObservable(
                "/reportMessages/view/letter/" + letterId,
                (idAsString) -> Optional.of(Long.parseLong(idAsString)),
                client -> {
                    client.accept(MediaType.TEXT_PLAIN_TYPE);
                    return client;
                }
        ).onErrorReturn(error -> {
            if(error instanceof HttpExceptionWithResponse && ((HttpExceptionWithResponse) error).status == 404) {
                return Optional.empty();
            }
            if(error instanceof RuntimeException) {
                throw (RuntimeException) error;
            }
            throw new RuntimeException(error);
        });
    }

}
