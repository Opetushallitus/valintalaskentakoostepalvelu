package fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import io.reactivex.Observable;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * https://${host.virkailija}/organisaatio-service/rest esim
 * /organisaatio-service/rest/organisaatio/1.2.246.562.10.39218317368 ?noCache=1413976497594
 */
@Service
public class OrganisaatioAsyncResourceImpl extends UrlConfiguredResource
    implements OrganisaatioAsyncResource {
  private final HttpClient client;

  @Autowired
  public OrganisaatioAsyncResourceImpl(@Qualifier("OrganisaatioHttpClient") HttpClient client) {
    super(TimeUnit.MINUTES.toMillis(1));
    this.client = client;
  }

  @Override
  public Observable<Response> haeOrganisaatio(String organisaatioOid) {
    return getAsObservableLazily(
        getUrl("organisaatio-service.organisaatio", organisaatioOid),
        webClient -> webClient.accept(MediaType.WILDCARD_TYPE));
  }

  @Override
  public CompletableFuture<OrganisaatioTyyppiHierarkia> haeOrganisaationTyyppiHierarkia(
      String organisaatioOid) {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("oid", organisaatioOid);
    parameters.put("aktiiviset", Boolean.toString(true));
    parameters.put("suunnitellut", Boolean.toString(true));
    parameters.put("lakkautetut", Boolean.toString(true));
    String url = getUrl("organisaatio-service.organisaatio.hierarkia.tyyppi", parameters);

    return client.getJson(
        url,
        Duration.ofMinutes(1),
        new com.google.gson.reflect.TypeToken<OrganisaatioTyyppiHierarkia>() {}.getType());
  }

  @Override
  public CompletableFuture<Optional<HakutoimistoDTO>> haeHakutoimisto(String organisaatioId) {
    return this.client
        .getResponse(
            getUrl("organisaatio-service.organisaatio.hakutoimisto", organisaatioId),
            Duration.ofMinutes(1),
            x -> x)
        .thenApply(
            response -> {
              if (response.statusCode() == 404) {
                return Optional.empty();
              }
              return Optional.of(
                  this.client.parseJson(
                      response,
                      new com.google.gson.reflect.TypeToken<HakutoimistoDTO>() {}.getType()));
            });
  }
}
