package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import rx.Observable;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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
    public Observable<String> valintakokeet(LaskeDTO laskeDTO) {
        return null;
    }

    @Override
    public Peruutettava lisaaTuloksia(String hakuOid, String hakukohdeOid, String tarjoajaOid, ValinnanvaiheDTO vaihe, Consumer<ValinnanvaiheDTO> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(String hakukohdeOid) {
        return Observable.just(Collections.EMPTY_LIST);
    }

    @Override
    public Peruutettava laske(LaskeDTO laskeDTO, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava valintakokeet(LaskeDTO laskeDTO, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava laskeKaikki(LaskeDTO laskeDTO, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava laskeJaSijoittele(List<LaskeDTO> lista, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

}

