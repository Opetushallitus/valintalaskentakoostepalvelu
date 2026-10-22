package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("mockresources")
@Service
public class MockSuoritusrekisteriAsyncResource implements SuoritusrekisteriAsyncResource {
  private static AtomicReference<Oppija> oppijaRef = new AtomicReference<>();
  private static AtomicReference<List<Oppija>> oppijatRef = new AtomicReference<>();

  public static void setResult(Oppija oppija) {
    oppijaRef.set(oppija);
  }

  public static void setResults(List<Oppija> oppijat) {
    oppijatRef.set(oppijat);
  }

  public static synchronized void clear() {
    oppijaRef.set(null);
    oppijatRef.set(null);
  }

  @Override
  public CompletableFuture<List<Oppija>> getSuorituksetByOppijas(
      List<String> opiskelijaOids, String hakuOid, boolean fetchEnsikertalaisuus) {
    return CompletableFuture.completedFuture(oppijatRef.get());
  }

  @Override
  public synchronized Observable<Oppija> getSuorituksetByOppija(
      String opiskelijaOid, String hakuOid) {
    return Observable.just(oppijaRef.get());
  }

  @Override
  public Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(final String opiskelijaOid) {
    Oppija oppija = oppijaRef.get();
    if (oppija == null) {
      List<Oppija> oppijas = oppijatRef.get();
      Optional<Oppija> first =
          oppijas.stream()
              .filter(oppija1 -> oppija1.getOppijanumero().equalsIgnoreCase(opiskelijaOid))
              .findFirst();
      oppija = first.get();
    }
    return Observable.just(oppija);
  }

  @Override
  public CompletableFuture<List<Oppija>> getSuorituksetForOppijasWithoutEnsikertalaisuus(
      List<String> opiskelijaOids) {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }
}
