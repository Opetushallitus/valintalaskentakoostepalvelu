package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import io.reactivex.Observable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SuoritusrekisteriAsyncResource {

  Observable<Oppija> getSuorituksetByOppija(String opiskelijaOid, String hakuOid);

  CompletableFuture<List<Oppija>> getSuorituksetByOppijas(
      List<String> opiskelijaOids, String hakuOid, boolean fetchEnsikertalaisuus);

  Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(String opiskelijaOid);

  CompletableFuture<List<Oppija>> getSuorituksetForOppijasWithoutEnsikertalaisuus(
      List<String> opiskelijaOids);
}
