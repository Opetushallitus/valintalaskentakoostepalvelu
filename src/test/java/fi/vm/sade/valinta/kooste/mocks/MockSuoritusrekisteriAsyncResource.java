package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
public class MockSuoritusrekisteriAsyncResource implements SuoritusrekisteriAsyncResource {
    private static AtomicReference<Oppija> oppijaRef = new AtomicReference<>();

    public static void setResult(Oppija oppija) {
        oppijaRef.set(oppija);
    }

    public static void clear() {
        oppijaRef.set(null);
    }

    @Override
    public Peruutettava getOppijatByHakukohde(String hakukohdeOid, String referenssiPvm, Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback) {
        return null;
    }

    @Override
    public Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid, String referenssiPvm) {
        return null;
    }

    @Override
    public Future<Response> getSuorituksetByOppija(String opiskelijaOid, Consumer<Oppija> callback, Consumer<Throwable> failureCallback) {
        callback.accept(oppijaRef.get());
        return Futures.immediateCancelledFuture();
    }
}