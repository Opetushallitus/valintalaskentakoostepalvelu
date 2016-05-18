package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import fi.vm.sade.tarjonta.service.resources.v1.dto.*;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultTulos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import rx.Observable;

@Service
public class TarjontaAsyncResourceImpl extends HttpResource implements TarjontaAsyncResource {
    private final static Logger LOG = LoggerFactory.getLogger(TarjontaAsyncResourceImpl.class);
    private static final Gson GSON = DateDeserializer.gsonBuilder()
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

    @Override
    public Gson gson() {
        return GSON;
    }

    @Autowired
    public TarjontaAsyncResourceImpl(@Value("${valintalaskentakoostepalvelu.tarjonta.rest.url}") String address) {
        super(address, TimeUnit.MINUTES.toMillis(5));
    }
    @Override
    public Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationGroupOids(Collection<String> organizationGroupOids) {
        return this.<ResultSearch>getAsObservable("/v1/hakukohde/search", new TypeToken<ResultSearch>() {
        }.getType(), client -> {
            client.query("organisaatioRyhmaOid", organizationGroupOids.toArray());
            return client;
        }).map(ResultSearch::getResult).map(ResultTulos::getTulokset);
    }
    @Override
    public Observable<List<ResultOrganization>> hakukohdeSearchByOrganizationOids(Collection<String> organizationOids) {
        return this.<ResultSearch>getAsObservable("/v1/hakukohde/search", new TypeToken<ResultSearch>() {
        }.getType(), client -> {
            client.query("organisationOid", organizationOids.toArray());
            return client;
        }).map(ResultSearch::getResult).map(ResultTulos::getTulokset);
    }

    @Override
    public Observable<HakuV1RDTO> haeHaku(String hakuOid) {
        return this.<ResultV1RDTO<HakuV1RDTO>>getAsObservable("/v1/haku/" + hakuOid + "/", new TypeToken<ResultV1RDTO<HakuV1RDTO>>() {
        }.getType()).map(result -> result.getResult());
    }

    @Override
    public Observable<HakukohdeV1RDTO> haeHakukohde(String hakukohdeOid) {
        return this.<ResultV1RDTO<HakukohdeV1RDTO>>getAsObservable("/v1/hakukohde/" + hakukohdeOid + "/", new TypeToken<ResultV1RDTO<HakukohdeV1RDTO>>() {
        }.getType()).map(result -> result.getResult());
    }
}
