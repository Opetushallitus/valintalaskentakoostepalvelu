package fi.vm.sade.valinta.kooste.mocks;

import static fi.vm.sade.valinta.kooste.mocks.MockData.*;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.TilaAsyncResource;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

    @Override
    public Observable<List<Valintatulos>> getValintatuloksetValintatapajonolle(String hakukohdeOid, String valintatapajonoOid) {
        return Observable.just(Collections.singletonList(new Valintatulos(valintatapajonoOid, hakemusOid, hakukohdeOid, hakijaOid, hakuOid, 1)));
    }

    @Override
    public Observable<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid) {
        return null;
    }

    @Override
    public Observable<Valintatulos> getHakemuksenSijoittelunTulos(String hakemusOid, String hakuOid, String hakukohdeOid, String valintatapajonoOid) {
        return null;
    }

    @Override
    public Observable<List<Valintatulos>> getHakemuksenTulokset(String hakemusOid) {
        return null;
    }

    public final List<Result> results = new ArrayList<>();
    @Override
    public Observable<Response> tuoErillishaunTilat(final String hakuOid, final String hakukohdeOid, final String valintatapajononNimi, final Collection<ErillishaunHakijaDTO> erillishaunHakijat) {
        results.add(new Result(hakuOid, hakukohdeOid, valintatapajononNimi, erillishaunHakijat));
        return Observable.just(null);
    }

}
