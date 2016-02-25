package fi.vm.sade.valinta.kooste.external.resource.sijoittelu;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.ws.rs.core.Response;

import fi.vm.sade.sijoittelu.domain.Valintatulos;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import rx.Observable;

public interface TilaAsyncResource {
    Observable<Response> tuoErillishaunTilat(String hakuOid, String hakukohdeOid, String valintatapajononNimi, Collection<ErillishaunHakijaDTO> erillishaunHakijat);

    @Deprecated
    Future<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid, String valintatapajonoOid);

    Observable<List<Valintatulos>> getValintatuloksetHakukohteelle(String hakukohdeOid);

}
