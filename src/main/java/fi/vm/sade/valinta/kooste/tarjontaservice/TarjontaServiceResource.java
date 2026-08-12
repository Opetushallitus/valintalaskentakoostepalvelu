package fi.vm.sade.valinta.kooste.tarjontaservice;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.tarjonta.service.resources.v1.dto.*;
import fi.vm.sade.tarjonta.service.types.KoulutusasteTyyppi;
import fi.vm.sade.tarjonta.service.types.KoulutusmoduuliTyyppi;
import fi.vm.sade.tarjonta.shared.types.ToteutustyyppiEnum;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController("TarjontaServiceResource")
@RequestMapping("/resources/tarjonta-service")
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

  @GetMapping(
      value = "/rest/v1/hakukohde/{hakukohdeOid}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Palauttaa hakukohteen tiedot")
  public CompletableFuture<ResultV1RDTO<HakukohdeV1RDTO>> getHakukohde(
      @PathVariable(name = "hakukohdeOid") String hakukohdeOid) {
    return this.tarjontaClient.<ResultV1RDTO<HakukohdeV1RDTO>>getJson(
        urlConfiguration.url("tarjonta-service.hakukohde.hakukohdeoid", hakukohdeOid),
        Duration.ofMinutes(5),
        new TypeToken<ResultV1RDTO<HakukohdeV1RDTO>>() {}.getType());
  }

  @GetMapping(value = "/rest/v1/haku/{hakuOid}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Palauttaa haun tiedot")
  public CompletableFuture<ResultV1RDTO<HakuV1RDTO>> getHaku(
      @PathVariable(name = "hakuOid") String hakuOid) {
    return this.tarjontaClient.<ResultV1RDTO<HakuV1RDTO>>getJson(
        urlConfiguration.url("tarjonta-service.haku.hakuoid", hakuOid),
        Duration.ofMinutes(5),
        new com.google.gson.reflect.TypeToken<ResultV1RDTO<HakuV1RDTO>>() {}.getType());
  }

  @GetMapping(
      value = "/rest/v1/haku/{hakuOid}/hakukohdeTulos",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Palauttaa haun hakukohde tulokset")
  public CompletableFuture<HakukohdeTulosV1RDTO> getHakuHakukohdeTulos(
      @PathVariable(name = "hakuOid") String hakuOid,
      @RequestParam(value = "searchTerms", required = false) String searchTerms,
      @RequestParam(value = "count", required = false) Integer count,
      @RequestParam(value = "startIndex", required = false) Integer startIndex,
      @RequestParam(value = "lastModifiedBefore", required = false) Date lastModifiedBefore,
      @RequestParam(value = "lastModifiedSince", required = false) Date lastModifiedSince,
      @RequestParam(value = "organisationOids", required = false) String organisationOidsStr,
      @RequestParam(value = "organisationGroupOids", required = false)
          String organisationGroupOidsStr,
      @RequestParam(value = "hakukohdeTilas", required = false) String hakukohdeTilasStr,
      @RequestParam(value = "alkamisVuosi", required = false) Integer alkamisVuosi,
      @RequestParam(value = "alkamisKausi", required = false) String alkamisKausi) {
    String queryString =
        UriComponentsBuilder.newInstance()
            .queryParamIfPresent("searchTerms", Optional.ofNullable(searchTerms))
            .queryParamIfPresent("count", Optional.ofNullable(count))
            .queryParamIfPresent("startIndex", Optional.ofNullable(startIndex))
            .queryParamIfPresent("lastModifiedBefore", Optional.ofNullable(lastModifiedBefore))
            .queryParamIfPresent("lastModifiedSince", Optional.ofNullable(lastModifiedSince))
            .queryParamIfPresent("organisationOids", Optional.ofNullable(organisationOidsStr))
            .queryParamIfPresent(
                "organisationGroupOids", Optional.ofNullable(organisationGroupOidsStr))
            .queryParamIfPresent("hakukohdeTilas", Optional.ofNullable(hakukohdeTilasStr))
            .queryParamIfPresent("alkamisVuosi", Optional.ofNullable(alkamisVuosi))
            .queryParamIfPresent("alkamisKausi", Optional.ofNullable(alkamisKausi))
            .build()
            .toUri()
            .getQuery();

    return this.tarjontaClient.<HakukohdeTulosV1RDTO>getJson(
        urlConfiguration.url("tarjonta-service.haku.hakukohdetulos", hakuOid) + "?" + queryString,
        Duration.ofMinutes(5),
        new com.google.gson.reflect.TypeToken<HakukohdeTulosV1RDTO>() {}.getType());
  }

  @GetMapping(value = "/rest/v1/haku/find", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Palauttaa kaikki hakukriteerejä vastaavat haut")
  public CompletableFuture<ResultV1RDTO<List<HakuV1RDTO>>> find(
      @RequestParam(value = "addHakukohdes", required = false) Boolean addHakukohdes,
      @RequestParam(value = "virkailijaTyyppi", required = false) String virkailijaTyyppi,
      @RequestParam(value = "cache", required = false) Boolean cache,
      @RequestParam(value = "modifiedBeforeAsDate", required = false) String modifiedBeforeAsDate,
      @RequestParam(value = "modifiedAfterAsDate", required = false) String modifiedAfterAsDate,
      @RequestParam(value = "startIndex", required = false) Integer startIndex,
      @RequestParam(value = "modifiedBefore", required = false) Integer modifiedBefore,
      @RequestParam(value = "modifiedAfter", required = false) Integer modifiedAfter,
      @RequestParam(value = "count", required = false) Integer count) {
    String queryString =
        UriComponentsBuilder.newInstance()
            .queryParamIfPresent("addHakukohdes", Optional.ofNullable(addHakukohdes))
            .queryParamIfPresent("virkailijaTyyppi", Optional.ofNullable(virkailijaTyyppi))
            .queryParamIfPresent("cache", Optional.ofNullable(cache))
            .queryParamIfPresent("modifiedBeforeAsDate", Optional.ofNullable(modifiedBeforeAsDate))
            .queryParamIfPresent("modifiedAfterAsDate", Optional.ofNullable(modifiedAfterAsDate))
            .queryParamIfPresent("startIndex", Optional.ofNullable(startIndex))
            .queryParamIfPresent("modifiedBefore", Optional.ofNullable(modifiedBefore))
            .queryParamIfPresent("modifiedAfter", Optional.ofNullable(modifiedAfter))
            .queryParamIfPresent("count", Optional.ofNullable(count))
            .build()
            .toUri()
            .getQuery();

    return this.tarjontaClient.<ResultV1RDTO<List<HakuV1RDTO>>>getJson(
        urlConfiguration.url("tarjonta-service.haku.find") + "?" + queryString,
        Duration.ofMinutes(5),
        new com.google.gson.reflect.TypeToken<ResultV1RDTO<List<HakuV1RDTO>>>() {}.getType());
  }

  @GetMapping(
      value = "/rest/v1/hakukohde/search",
      produces = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
  @Operation(
      summary = "Palauttaa listan hakukohteista annetuilla parametreilla",
      description = "Palauttaa listan hakukohteista annetuilla parametreilla.")
  public CompletableFuture<String> search(
      @RequestParam(value = "searchTerms", required = false) String searchTerms,
      @RequestParam(value = "hakukohteenNimiUri", required = false) String hakukohteenNimiUri,
      @RequestParam(value = "organisationOid", required = false) List<String> organisationOids,
      @RequestParam(value = "tila", required = false) List<String> hakukohdeTilas,
      @RequestParam(value = "alkamisKausi", required = false) String alkamisKausi,
      @RequestParam(value = "alkamisVuosi", required = false) Integer alkamisVuosi,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      @RequestParam(value = "koulutusastetyyppi", required = false)
          List<KoulutusasteTyyppi> koulutusastetyyppi,
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "organisaatioRyhmaOid", required = false)
          List<String> organisaatioRyhmaOid,
      @RequestParam(value = "koulutustyyppi", required = false)
          List<ToteutustyyppiEnum> koulutustyypit,
      @RequestParam(value = "koulutusmoduuliTyyppi", required = false)
          List<KoulutusmoduuliTyyppi> koulutusmoduulityypit,
      @RequestParam(value = "defaultTarjoaja", required = false) String defaultTarjoaja,
      @RequestParam(value = "hakutapa", required = false) String hakutapa,
      @RequestParam(value = "hakutyyppi", required = false) String hakutyyppi,
      @RequestParam(value = "koulutuslaji", required = false) String koulutuslaji,
      @RequestParam(value = "kohdejoukko", required = false) String kohdejoukko,
      @RequestParam(value = "oppilaitostyyppi", required = false) String oppilaitostyyppi,
      @RequestParam(value = "kunta", required = false) String kunta,
      @RequestParam(value = "opetuskielet", required = false) List<String> opetuskielet,
      @RequestParam(value = "koulutusOid", required = false) List<String> koulutusOids,
      @RequestParam(value = "offset", required = false) Integer offset,
      @RequestParam(value = "limit", required = false) Integer limit) {

    String queryString =
        UriComponentsBuilder.newInstance()
            .queryParamIfPresent("searchTerms", Optional.ofNullable(searchTerms))
            .queryParamIfPresent("hakukohteenNimiUri", Optional.ofNullable(hakukohteenNimiUri))
            .queryParamIfPresent("organisationOid", Optional.ofNullable(organisationOids))
            .queryParamIfPresent("tila", Optional.ofNullable(hakukohdeTilas))
            .queryParamIfPresent("alkamisKausi", Optional.ofNullable(alkamisKausi))
            .queryParamIfPresent("alkamisVuosi", Optional.ofNullable(alkamisVuosi))
            .queryParamIfPresent("hakukohdeOid", Optional.ofNullable(hakukohdeOid))
            .queryParamIfPresent("koulutusastetyyppi", Optional.ofNullable(koulutusastetyyppi))
            .queryParamIfPresent("hakuOid", Optional.ofNullable(hakuOid))
            .queryParamIfPresent("organisaatioRyhmaOid", Optional.ofNullable(organisaatioRyhmaOid))
            .queryParamIfPresent("koulutustyyppi", Optional.ofNullable(koulutustyypit))
            .queryParamIfPresent(
                "koulutusmoduuliTyyppi", Optional.ofNullable(koulutusmoduulityypit))
            .queryParamIfPresent("defaultTarjoaja", Optional.ofNullable(defaultTarjoaja))
            .queryParamIfPresent("hakutapa", Optional.ofNullable(hakutapa))
            .queryParamIfPresent("hakutyyppi", Optional.ofNullable(hakutyyppi))
            .queryParamIfPresent("koulutuslaji", Optional.ofNullable(koulutuslaji))
            .queryParamIfPresent("kohdejoukko", Optional.ofNullable(kohdejoukko))
            .queryParamIfPresent("oppilaitostyyppi", Optional.ofNullable(oppilaitostyyppi))
            .queryParamIfPresent("kunta", Optional.ofNullable(kunta))
            .queryParamIfPresent("opetuskielet", Optional.ofNullable(opetuskielet))
            .queryParamIfPresent("koulutusOid", Optional.ofNullable(koulutusOids))
            .queryParamIfPresent("offset", Optional.ofNullable(offset))
            .queryParamIfPresent("limit", Optional.ofNullable(limit))
            .build()
            .toUri()
            .getQuery();

    return this.tarjontaClient.getString(
        urlConfiguration.url("tarjonta-service.hakukohde.search") + "?" + queryString,
        Duration.ofMinutes(5));
  }
}
