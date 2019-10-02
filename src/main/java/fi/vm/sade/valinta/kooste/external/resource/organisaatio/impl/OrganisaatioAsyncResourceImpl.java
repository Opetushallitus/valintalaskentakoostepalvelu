package fi.vm.sade.valinta.kooste.external.resource.organisaatio.impl;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.organisaatio.resource.dto.HakutoimistoDTO;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import io.reactivex.Observable;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 *         https://${host.virkailija}/organisaatio-service/rest esim
 *         /organisaatio-service/rest/organisaatio/1.2.246.562.10.39218317368
 *         ?noCache=1413976497594
 */
@Service
public class OrganisaatioAsyncResourceImpl extends UrlConfiguredResource implements OrganisaatioAsyncResource {
    private final HttpClient client;

    @Autowired
    public OrganisaatioAsyncResourceImpl(
            @Qualifier("OrganisaatioHttpClient") HttpClient client
    ) {
        super(TimeUnit.MINUTES.toMillis(1));
        this.client = client;
    }

    @Override
    public Observable<Response> haeOrganisaatio(String organisaatioOid) {
        return getAsObservableLazily(getUrl("organisaatio-service.organisaatio", organisaatioOid), webClient -> webClient.accept(MediaType.WILDCARD_TYPE));
    }

    @Override
    public Observable<OrganisaatioTyyppiHierarkia> haeOrganisaationTyyppiHierarkiaSisaltaenLakkautetut(String organisaatioOid) {
        return getOrganisaatioTyyppiHierarkiaObservable(client -> {
            client.accept(MediaType.APPLICATION_JSON_TYPE);
            client.query("oid", organisaatioOid);
            client.query("aktiiviset", true);
            client.query("suunnitellut", false);
            client.query("lakkautetut", true);
            return client;
        });
    }


    private Observable<OrganisaatioTyyppiHierarkia> getOrganisaatioTyyppiHierarkiaObservable(Function<WebClient, WebClient> paramsHeadersAndStuff) {
        return getAsObservableLazily(getUrl("organisaatio-service.organisaatio.hierarkia.tyyppi"),
            new TypeToken<OrganisaatioTyyppiHierarkia>() {}.getType(),
            paramsHeadersAndStuff);
    }

    @Override
    public CompletableFuture<Optional<HakutoimistoDTO>> haeHakutoimisto(String organisaatioId) {
        return this.client.getResponse(
                getUrl("organisaatio-service.organisaatio.hakutoimisto", organisaatioId),
                Duration.ofMinutes(1)
        ).thenApply(response -> {
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            return Optional.of(this.client.parseJson(response, new com.google.gson.reflect.TypeToken<HakutoimistoDTO>() {}.getType()));
        });
    }
}
