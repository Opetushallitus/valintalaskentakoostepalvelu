package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface SuoritusrekisteriAsyncResource {

    Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid, String referenssiPvm);

    Peruutettava getOppijatByHakukohde(String hakukohdeOid, String referenssiPvm,
                                       Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback);

    Future<Response> getSuorituksetByOppija(String opiskelijaOid, Consumer<Oppija> callback, Consumer<Throwable> failureCallback);
}
