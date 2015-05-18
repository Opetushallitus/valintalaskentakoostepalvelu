package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.service.valintaperusteet.dto.*;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
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
    public Future<List<ValintakoeDTO>> haeValintakokeetTunnisteilla(Collection<String> tunnisteet) {
        return Futures.immediateFuture(valintakokeetResultReference.get());
    }

    @Override
    public Peruutettava haeValintakokeetTunnisteilla(Collection<String> tunnisteet, Consumer<List<ValintakoeDTO>> callback, Consumer<Throwable> failureCallback) {
        callback.accept(valintakokeetResultReference.get());
        return new PeruutettavaImpl(Futures.immediateFuture(valintakokeetResultReference.get()));
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
    public Peruutettava haeHakijaryhmat(String hakukohdeOid, Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava haunHakukohteet(String hakuOid, Consumer<List<HakukohdeViiteDTO>> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Peruutettava findAvaimet(String hakukohdeOid, Consumer<List<ValintaperusteDTO>> callback, Consumer<Throwable> failureCallback) {
        callback.accept(valintaperusteetResultReference.get());
        return new PeruutettavaImpl(Futures.immediateFuture(valintaperusteetResultReference.get()));
    }

    @Override
    public Peruutettava haeValintaperusteet(String hakukohdeOid, Integer valinnanVaiheJarjestysluku, Consumer<List<ValintaperusteetDTO>> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<List<ValinnanVaiheJonoillaDTO>> ilmanLaskentaa(String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Response> tuoHakukohde(HakukohdeImportDTO hakukohde) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<List<ValintakoeDTO>> haeValintakokeet(Collection<String> oids) {
        return Futures.immediateFuture(valintakokeetResultReference.get());
    }

    @Override
    public Future<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(Collection<String> hakukohdeOids) {
        return Futures.immediateFuture(hakukohdeResultReference.get());
    }

    @Override
    public Peruutettava haeIlmanlaskentaa(String hakukohdeOid, Consumer<List<ValinnanVaiheJonoillaDTO>> callback, Consumer<Throwable> failureCallback) {
        throw new UnsupportedOperationException();
    }
}
