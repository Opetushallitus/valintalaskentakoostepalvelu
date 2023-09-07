package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.impl;

import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RyhmasahkopostiAsyncResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import io.reactivex.Observable;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RyhmasahkopostiAsyncResourceImpl implements RyhmasahkopostiAsyncResource {

  private final RestCasClient restCasClient;

  private final UrlConfiguration urlConfiguration;

  @Autowired
  public RyhmasahkopostiAsyncResourceImpl(
      @Qualifier("ryhmasahkopostiCasClient") RestCasClient restCasClient) {
    this.restCasClient = restCasClient;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  @Override
  public Observable<Optional<Long>> haeRyhmasahkopostiIdByLetterObservable(Long letterId) {
    return Observable.fromFuture(
        this.restCasClient
            .get(
                this.urlConfiguration.url("viestintapalvelu.reportMessages", letterId),
                Map.of("Accept", "text/plain"),
                10 * 60 * 1000)
            .thenApply(
                response -> {
                  if (response.getStatusCode() == 404) {
                    return Optional.empty();
                  }
                  return Optional.of(Long.parseLong(response.getResponseBody()));
                }));
  }
}
