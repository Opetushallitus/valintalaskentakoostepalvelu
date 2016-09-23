package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class MockSuoritusrekisteriAsyncResource implements SuoritusrekisteriAsyncResource {
    private static AtomicReference<Oppija> oppijaRef = new AtomicReference<>();

    public static void setResult(Oppija oppija) {
        oppijaRef.set(oppija);
    }

    public static AtomicReference<List<Suoritus>> suorituksetRef = new AtomicReference<>(new ArrayList<>());
    public static AtomicReference<List<Arvosana>> arvosanatRef = new AtomicReference<>(new ArrayList<>());

    private static AtomicInteger ids = new AtomicInteger(100);

    private static Optional<RuntimeException> postException = Optional.empty();

    public static void clear() {
        postException = Optional.empty();
        oppijaRef.set(null);
        suorituksetRef.set(new ArrayList<>());
        arvosanatRef.set(new ArrayList<>());
    }

    public static void setPostException(Optional<RuntimeException> exception) {
        MockSuoritusrekisteriAsyncResource.postException = exception;
    }


    @Override
    public Peruutettava getOppijatByHakukohde(String hakukohdeOid, String hakuOid, Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback) {
        return null;
    }

    @Override
    public Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid, String hakuOid) {
        return Observable.just(ImmutableList.of(oppijaRef.get()));
    }

    @Override
    public Future<Response> getSuorituksetByOppija(String opiskelijaOid, String hakuOid, Consumer<Oppija> callback, Consumer<Throwable> failureCallback) {
        callback.accept(oppijaRef.get());
        return Futures.immediateCancelledFuture();
    }

    @Override
    public Observable<Oppija> getSuorituksetByOppija(String opiskelijaOid, String hakuOid) {
        return Observable.just(oppijaRef.get());
    }

    @Override
    public Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(String opiskelijaOid) {
        return Observable.just(oppijaRef.get());
    }

    @Override
    public Observable<Suoritus> postSuoritus(Suoritus suoritus) {
        if (postException.isPresent()) {
            throw postException.get();
        }
        suoritus.setId("" + ids.getAndIncrement());
        suorituksetRef.getAndUpdate((List<Suoritus> suoritukset) -> {
                suoritukset.add(suoritus);
                return suoritukset;
        });
        return Observable.just(suoritus);
    }

    @Override
    public Observable<Arvosana> postArvosana(Arvosana arvosana) {
        if (postException.isPresent()) {
            throw postException.get();
        }
        arvosanatRef.getAndUpdate((List<Arvosana> arvosanat) -> {
            arvosanat.add(arvosana);
            return arvosanat;
        });
        return Observable.just(arvosana);
    }
}
