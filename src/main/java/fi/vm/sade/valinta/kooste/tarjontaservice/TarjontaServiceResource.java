package fi.vm.sade.valinta.kooste.tarjontaservice;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@RestController("TarjontaServiceResource")
@RequestMapping("/resources/tarjonta-service")
@PreAuthorize("isAuthenticated()")
@Tag(
        name = "/tarjonta-service",
        description = "Proxy-rajapinnat vanhan tarjonnan tietojen hakemiseen")
public class TarjontaServiceResource {

    private static final Logger LOG = LoggerFactory.getLogger(TarjontaServiceResource.class);

    private final UrlConfiguration urlConfiguration = UrlConfiguration.getInstance();
    private final HttpClient tarjontaClient;

    @Autowired
    public TarjontaServiceResource(@Qualifier("TarjontaHttpClient") HttpClient tarjontaClient) {
        this.tarjontaClient = tarjontaClient;
    }

    @GetMapping(value = "/rest/v1/hakukohde/{hakukohdeOid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Palauttaa hakukohteen tiedot")
    public CompletableFuture<HakukohdeV1RDTO> getHakukohde(@PathVariable(name = "hakukohdeOid") String hakukohdeOid) {
        return this.tarjontaClient
                        .<ResultV1RDTO<HakukohdeV1RDTO>>getJson(
                                urlConfiguration.url("tarjonta-service.hakukohde.hakukohdeoid", hakukohdeOid),
                                Duration.ofMinutes(5),
                                new TypeToken<ResultV1RDTO<HakukohdeV1RDTO>>() {}.getType())
                        .thenApplyAsync(ResultV1RDTO::getResult);
    }

    @GetMapping(value = "/rest/v1/haku/{hakuOid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Palauttaa haun tiedot")
    public CompletableFuture<HakuV1RDTO> getHaku(@PathVariable(name = "hakuOid") String hakuOid) {
        return this.tarjontaClient
                .<ResultV1RDTO<HakuV1RDTO>>getJson(
                        urlConfiguration.url("tarjonta-service.haku.hakuoid", hakuOid),
                        Duration.ofMinutes(5),
                        new com.google.gson.reflect.TypeToken<ResultV1RDTO<HakuV1RDTO>>() {}.getType())
                .thenApplyAsync(ResultV1RDTO::getResult);
    }

}
