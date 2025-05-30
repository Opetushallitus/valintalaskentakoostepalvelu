package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ErrorV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.GenericSearchParamsV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KomoV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KoulutusV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.kouta.dto.HakukohderyhmaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.kouta.dto.KoutaHakukohdeDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.*;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.CompletableFutureUtil;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class TarjontaAsyncResourceImpl implements TarjontaAsyncResource {
  private final UrlConfiguration urlConfiguration = UrlConfiguration.getInstance();
  private final HttpClient client;
  private final RestCasClient koutaClient;
  private final RestCasClient hakukohderyhmapalveluClient;
  private final OhjausparametritAsyncResource ohjausparametritAsyncResource;
  private final Integer KOUTA_OID_LENGTH = 35;

  private static final Logger LOG = LoggerFactory.getLogger(TarjontaAsyncResourceImpl.class);

  private final Cache<String, CompletableFuture<List<String>>> hakukohderyhmanHakukohteetCache =
      CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();

  private final Cache<String, CompletableFuture<Map<String, List<String>>>>
      haunHakukohteidenHakukohderyhmatCache =
          CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();

  @Autowired
  public TarjontaAsyncResourceImpl(
      @Qualifier("TarjontaHttpClient") HttpClient client,
      @Qualifier("KoutaCasClient") RestCasClient koutaClient,
      @Qualifier("HakukohderyhmapalveluCasClient") RestCasClient hakukohderyhmapalveluClient,
      @Qualifier("OhjausparametritAsyncResource")
          OhjausparametritAsyncResource ohjausparametritAsyncResource) {
    this.client = client;
    this.koutaClient = koutaClient;
    this.hakukohderyhmapalveluClient = hakukohderyhmapalveluClient;
    this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
  }

  @Override
  public CompletableFuture<Set<String>> hakukohdeSearchByOrganizationGroupOids(
      Iterable<String> organizationGroupOids) {

    Map<String, String[]> tarjontaParams = new HashMap<>();
    tarjontaParams.put(
        "organisaatioRyhmaOid",
        StreamSupport.stream(organizationGroupOids.spliterator(), false).toArray(String[]::new));
    CompletableFuture<Set<String>> fromTarjonta =
        this.client
            .<ResultSearch>getJson(
                urlConfiguration.url("tarjonta-service.hakukohde.search", tarjontaParams),
                Duration.ofMinutes(5),
                new TypeToken<ResultSearch>() {}.getType())
            .thenApplyAsync(
                r ->
                    r.getResult().getTulokset().stream()
                        .flatMap(t -> t.getTulokset().stream())
                        .map(ResultHakukohde::getOid)
                        .collect(Collectors.toSet()));

    CompletableFuture<List<List<String>>> fromHakukohderyhmapalvelu =
        CompletableFutureUtil.sequence(
            StreamSupport.stream(organizationGroupOids.spliterator(), false)
                .map(this::hakukohderyhmanHakukohteet)
                .collect(Collectors.toList()));

    return fromTarjonta.thenCombine(
        fromHakukohderyhmapalvelu,
        (t, h) -> {
          Set<String> result = new HashSet<>(t);
          for (List<String> oids : h) {
            result.addAll(oids);
          }
          return result;
        });
  }

  private CompletableFuture<List<String>> hakukohderyhmanHakukohteet(String hakukohderyhmaOid) {
    try {
      return hakukohderyhmanHakukohteetCache.get(
          hakukohderyhmaOid,
          () ->
              this.hakukohderyhmapalveluClient.get(
                  urlConfiguration.url(
                      "hakukohderyhmapalvelu.hakukohderyhman-hakukohteet", hakukohderyhmaOid),
                  new TypeToken<>() {},
                  Collections.emptyMap(),
                  60 * 1000));
    } catch (ExecutionException e) {
      LOG.error(
          "Hakukohderyhmän {} hakukohteiden haku epäonnistui: {}",
          hakukohderyhmaOid,
          e.getMessage());
      return CompletableFuture.failedFuture(e);
    }
  }

  @Override
  public CompletableFuture<Set<String>> hakukohdeSearchByOrganizationOids(
      Iterable<String> organizationOids) {
    Map<String, String[]> tarjontaParameters = new HashMap<>();
    tarjontaParameters.put(
        "organisationOid",
        StreamSupport.stream(organizationOids.spliterator(), false).toArray(String[]::new));
    CompletableFuture<Set<String>> tarjontaF =
        this.client
            .<ResultSearch>getJson(
                urlConfiguration.url("tarjonta-service.hakukohde.search", tarjontaParameters),
                Duration.ofMinutes(5),
                new TypeToken<ResultSearch>() {}.getType())
            .thenApplyAsync(
                r ->
                    r.getResult().getTulokset().stream()
                        .flatMap(t -> t.getTulokset().stream())
                        .map(ResultHakukohde::getOid)
                        .collect(Collectors.toSet()));
    CompletableFuture<List<Set<String>>> koutaF =
        CompletableFutureUtil.sequence(
            StreamSupport.stream(organizationOids.spliterator(), false)
                .map(
                    organizationOid -> {
                      Map<String, String> koutaParameters = new HashMap<>();
                      koutaParameters.put("tarjoaja", organizationOid);
                      return this.koutaClient
                          .get(
                              urlConfiguration.url(
                                  "kouta-internal.hakukohde.search", koutaParameters),
                              new TypeToken<Set<KoutaHakukohdeDTO>>() {},
                              Collections.emptyMap(),
                              10 * 1000)
                          .thenApplyAsync(
                              hakukohteet ->
                                  hakukohteet.stream().map(h -> h.oid).collect(Collectors.toSet()));
                    })
                .collect(Collectors.toList()));
    return tarjontaF.thenComposeAsync(
        tarjontaHakukohdeOids ->
            koutaF.thenApplyAsync(
                koutaHakukohdeOids -> {
                  HashSet<String> s = new HashSet<>(tarjontaHakukohdeOids);
                  for (Set<String> oids : koutaHakukohdeOids) {
                    s.addAll(oids);
                  }
                  return s;
                }));
  }

  private CompletableFuture<HakuV1RDTO> getTarjontaHaku(String hakuOid) {
    return this.client
        .<ResultV1RDTO<HakuV1RDTO>>getJson(
            urlConfiguration.url("tarjonta-service.haku.hakuoid", hakuOid),
            Duration.ofMinutes(5),
            new com.google.gson.reflect.TypeToken<ResultV1RDTO<HakuV1RDTO>>() {}.getType())
        .thenApplyAsync(ResultV1RDTO::getResult);
  }

  @Override
  public CompletableFuture<Haku> haeHaku(String hakuOid) {
    if (KOUTA_OID_LENGTH.equals(hakuOid.length())) {
      CompletableFuture<ParametritDTO> parametritF =
          ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid);
      CompletableFuture<KoutaHaku> koutaF =
          this.koutaClient.get(
              urlConfiguration.url("kouta-internal.haku.hakuoid", hakuOid),
              new TypeToken<KoutaHaku>() {},
              Collections.emptyMap(),
              10 * 1000);
      return koutaF
          .thenApplyAsync(Haku::new)
          .thenCombineAsync(
              parametritF,
              (haku, parametrit) ->
                  haku.withSynteettisetHakemukset(parametrit.getSynteettisetHakemukset()));
    } else {
      return this.getTarjontaHaku(hakuOid).thenApplyAsync(Haku::new);
    }
  }

  @Override
  public CompletableFuture<AbstractHakukohde> haeHakukohde(String hakukohdeOid) {
    if (KOUTA_OID_LENGTH.equals(hakukohdeOid.length())) {
      CompletableFuture<KoutaHakukohdeDTO> koutaF =
          this.koutaClient.get(
              urlConfiguration.url("kouta-internal.hakukohde.hakukohdeoid", hakukohdeOid),
              new TypeToken<KoutaHakukohdeDTO>() {},
              Collections.emptyMap(),
              10 * 1000);
      return koutaF.thenApplyAsync(KoutaHakukohde::new);
    } else {
      CompletableFuture<HakukohdeV1RDTO> tarjontaF =
          this.client
              .<ResultV1RDTO<HakukohdeV1RDTO>>getJson(
                  urlConfiguration.url("tarjonta-service.hakukohde.hakukohdeoid", hakukohdeOid),
                  Duration.ofMinutes(5),
                  new TypeToken<ResultV1RDTO<HakukohdeV1RDTO>>() {}.getType())
              .thenApplyAsync(ResultV1RDTO::getResult);
      return tarjontaF.thenApplyAsync(TarjontaHakukohde::new);
    }
  }

  @Override
  public CompletableFuture<Set<String>> haunHakukohteet(String hakuOid) {
    if (KOUTA_OID_LENGTH.equals(hakuOid.length())) {
      HashMap<String, String> koutaParameters = new HashMap<>();
      koutaParameters.put("haku", hakuOid);
      CompletableFuture<Set<KoutaHakukohde>> koutaF =
          this.koutaClient.get(
              urlConfiguration.url("kouta-internal.hakukohde.search", koutaParameters),
              new TypeToken<Set<KoutaHakukohde>>() {},
              Collections.emptyMap(),
              10 * 1000);
      return koutaF.thenApplyAsync(
          koutaResult ->
              koutaResult.stream().map(hakukohde -> hakukohde.oid).collect(Collectors.toSet()));
    } else {
      return this.getTarjontaHaku(hakuOid).thenApplyAsync(h -> new HashSet<>(h.getHakukohdeOids()));
    }
  }

  @Override
  public CompletableFuture<Toteutus> haeToteutus(String toteutusOid) {
    if (KOUTA_OID_LENGTH.equals(toteutusOid.length())) {
      CompletableFuture<KoutaToteutus> koutaF =
          this.koutaClient.get(
              urlConfiguration.url("kouta-internal.toteutus.toteutusoid", toteutusOid),
              new TypeToken<KoutaToteutus>() {},
              Collections.emptyMap(),
              5 * 60 * 1000);
      return koutaF.thenApplyAsync(Toteutus::new);
    } else {
      CompletableFuture<KoulutusV1RDTO> tarjontaF =
          this.client
              .<ResultV1RDTO<KoulutusV1RDTO>>getJson(
                  urlConfiguration.url("tarjonta-service.koulutus.koulutusoid", toteutusOid),
                  Duration.ofMinutes(5),
                  new TypeToken<ResultV1RDTO<KoulutusV1RDTO>>() {}.getType())
              .thenApplyAsync(ResultV1RDTO::getResult);
      return tarjontaF.thenApplyAsync(Toteutus::new);
    }
  }

  @Override
  public CompletableFuture<Koulutus> haeKoulutus(String koulutusOid) {
    if (KOUTA_OID_LENGTH.equals(koulutusOid.length())) {
      CompletableFuture<KoutaKoulutus> koutaF =
          this.koutaClient.get(
              urlConfiguration.url("kouta-internal.koulutus.koulutusoid", koulutusOid),
              new TypeToken<KoutaKoulutus>() {},
              Collections.emptyMap(),
              5 * 60 * 1000);
      return koutaF.thenApplyAsync(Koulutus::new);
    } else {
      CompletableFuture<KomoV1RDTO> tarjontaF =
          this.client
              .<ResultV1RDTO<KomoV1RDTO>>getJson(
                  urlConfiguration.url("tarjonta-service.komo.komooid", koulutusOid),
                  Duration.ofMinutes(5),
                  new TypeToken<ResultV1RDTO<KomoV1RDTO>>() {}.getType())
              .thenApplyAsync(ResultV1RDTO::getResult);
      return tarjontaF.thenApplyAsync(Koulutus::new);
    }
  }

  @Override
  public CompletableFuture<Set<String>> findHakuOidsForAutosyncTarjonta() {
    return this.client
        .<ResultV1RDTO<Set<String>>>getJson(
            urlConfiguration.url("tarjonta-service.haku.findoidstosynctarjontafor"),
            Duration.ofMinutes(5),
            new TypeToken<ResultV1RDTO<Set<String>>>() {}.getType())
        .thenApplyAsync(r -> r.getResult() == null ? new HashSet<>() : r.getResult());
  }

  @Override
  public CompletableFuture<Map<String, List<String>>> haunHakukohderyhmatCached(String hakuOid) {
    try {
      return haunHakukohteidenHakukohderyhmatCache.get(
          hakuOid, () -> hakukohdeRyhmasForHakukohdes(hakuOid));
    } catch (ExecutionException e) {
      LOG.error(
          "Haun {} hakukohteiden hakukohderyhmien haku epäonnistui: {}", hakuOid, e.getMessage());
      return CompletableFuture.failedFuture(e);
    }
  }

  @Override
  public CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes(String hakuOid) {
    if (KOUTA_OID_LENGTH.equals(hakuOid.length())) {
      CompletableFuture<Set<KoutaHakukohde>> koutaF = this.findKoutaHakukohteetForHaku(hakuOid);
      return koutaF.thenComposeAsync(this::findHakukohderyhmasForHakukohteet);
    } else {
      CompletableFuture<ResultSearch> tarjontaF = this.findTarjontaHakukohteetForHaku(hakuOid);
      return tarjontaF.thenApplyAsync(TarjontaAsyncResourceImpl::resultSearchToHakukohdeRyhmaMap);
    }
  }

  @Override
  public CompletableFuture<List<String>> hakukohdeRyhmasForHakukohde(String hakukohdeOid) {
    if (KOUTA_OID_LENGTH.equals(hakukohdeOid.length())) {
      return findHakukohderyhmasForHakukohde(hakukohdeOid);
    } else {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
  }

  @Override
  public CompletableFuture<HakukohdeValintaperusteetDTO> findValintaperusteetByOid(
      String hakukohdeOid) {
    return this.client
        .<ResultV1RDTO<HakukohdeValintaperusteetDTO>>getJson(
            urlConfiguration.url("tarjonta-service.hakukohde.valintaperusteet", hakukohdeOid),
            Duration.ofMinutes(5),
            new TypeToken<ResultV1RDTO<HakukohdeValintaperusteetDTO>>() {}.getType())
        .thenApplyAsync(ResultV1RDTO::getResult);
  }

  private CompletableFuture<ResultSearch> findTarjontaHakukohteetForHaku(String hakuOid) {
    Map<String, String> tarjontaParameters = new HashMap<>();
    tarjontaParameters.put("hakuOid", hakuOid);
    return this.client.getJson(
        urlConfiguration.url("tarjonta-service.hakukohde.search", tarjontaParameters),
        Duration.ofMinutes(5),
        new TypeToken<ResultSearch>() {}.getType());
  }

  private CompletableFuture<Set<KoutaHakukohde>> findKoutaHakukohteetForHaku(String hakuOid) {
    Map<String, String> koutaParameters = new HashMap<>();
    koutaParameters.put("haku", hakuOid);
    return this.koutaClient.get(
        urlConfiguration.url("kouta-internal.hakukohde.search", koutaParameters),
        new TypeToken<Set<KoutaHakukohde>>() {},
        Collections.emptyMap(),
        60 * 1000);
  }

  private CompletableFuture<Map<String, List<String>>> findHakukohderyhmasForHakukohteet(
      Set<KoutaHakukohde> hakukohdes) {
    List<String> hakukohdeOids = hakukohdes.stream().map(hk -> hk.oid).collect(Collectors.toList());
    Type inputType = new TypeToken<List<String>>() {}.getType();
    TypeToken outputType = new TypeToken<List<HakukohderyhmaHakukohde>>() {};

    return ((CompletableFuture<List<HakukohderyhmaHakukohde>>)
            this.hakukohderyhmapalveluClient.post(
                urlConfiguration.url("hakukohderyhmapalvelu.hakukohderyhma.search-by-hakukohteet"),
                outputType,
                hakukohdeOids,
                Collections.emptyMap(),
                60 * 1000))
        .thenApplyAsync(
            r ->
                r.stream()
                    .collect(
                        Collectors.toMap(
                            HakukohderyhmaHakukohde::getHakukohdeOid,
                            HakukohderyhmaHakukohde::getHakukohderyhmat)));
  }

  private CompletableFuture<List<String>> findHakukohderyhmasForHakukohde(String hakukohdeOid) {
    return this.hakukohderyhmapalveluClient.get(
        urlConfiguration.url("hakukohderyhmapalvelu.hakukohteen-hakukohderyhmat", hakukohdeOid),
        new TypeToken<>() {},
        Collections.emptyMap(),
        60 * 1000);
  }

  public static Map<String, List<String>> resultSearchToHakukohdeRyhmaMap(ResultSearch result) {
    return result.getResult().getTulokset().stream()
        .flatMap(t -> t.getTulokset().stream())
        .collect(
            Collectors.toMap(
                ResultHakukohde::getOid,
                hk ->
                    hk.getRyhmaliitokset() == null
                        ? Collections.emptyList()
                        : hk.getRyhmaliitokset().stream()
                            .map(ResultRyhmaliitos::getRyhmaOid)
                            .collect(Collectors.toList())));
  }

  public static Gson getGson() {
    return DateDeserializer.gsonBuilder()
        .registerTypeAdapter(
            KoulutusV1RDTO.class,
            (JsonDeserializer<KoulutusV1RDTO>)
                (json, typeOfT, context) -> {
                  JsonObject o = json.getAsJsonObject();
                  String toteutustyyppi = o.getAsJsonPrimitive("toteutustyyppi").getAsString();
                  for (JsonSubTypes.Type type :
                      KoulutusV1RDTO.class.getAnnotation(JsonSubTypes.class).value()) {
                    if (type.name().equals(toteutustyyppi)) {
                      return context.deserialize(o, type.value());
                    }
                  }
                  throw new IllegalStateException(
                      String.format(
                          "Tyyppiä %s olevan koulutuksen jäsentäminen epäonnistui",
                          toteutustyyppi));
                })
        .registerTypeAdapter(
            ResultV1RDTO.class,
            (JsonDeserializer)
                (json, typeOfT, context) -> {
                  Type accessRightsType = new TypeToken<Map<String, Boolean>>() {}.getType();
                  Type errorsType = new TypeToken<List<ErrorV1RDTO>>() {}.getType();
                  Type paramsType = new TypeToken<GenericSearchParamsV1RDTO>() {}.getType();
                  Type resultType = ((ParameterizedType) typeOfT).getActualTypeArguments()[0];
                  Type statusType = new TypeToken<ResultV1RDTO.ResultStatus>() {}.getType();
                  JsonObject o = json.getAsJsonObject();
                  ResultV1RDTO r = new ResultV1RDTO();
                  r.setAccessRights(context.deserialize(o.get("accessRights"), accessRightsType));
                  r.setErrors(context.deserialize(o.get("errors"), errorsType));
                  r.setParams(context.deserialize(o.get("params"), paramsType));
                  r.setResult(context.deserialize(o.get("result"), resultType));
                  r.setStatus(context.deserialize(o.get("status"), statusType));
                  return r;
                })
        .create();
  }
}
