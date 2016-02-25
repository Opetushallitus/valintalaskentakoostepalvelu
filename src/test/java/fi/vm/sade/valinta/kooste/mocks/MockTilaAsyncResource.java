package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;
import rx.Observable;

import static fi.vm.sade.valinta.kooste.mocks.MockData.hakemusOid;
import static fi.vm.sade.valinta.kooste.mocks.MockData.hakijaOid;
import static fi.vm.sade.valinta.kooste.mocks.MockData.hakuOid;

@Service
public class MockTilaAsyncResource implements TilaAsyncResource {
    public static class Result {
        public final String hakuOid;
        public final String hakukohdeOid;
        public final String valintatapajononNimi;
        public final Collection<ErillishaunHakijaDTO> erillishaunHakijat;

        public Result(final String hakuOid, final String hakukohdeOid, final String valintatapajononNimi, final Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
            this.hakuOid = hakuOid;
            this.hakukohdeOid = hakukohdeOid;
            this.valintatapajononNimi = valintatapajononNimi;
            this.erillishaunHakijat = erillishaunHakijat;
        }
    }
    private static AtomicReference<List<Valintatulos>> resultReference = new AtomicReference<>();

    @Override
    public Future<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid, String valintatapajonoOid) {
        return Futures.immediateFuture(Collections.singletonList(new Valintatulos(valintatapajonoOid, hakemusOid, hakukohdeOid, hakijaOid, hakuOid, 1)));
    }

    @Override
    public Observable<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid) {
        return null;
    }

    public static void setResult(List<Valintatulos> result) {
        resultReference.set(result);
    }
    public static void clear() {
        resultReference.set(null);
    }

    public final List<Result> results = new ArrayList<>();
    @Override
    public Observable<Response> tuoErillishaunTilat(final String hakuOid, final String hakukohdeOid, final String valintatapajononNimi, final Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
        results.add(new Result(hakuOid, hakukohdeOid, valintatapajononNimi, erillishaunHakijat));
        return Observable.just(null);
    }

}
