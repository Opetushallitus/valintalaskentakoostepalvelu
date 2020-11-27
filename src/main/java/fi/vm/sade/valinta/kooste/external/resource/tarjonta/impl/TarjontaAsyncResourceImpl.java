package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ErrorV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.GenericSearchParamsV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeValintaperusteetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KomoV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.koulutus.KoulutusV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Hakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Koulutus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Toteutus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaHaku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaKoulutus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaToteutus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultRyhmaliitos;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class TarjontaAsyncResourceImpl implements TarjontaAsyncResource {
  private final UrlConfiguration urlConfiguration = UrlConfiguration.getInstance();
  private final HttpClient client;
  private final HttpClient koutaClient;

  @Autowired
  public TarjontaAsyncResourceImpl(
      @Qualifier("TarjontaHttpClient") HttpClient client,
      @Qualifier("KoutaHttpClient") HttpClient koutaClient) {
    this.client = client;
    this.koutaClient = koutaClient;
  }

  @Override
  public CompletableFuture<Set<String>> hakukohdeSearchByOrganizationGroupOids(
      Iterable<String> organizationGroupOids) {
    Map<String, String[]> parameters = new HashMap<>();
    parameters.put(
        "organisaatioRyhmaOid",
        StreamSupport.stream(organizationGroupOids.spliterator(), false).toArray(String[]::new));
    return this.client
        .<ResultSearch>getJson(
            urlConfiguration.url("tarjonta-service.hakukohde.search", parameters),
            Duration.ofMinutes(5),
            new TypeToken<ResultSearch>() {}.getType())
        .thenApplyAsync(
            r ->
                r.getResult().getTulokset().stream()
                    .flatMap(t -> t.getTulokset().stream())
                    .map(ResultHakukohde::getOid)
                    .collect(Collectors.toSet()));
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
                          .<Set<KoutaHakukohde>>getJson(
                              urlConfiguration.url(
                                  "kouta-internal.hakukohde.search", koutaParameters),
                              Duration.ofSeconds(10),
                              new TypeToken<Set<KoutaHakukohde>>() {}.getType())
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
    CompletableFuture<KoutaHaku> koutaF =
        this.koutaClient.getJson(
            urlConfiguration.url("kouta-internal.haku.hakuoid", hakuOid),
            Duration.ofSeconds(10),
            new TypeToken<KoutaHaku>() {}.getType());
    return this.getTarjontaHaku(hakuOid)
        .thenComposeAsync(
            r ->
                r == null
                    ? koutaF.thenApplyAsync(Haku::new)
                    : CompletableFuture.completedFuture(new Haku(r)));
  }

  @Override
  public CompletableFuture<Hakukohde> haeHakukohde(String hakukohdeOid) {
    CompletableFuture<HakukohdeV1RDTO> tarjontaF =
        this.client
            .<ResultV1RDTO<HakukohdeV1RDTO>>getJson(
                urlConfiguration.url("tarjonta-service.hakukohde.hakukohdeoid", hakukohdeOid),
                Duration.ofMinutes(5),
                new TypeToken<ResultV1RDTO<HakukohdeV1RDTO>>() {}.getType())
            .thenApplyAsync(ResultV1RDTO::getResult);
    CompletableFuture<KoutaHakukohde> koutaF =
        this.koutaClient.getJson(
            urlConfiguration.url("kouta-internal.hakukohde.hakukohdeoid", hakukohdeOid),
            Duration.ofSeconds(10),
            new TypeToken<KoutaHakukohde>() {}.getType());
    return tarjontaF.thenComposeAsync(
        r ->
            r == null
                ? koutaF.thenApplyAsync(Hakukohde::new)
                : CompletableFuture.completedFuture(new Hakukohde(r)));
  }

  @Override
  public CompletableFuture<Set<String>> haunHakukohteet(String hakuOid) {
    HashMap<String, String> koutaParameters = new HashMap<>();
    koutaParameters.put("haku", hakuOid);
    CompletableFuture<Set<KoutaHakukohde>> koutaF =
        this.koutaClient.getJson(
            urlConfiguration.url("kouta-internal.hakukohde.search", koutaParameters),
            Duration.ofSeconds(10),
            new TypeToken<Set<KoutaHakukohde>>() {}.getType());
    return this.getTarjontaHaku(hakuOid)
        .thenComposeAsync(
            r ->
                r == null
                    ? koutaF.thenApplyAsync(
                        koutaHakukohteet ->
                            koutaHakukohteet.stream().map(h -> h.oid).collect(Collectors.toSet()))
                    : CompletableFuture.completedFuture(new HashSet<>(r.getHakukohdeOids())));
  }

  @Override
  public CompletableFuture<Toteutus> haeToteutus(String toteutusOid) {
    CompletableFuture<KoulutusV1RDTO> tarjontaF =
        this.client
            .<ResultV1RDTO<KoulutusV1RDTO>>getJson(
                urlConfiguration.url("tarjonta-service.koulutus.koulutusoid", toteutusOid),
                Duration.ofMinutes(5),
                new TypeToken<ResultV1RDTO<KoulutusV1RDTO>>() {}.getType())
            .thenApplyAsync(ResultV1RDTO::getResult);
    CompletableFuture<KoutaToteutus> koutaF =
        this.koutaClient.getJson(
            urlConfiguration.url("kouta-internal.toteutus.toteutusoid", toteutusOid),
            Duration.ofMinutes(5),
            new TypeToken<KoutaToteutus>() {}.getType());
    return tarjontaF.thenComposeAsync(
        r ->
            r == null
                ? koutaF.thenApplyAsync(Toteutus::new)
                : CompletableFuture.completedFuture(new Toteutus(r)));
  }

  @Override
  public CompletableFuture<Koulutus> haeKoulutus(String koulutusOid) {
    CompletableFuture<KomoV1RDTO> tarjontaF =
        this.client
            .<ResultV1RDTO<KomoV1RDTO>>getJson(
                urlConfiguration.url("tarjonta-service.komo.komooid", koulutusOid),
                Duration.ofMinutes(5),
                new TypeToken<ResultV1RDTO<KomoV1RDTO>>() {}.getType())
            .thenApplyAsync(ResultV1RDTO::getResult);
    CompletableFuture<KoutaKoulutus> koutaF =
        this.koutaClient.getJson(
            urlConfiguration.url("kouta-internal.koulutus.koulutusoid", koulutusOid),
            Duration.ofMinutes(5),
            new TypeToken<KoutaKoulutus>() {}.getType());
    return tarjontaF.thenComposeAsync(
        r ->
            r == null
                ? koutaF.thenApplyAsync(Koulutus::new)
                : CompletableFuture.completedFuture(new Koulutus(r)));
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
  public CompletableFuture<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes(String hakuOid) {
    Map<String, String> tarjontaParameters = new HashMap<>();
    tarjontaParameters.put("hakuOid", hakuOid);
    CompletableFuture<ResultSearch> tarjontaF =
        this.client.getJson(
            urlConfiguration.url("tarjonta-service.hakukohde.search", tarjontaParameters),
            Duration.ofMinutes(5),
            new TypeToken<ResultSearch>() {}.getType());
    Map<String, String> koutaParameters = new HashMap<>();
    koutaParameters.put("haku", hakuOid);
    CompletableFuture<Set<KoutaHakukohde>> koutaF =
        this.koutaClient.getJson(
            urlConfiguration.url("kouta-internal.hakukohde.search", koutaParameters),
            Duration.ofSeconds(10),
            new TypeToken<Set<KoutaHakukohde>>() {}.getType());
    return tarjontaF.thenComposeAsync(
        r ->
            r.getResult().getTulokset().isEmpty()
                ? koutaF.thenApplyAsync(
                    hakukohteet ->
                        hakukohteet.stream()
                            .collect(Collectors.toMap(hk -> hk.oid, hk -> Collections.emptyList())))
                : CompletableFuture.completedFuture(resultSearchToHakukohdeRyhmaMap(r)));
  }

  @Override
  public CompletableFuture<HakukohdeValintaperusteetV1RDTO> findValintaperusteetByOid(
      String hakukohdeOid) {
    return this.client
        .<ResultV1RDTO<HakukohdeValintaperusteetV1RDTO>>getJson(
            urlConfiguration.url("tarjonta-service.hakukohde.valintaperusteet", hakukohdeOid),
            Duration.ofMinutes(5),
            new TypeToken<ResultV1RDTO<HakukohdeValintaperusteetV1RDTO>>() {}.getType())
        .thenApplyAsync(ResultV1RDTO::getResult);
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
