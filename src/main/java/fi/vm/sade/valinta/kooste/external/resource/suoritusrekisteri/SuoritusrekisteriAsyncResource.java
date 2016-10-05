package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri;

import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface SuoritusrekisteriAsyncResource {

    Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid,
                                                   String hakuOid);

    @Deprecated
    Peruutettava getOppijatByHakukohde(String hakukohdeOid,
                                       String hakuOid,
                                       Consumer<List<Oppija>> callback,
                                       Consumer<Throwable> failureCallback);
    @Deprecated
    Future<Response> getSuorituksetByOppija(String opiskelijaOid,
                                            String hakuOid,
                                            Consumer<Oppija> callback,
                                            Consumer<Throwable> failureCallback);

    Observable<Oppija> getSuorituksetByOppija(String opiskelijaOid, String hakuOid);

    Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(String opiskelijaOid);

    Observable<Suoritus> postSuoritus(Suoritus suoritus);

    Observable<Arvosana> postArvosana(Arvosana arvosana);

    Observable<Suoritus> deleteSuoritus(String suoritusId);
}
