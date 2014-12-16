package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import jersey.repackaged.com.google.common.util.concurrent.Futures;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.springframework.stereotype.Service;

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
    public final List<Result> results = new ArrayList<>();
    @Override
    public Future<Response> tuoErillishaunTilat(final String hakuOid, final String hakukohdeOid, final String valintatapajononNimi, final Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
        results.add(new Result(hakuOid, hakukohdeOid, valintatapajononNimi, erillishaunHakijat));
        return Futures.immediateFuture(null);
    }
}
