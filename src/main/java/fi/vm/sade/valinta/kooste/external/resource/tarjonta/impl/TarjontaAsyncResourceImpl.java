package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import fi.vm.sade.tarjonta.service.resources.v1.dto.*;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultTulos;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import rx.Observable;

@Service
public class TarjontaAsyncResourceImpl extends UrlConfiguredResource implements TarjontaAsyncResource {

    @Autowired
    public TarjontaAsyncResourceImpl(UrlConfiguration urlConfiguration) {
        super(urlConfiguration, TimeUnit.MINUTES.toMillis(5));
    }

    @Override
    protected Gson createGson() {
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

    @Override
    public Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationGroupOids(Collection<String> organizationGroupOids) {
        return this.<ResultSearch>getAsObservable(getUrl("tarjonta-service.hakukohde.search"), new TypeToken<ResultSearch>() {
        }.getType(), client -> {
            client.query("organisaatioRyhmaOid", organizationGroupOids.toArray());
            return client;
        }).map(ResultSearch::getResult).map(ResultTulos::getTulokset);
    }
    @Override
    public Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationOids(Collection<String> organizationOids) {
        return this.<ResultSearch>getAsObservable(
                getUrl("tarjonta-service.hakukohde.search"),
                new TypeToken<ResultSearch>() {
        }.getType(), client -> {
            client.query("organisationOid", organizationOids.toArray());
            return client;
        }).map(ResultSearch::getResult).map(ResultTulos::getTulokset);
    }

    @Override
    public Observable<HakuV1RDTO> haeHaku(String hakuOid) {
        return this.<ResultV1RDTO<HakuV1RDTO>>getAsObservable(
                getUrl("tarjonta-service.haku.hakuoid", hakuOid),
                new TypeToken<ResultV1RDTO<HakuV1RDTO>>() {
        }.getType()).map(result -> result.getResult());
    }

    @Override
    public Observable<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid) {
        return this.<ResultV1RDTO<HakukohdeV1RDTO>>getAsObservable(
                getUrl("tarjonta-service.hakukohde.hakukohdeoid", hakukohdeOid),
                new TypeToken<ResultV1RDTO<HakukohdeV1RDTO>>() {
        }.getType()).map(result -> result.getResult());
    }

    @Override
    public Observable<Set<String>> findHakuOidsForAutosyncTarjonta() {
        return this.<ResultV1RDTO<Set<String>>>getAsObservable(getUrl("tarjonta-service.haku.findoidstosynctarjontafor"), new TypeToken<ResultV1RDTO<Set<String>>>() {
        }.getType()).map(result -> result.getResult());
    }
}
