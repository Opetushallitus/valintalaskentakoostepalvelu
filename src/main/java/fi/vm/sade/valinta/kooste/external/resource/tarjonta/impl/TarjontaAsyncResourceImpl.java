package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;

import fi.vm.sade.tarjonta.service.resources.v1.dto.ErrorV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.GenericSearchParamsV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultRyhmaliitos;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultTulos;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TarjontaAsyncResourceImpl extends UrlConfiguredResource implements TarjontaAsyncResource {
    private final HttpClient client;

    @Autowired
    public TarjontaAsyncResourceImpl(@Qualifier("TarjontaHttpClient") HttpClient client) {
        super(TimeUnit.MINUTES.toMillis(5));
        this.client = client;
    }

    @Override
    protected Gson createGson() {
        return getGson();
    }

    @Override
    public Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationGroupOids(Collection<String> organizationGroupOids) {
        return this.<ResultSearch>getAsObservableLazily(getUrl("tarjonta-service.hakukohde.search"), new TypeToken<ResultSearch>() {
        }.getType(), client -> {
            client.query("organisaatioRyhmaOid", organizationGroupOids.toArray());
            return client;
        }).map(ResultSearch::getResult).map(ResultTulos::getTulokset);
    }
    @Override
    public Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationOids(Collection<String> organizationOids) {
        return this.<ResultSearch>getAsObservableLazily(
                getUrl("tarjonta-service.hakukohde.search"),
                new TypeToken<ResultSearch>() {
        }.getType(), client -> {
            client.query("organisationOid", organizationOids.toArray());
            return client;
        }).map(ResultSearch::getResult).map(ResultTulos::getTulokset);
    }

    @Override
    public CompletableFuture<HakuV1RDTO> haeHaku(String hakuOid) {
        return this.client.<ResultV1RDTO<HakuV1RDTO>>getJson(
                getUrl("tarjonta-service.haku.hakuoid", hakuOid),
                Duration.ofMinutes(5),
                new com.google.gson.reflect.TypeToken<ResultV1RDTO<HakuV1RDTO>>() {}.getType()
        ).thenApply(ResultV1RDTO::getResult);
    }

    @Override
    public CompletableFuture<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid) {
        return this.client.<ResultV1RDTO<HakukohdeV1RDTO>>getJson(
                getUrl("tarjonta-service.hakukohde.hakukohdeoid", hakukohdeOid),
                Duration.ofMinutes(5),
                new com.google.gson.reflect.TypeToken<ResultV1RDTO<HakukohdeV1RDTO>>() {}.getType()
        ).thenApply(ResultV1RDTO::getResult);
    }

    @Override
    public Observable<Set<String>> findHakuOidsForAutosyncTarjonta() {
        return this.<ResultV1RDTO<Set<String>>>getAsObservableLazily(getUrl("tarjonta-service.haku.findoidstosynctarjontafor"), new TypeToken<ResultV1RDTO<Set<String>>>() {
        }.getType()).map(result -> result.getResult());
    }

    @Override
    public Observable<Map<String, List<String>>> hakukohdeRyhmasForHakukohdes(String hakuOid) {
        Observable<ResultSearch> s = this.getAsObservableLazily(
                getUrl("tarjonta-service.hakukohde.search"),
                new TypeToken<ResultSearch>() {
                }.getType(), client -> {
                    client.query("hakuOid", hakuOid);
                    return client;
                });
        return resultSearchToHakukohdeRyhmaMap(s);
    }

    public static Observable<Map<String, List<String>>> resultSearchToHakukohdeRyhmaMap(Observable<ResultSearch> observable) {
        return observable.map(ResultSearch::getResult)
                .map(ResultTulos::getTulokset)
                .flatMap(Observable::fromIterable)
                .map(ResultOrganization::getTulokset)
                .flatMap(Observable::fromIterable)
                .map((ResultHakukohde s) -> new ImmutablePair<>(
                        s.getOid(),
                        getRyhmaList(s)))
                .toMap(Pair::getKey, Pair::getValue).toObservable();
    }

    private static List<String> getRyhmaList (ResultHakukohde hk) {
        if (hk.getRyhmaliitokset () != null)
            return hk.getRyhmaliitokset ().stream ()
                    .map (ResultRyhmaliitos::getRyhmaOid)
                    .collect (Collectors.toList ());
        return Lists.newArrayList ();
    }

    public static Gson getGson() {
        return DateDeserializer.gsonBuilder()
                .registerTypeAdapter(ResultV1RDTO.class, (JsonDeserializer) (json, typeOfT, context) -> {
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