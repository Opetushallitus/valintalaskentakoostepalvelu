package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import io.reactivex.Observable;

import java.util.Optional;

public interface RyhmasahkopostiAsyncResource {
    Observable<Optional<Long>> haeRyhmasahkopostiIdByLetterObservable(Long letterId);
}
