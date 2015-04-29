package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
@Service
public class MockValintalaskentaValintakoeAsyncResource implements ValintalaskentaValintakoeAsyncResource {

    private static final AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedot = new AtomicReference<>();

    public static void setResult(List<ValintakoeOsallistuminenDTO> res) {
        osallistumistiedot.set(res);
    }

    @Override
    public Future<List<ValintakoeOsallistuminenDTO>> haeOsallistumiset(Collection<String> hakemusOid) {
        return Futures.immediateFuture(osallistumistiedot.get());
    }

    @Override
    public Future<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(String hakukohdeOid) {
        return Futures.immediateFuture(osallistumistiedot.get());
    }

    @Override
    public Peruutettava haeHakutoiveelle(String hakukohdeOid, Consumer<List<ValintakoeOsallistuminenDTO>> callback, Consumer<Throwable> failureCallback) {
        callback.accept(osallistumistiedot.get());
        return new PeruutettavaImpl(Futures.immediateCancelledFuture());
    }
}
