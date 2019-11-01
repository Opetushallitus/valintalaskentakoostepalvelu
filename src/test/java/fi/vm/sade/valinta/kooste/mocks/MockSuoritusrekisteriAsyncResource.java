package fi.vm.sade.valinta.kooste.mocks;

import com.google.common.collect.ImmutableList;

import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import io.reactivex.Observable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

    public static AtomicReference<List<Suoritus>> suorituksetRef = new AtomicReference<>(new ArrayList<>());
    public static AtomicReference<List<Arvosana>> createdArvosanatRef = new AtomicReference<>(new ArrayList<>());
    public static AtomicReference<List<Arvosana>> updatedArvosanatRef = new AtomicReference<>(new ArrayList<>());

    private static AtomicInteger ids = new AtomicInteger(100);

    private static Optional<RuntimeException> postException = Optional.empty();

    public static AtomicReference<List<String>> deletedSuorituksetRef = new AtomicReference<>(new ArrayList<>());
    public static AtomicReference<List<String>> deletedArvosanatRef = new AtomicReference<>(new ArrayList<>());

    public static void clear() {
        postException = Optional.empty();
        oppijaRef.set(null);
        oppijatRef.set(null);
        suorituksetRef.set(new ArrayList<>());
        createdArvosanatRef.set(new ArrayList<>());
        updatedArvosanatRef.set(new ArrayList<>());
        deletedSuorituksetRef.set(new ArrayList<>());
        deletedArvosanatRef.set(new ArrayList<>());
    }

    public static void setPostException(Optional<RuntimeException> exception) {
        MockSuoritusrekisteriAsyncResource.postException = exception;
    }


    @Override
    public Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid, String hakuOid) {
        return Observable.just(ImmutableList.of(oppijaRef.get()));
    }

    @Override
    public CompletableFuture<List<Oppija>> getOppijatByHakukohdeWithoutEnsikertalaisuus(String hakukohdeOid, String hakuOid) {
        return CompletableFuture.completedFuture(ImmutableList.of(oppijaRef.get()));
    }

    @Override
    public CompletableFuture<List<Oppija>> getSuorituksetByOppijas(List<String> opiskelijaOids, String hakuOid) {
        return CompletableFuture.completedFuture(oppijatRef.get());
    }

    @Override
    public Observable<Oppija> getSuorituksetByOppija(String opiskelijaOid, String hakuOid) {
        return Observable.just(oppijaRef.get());
    }

    @Override
    public Observable<List<Oppija>> getSuorituksetWithoutEnsikertalaisuus(List<String> opiskelijaOids) {
        return Observable.just(oppijatRef.get());
    }

    @Override
    public Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(final String opiskelijaOid) {
        Oppija oppija = oppijaRef.get();
        if (oppija == null) {
            List<Oppija> oppijas = oppijatRef.get();
            Optional<Oppija> first = oppijas.stream()
                    .filter(oppija1 -> oppija1.getOppijanumero().equalsIgnoreCase(opiskelijaOid))
                    .findFirst();
            oppija = first.get();
        }
        return Observable.just(oppija);
    }

    @Override
    public CompletableFuture<Suoritus> postSuoritus(Suoritus suoritus) {
        if (postException.isPresent()) {
            return CompletableFuture.failedFuture(postException.get());
        }
        suoritus.setId("" + ids.getAndIncrement());
        suorituksetRef.getAndUpdate((List<Suoritus> suoritukset) -> {
                suoritukset.add(suoritus);
                return suoritukset;
        });
        return CompletableFuture.completedFuture(suoritus);
    }

    @Override
    public CompletableFuture<Arvosana> postArvosana(Arvosana arvosana) {
        if (postException.isPresent()) {
            return CompletableFuture.failedFuture(postException.get());
        }
        createdArvosanatRef.getAndUpdate((List<Arvosana> arvosanat) -> {
            arvosanat.add(arvosana);
            return arvosanat;
        });
        return CompletableFuture.completedFuture(arvosana);
    }

    @Override
    public CompletableFuture<Arvosana> updateExistingArvosana(String arvosanaId, Arvosana arvosanaWithUpdatedValues) {
        if (postException.isPresent()) {
            return CompletableFuture.failedFuture(postException.get());
        }
        updatedArvosanatRef.getAndUpdate((List<Arvosana> arvosanat) -> {
            arvosanat.add(arvosanaWithUpdatedValues);
            return arvosanat;
        });
        return CompletableFuture.completedFuture(arvosanaWithUpdatedValues);
    }

    @Override
    public CompletableFuture<String> deleteSuoritus(String suoritusId) {
        deletedSuorituksetRef.getAndUpdate((List<String> suoritusIdt) -> {
            suoritusIdt.add(suoritusId);
            return suoritusIdt;
        });
        Suoritus suoritus = new Suoritus();
        suoritus.setId(suoritusId);
        return CompletableFuture.completedFuture("OK");
    }

    @Override
    public CompletableFuture<String> deleteArvosana(String arvosanaId) {
        deletedArvosanatRef.getAndUpdate((List<String> arvosanaIdt) -> {
            arvosanaIdt.add(arvosanaId);
            return arvosanaIdt;
        });
        Arvosana arvosana = new Arvosana();
        arvosana.setId(arvosanaId);
        return CompletableFuture.completedFuture("OK");
    }
}
