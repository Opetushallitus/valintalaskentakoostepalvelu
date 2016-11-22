package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import rx.Observable;

import java.util.Optional;

public interface RyhmasahkopostiAsyncResource {
    Observable<Optional<Long>> haeRyhmasahkopostiIdByLetterObservable(Long letterId);
}
