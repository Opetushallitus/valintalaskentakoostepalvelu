package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ApplicationAsyncResourceImpl extends UrlConfiguredResource implements ApplicationAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    public ApplicationAsyncResourceImpl(
            @Qualifier("HakemusServiceRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
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
    public Observable<List<HakemusWrapper>> getApplicationsByOidsWithPOST(String hakuOid, Collection<String> hakukohdeOids) {
        Map<String, List<String>> requestBody = new HashMap<>();
        requestBody.put("states", DEFAULT_STATES);
        requestBody.put("asIds", Collections.singletonList(hakuOid));
        requestBody.put("aoOids", Lists.newArrayList(hakukohdeOids));
        requestBody.put("keys", ApplicationAsyncResource.DEFAULT_KEYS);
        return this.<Map<String, List<String>>,List<Hakemus>>postAsObservableLazily(getUrl("haku-app.applications.listfull"), new TypeToken<List<Hakemus>>() {}.getType(),
                Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    LOG.info("Calling url {} with asIds {} and aoOids {}", client.getCurrentURI(), requestBody.get("asIds"), requestBody.get("aoOids"));
                    return client;
                }).map(this::toHakemusWrapper);
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByHakemusOids(List<String> hakemusOids) {
        return getApplicationsByHakemusOids(null, hakemusOids, Collections.emptyList());
    }

    private Observable<List<HakemusWrapper>> getApplicationsByHakemusOids(String hakuOid, Collection<String> hakemusOids, Collection<String> keys) {
        Function<List<Hakemus>, List<HakemusWrapper>> filterApplicationsInDefaultStates = hs ->
                hs.stream().filter(h ->
                        StringUtils.isEmpty(h.getState()) ||
                                DEFAULT_STATES.contains(h.getState()))
                        .map(HakuappHakemusWrapper::new)
                        .collect(Collectors.toList());
        return this.<List<String>, List<Hakemus>>postAsObservableLazily(getUrl("haku-app.applications.list"), new TypeToken<List<Hakemus>>() {}.getType(),
                Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("rows", DEFAULT_ROW_LIMIT);
                    if (!keys.isEmpty()) { // While this behaviour might seem strange, this is how the haku-app
                        client.query("asIds", hakuOid);  // implementation works too. The filter parameters are only
                        client.query("state", DEFAULT_STATES.toArray());  // applied when the keys parameter is given.
                        client.query("keys", keys.toArray());
                    }
                    return client;
                }).map(filterApplicationsInDefaultStates);
    }

    @Override
    public Observable<List<HakemusWrapper>> getApplicationsByhakemusOidsInParts(String hakuOid, List<String> hakemusOids, Collection<String> keys) {
        LOG.info("Haetaan " + hakemusOids.size() + " hakemusta haku-app:sta");
        return Observable.fromIterable(Lists.partition(hakemusOids, DEFAULT_ROW_LIMIT))
                .concatMap(oids -> getApplicationsByHakemusOids(hakuOid, oids, keys))
                .concatMap(Observable::fromIterable)
                .toList().toObservable();
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
