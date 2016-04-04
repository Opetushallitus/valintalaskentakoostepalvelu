package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.service.valintaperusteet.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
@Service
public class MockValintaperusteetAsyncResource implements ValintaperusteetAsyncResource {
    private static AtomicReference<List<HakukohdeJaValintakoeDTO>> hakukohdeResultReference = new AtomicReference<>();
    private static AtomicReference<List<ValinnanVaiheJonoillaDTO>> resultReference = new AtomicReference<>();
    private static AtomicReference<List<ValintaperusteDTO>> valintaperusteetResultReference = new AtomicReference<>();
    private static AtomicReference<List<ValintakoeDTO>> valintakokeetResultReference = new AtomicReference<>();
    public static void setValintaperusteetResultReference(List<ValintaperusteDTO> result) {
        valintaperusteetResultReference.set(result);
    }

    @Override
    public Observable<List<ValintaperusteetHakijaryhmaDTO>> haeHakijaryhmat(String hakukohdeOid) {
        return null;
    }

    @Override
    public Observable<List<ValintaperusteetDTO>> haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku) {
        return null;
    }

    public static void setValintakokeetResult(List<ValintakoeDTO> result) {
        valintakokeetResultReference.set(result);
    }
    public static void setResult(List<ValinnanVaiheJonoillaDTO> result) {
        resultReference.set(result);
    }
    public static void setHakukohdeResult(List<HakukohdeJaValintakoeDTO> result) {
        hakukohdeResultReference.set(result);
    }
    public static void clear() {
        resultReference.set(null);
    }

    @Override
    public Future<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid) {
        return Futures.immediateFuture(valintakokeetResultReference.get());
    }

    @Override
    public Peruutettava haeValintakokeetHakukohteelle(String hakukohdeOid, Consumer<List<ValintakoeDTO>> callback, Consumer<Throwable> failureCallback) {
        callback.accept(valintakokeetResultReference.get());
        return new PeruutettavaImpl(Futures.immediateFuture(valintakokeetResultReference.get()));
    }

    @Override
    public Peruutettava haeValintakokeetHakukohteille(Collection<String> hakukohdeOids, Consumer<List<HakukohdeJaValintakoeDTO>> callback, Consumer<Throwable> failureCallback) {
        callback.accept(hakukohdeResultReference.get());
        return new PeruutettavaImpl(Futures.immediateFuture(hakukohdeResultReference.get()));
    }

    @Override
    public Peruutettava haeValinnanvaiheetHakukohteelle(String hakukohdeOid, Consumer<List<ValinnanVaiheJonoillaDTO>> callback, Consumer<Throwable> failureCallback) {
        callback.accept(resultReference.get());
        return new PeruutettavaImpl(Futures.immediateCancelledFuture());
    }

    @Override
    public Observable<Set<String>> haeHakukohteetValinnanvaiheelle(String oid) {
        Set<String> hakukohdelist = Sets.newHashSet("1.2.3.4", "4.3.2.1");
        return Observable.just(hakukohdelist);
    }

    @Override
    public Peruutettava haeHakijaryhmat(String hakukohdeOid, Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava haunHakukohteet(String hakuOid, Consumer<List<HakukohdeViiteDTO>> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid) {
        return Observable.just(valintaperusteetResultReference.get());
    }

    @Override
    public Observable<List<HakukohdeJaValintaperusteDTO>> findAvaimet(Collection<String> hakukohdeOids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<List<ValintaperusteetDTO>> valintaperusteet(String valinnanvaiheOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<List<ValintakoeDTO>> readByTunnisteet(Collection<String> tunnisteet) {
        throw new UnsupportedOperationException();
    }
    @Override
    public Peruutettava haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku, Consumer<List<ValintaperusteetDTO>> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Response> tuoHakukohde(HakukohdeImportDTO hakukohde) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(Collection<String> hakukohdeOids) {
        return Futures.immediateFuture(hakukohdeResultReference.get());
    }

    @Override
    public Observable<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakutoiveille(Collection<String> hakukohdeOids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Observable<List<ValinnanVaiheJonoillaDTO>> haeIlmanlaskentaa(String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }
}
