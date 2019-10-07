package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusOid;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppiBatch;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ListFullSearchDTO;
import fi.vm.sade.valinta.kooste.hakemus.dto.ApplicationOidsAndReason;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ApplicationAsyncResourceImpl extends UrlConfiguredResource implements ApplicationAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    private final HttpClient client;

    @Autowired
    public ApplicationAsyncResourceImpl(
            @Qualifier("HakuAppHttpClient") HttpClient client,
            @Qualifier("HakemusServiceRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
        this.client = client;
    }

    private List<HakemusWrapper> toHakemusWrapper(List<Hakemus> h) {
        return h.stream().map(HakuappHakemusWrapper::new).collect(Collectors.toList());
    }

    @Override
    public Observable<List<HakemusWrapper>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        String url = getUrl("haku-app.applications.syntheticapplication");
        HakemusPrototyyppiBatch hakemusPrototyyppiBatch = new HakemusPrototyyppiBatch(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit);
        Entity<String> entity = Entity.entity(gson().toJson(hakemusPrototyyppiBatch), MediaType.APPLICATION_JSON);
        return this.<String,List<Hakemus>>putAsObservableLazily(url, new GenericType<List<Hakemus>>() {}.getType(), entity, ACCEPT_JSON).map(this::toHakemusWrapper);
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByOid(String hakuOid, String hakukohdeOid) {
        return getApplicationsByOids(hakuOid, Collections.singletonList(hakukohdeOid));
    }

    @Override
    public Observable<Set<String>> getApplicationOids(String hakuOid, String hakukohdeOid) {
        ListFullSearchDTO s = new ListFullSearchDTO(
                "",
                Collections.singletonList(hakukohdeOid),
                Collections.singletonList(hakuOid),
                Collections.emptyList(),
                Collections.singletonList("oid")
        );
        return this.<ListFullSearchDTO, List<HakemusOid>>postAsObservableLazily(
                getUrl("haku-app.applications.listfull"),
                new TypeToken<List<HakemusOid>>() {}.getType(),
                Entity.entity(s, MediaType.APPLICATION_JSON_TYPE)
        ).map(lh -> lh.stream().map(HakemusOid::getOid).collect(Collectors.toSet()));
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids) {
        return this.<List<Hakemus>>getAsObservableLazily(getUrl("haku-app.applications.listfull"), new TypeToken<List<Hakemus>>() {}.getType(), client -> {
            client.query("appState", DEFAULT_STATES.toArray());
            client.query("rows", DEFAULT_ROW_LIMIT).query("asId", hakuOid).query("aoOid", hakukohdeOids);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        }).map(this::toHakemusWrapper);
    }

    @Override
    public CompletableFuture<List<HakemusWrapper>> getApplicationsByOidsWithPOST(String hakuOid, List<String> hakukohdeOids) {
        Map<String, List<String>> requestBody = new HashMap<>();
        requestBody.put("states", DEFAULT_STATES);
        requestBody.put("asIds", Collections.singletonList(hakuOid));
        requestBody.put("aoOids", hakukohdeOids);
        requestBody.put("keys", ApplicationAsyncResource.DEFAULT_KEYS);
        return this.client.<Map<String, List<String>>, List<Hakemus>>postJson(
                getUrl("haku-app.applications.listfull"),
                Duration.ofHours(1),
                requestBody,
                new com.google.gson.reflect.TypeToken<Map<String, List<String>>>() {}.getType(),
                new com.google.gson.reflect.TypeToken<List<Hakemus>>() {}.getType()
        ).thenApplyAsync(this::toHakemusWrapper);
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByHakemusOids(List<String> hakemusOids) {
        return Observable.fromFuture(getApplicationsByHakemusOids(null, hakemusOids, Collections.emptyList()));
    }

    private CompletableFuture<List<HakemusWrapper>> getApplicationsByHakemusOids(String hakuOid, List<String> hakemusOids, List<String> keys) {
        HashMap<String, Object> query = new HashMap<>();
        query.put("rows", DEFAULT_ROW_LIMIT);
        if (!keys.isEmpty()) {
            query.put("asIds", hakuOid);
            query.put("state", DEFAULT_STATES);
            query.put("keys", keys);
        }
        return this.client.<List<String>, List<Hakemus>>postJson(
                getUrl("haku-app.applications.list", query),
                Duration.ofHours(1),
                hakemusOids,
                new com.google.gson.reflect.TypeToken<List<String>>() {}.getType(),
                new com.google.gson.reflect.TypeToken<List<Hakemus>>() {}.getType()
        ).thenApplyAsync(hs -> hs.stream()
                .filter(h -> StringUtils.isEmpty(h.getState()) || DEFAULT_STATES.contains(h.getState()))
                .map(HakuappHakemusWrapper::new)
                .collect(Collectors.toList()));
    }

    @Override
    public CompletableFuture<List<HakemusWrapper>> getApplicationsByhakemusOidsInParts(String hakuOid, List<String> hakemusOids, List<String> keys) {
        List<CompletableFuture<List<HakemusWrapper>>> fs = Lists.partition(hakemusOids, DEFAULT_ROW_LIMIT).stream()
                .map(oids -> getApplicationsByHakemusOids(hakuOid, oids, keys))
                .collect(Collectors.toList());
        return CompletableFuture.allOf(fs.toArray(new CompletableFuture[0]))
                .thenApplyAsync(v -> fs.stream().flatMap(f -> f.join().stream()).collect(Collectors.toList()));
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByOids(Collection<String> hakemusOids) {
        return this.<List<String>, List<Hakemus>>postAsObservableLazily(getUrl("haku-app.applications.list"),
            new GenericType<List<Hakemus>>() {}.getType(),
            Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE),
            webClient -> webClient.query("rows", DEFAULT_ROW_LIMIT).accept(MediaType.APPLICATION_JSON_TYPE)).map(this::toHakemusWrapper);
    }

    @Override
    public Observable<HakemusWrapper> getApplication(String hakemusOid) {
        return this.<Hakemus>getAsObservableLazily(getUrl("haku-app.applications", hakemusOid), Hakemus.class).map(HakuappHakemusWrapper::new);
    }

    @Override
    public Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOids, String reason) {
        return postAsObservableLazily(getUrl("haku-app.applications.state.passivate"), Entity.json(new ApplicationOidsAndReason(hakemusOids, reason)));
    }
}
