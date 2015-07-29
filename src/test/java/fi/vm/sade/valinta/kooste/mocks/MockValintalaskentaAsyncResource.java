package fi.vm.sade.valinta.kooste.mocks;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import rx.Observable;

/**
 * @author Jussi Jartamo
 */
@Service
public class MockValintalaskentaAsyncResource implements ValintalaskentaAsyncResource {

    private static AtomicReference<List<ValintatietoValinnanvaiheDTO>> resultReference = new AtomicReference<>();

    public static void setResult(List<ValintatietoValinnanvaiheDTO> result) {
        resultReference.set(result);
    }
    public static void clear() {
        resultReference.set(null);
    }

    @Override
    public Observable<String> laske(LaskeDTO laskeDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<String> laskeKaikki(LaskeDTO laskeDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<String> valintakokeet(LaskeDTO laskeDTO) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid) {
        return Observable.just(resultReference.get());
    }

    @Override
    public Observable<ValinnanvaiheDTO> lisaaTuloksia(final String hakuOid, final String hakukohdeOid, final String tarjoajaOid, final ValinnanvaiheDTO vaihe) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava laskeJaSijoittele(List<LaskeDTO> lista, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

}

