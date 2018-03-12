package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.*;
import fi.vm.sade.valinta.kooste.hakemus.dto.ApplicationOidsAndReason;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.functions.Func1;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ApplicationAsyncResourceImpl extends UrlConfiguredResource implements ApplicationAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    public ApplicationAsyncResourceImpl(
            @Qualifier("AtaruRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
    }

    @Override
    public Observable<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        String url = getUrl("haku-app.applications.syntheticapplication");
        HakemusPrototyyppiBatch hakemusPrototyyppiBatch = new HakemusPrototyyppiBatch(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit);
        Entity<String> entity = Entity.entity(gson().toJson(hakemusPrototyyppiBatch), MediaType.APPLICATION_JSON);
        return this.putAsObservableLazily(url, new GenericType<List<Hakemus>>() {}.getType(), entity, ACCEPT_JSON);
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid) {
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
    public Observable<List<Hakemus>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids) {
        return getAsObservableLazily(getUrl("haku-app.applications.listfull"), new TypeToken<List<Hakemus>>() {}.getType(), client -> {
            client.query("appState", DEFAULT_STATES.toArray());
            client.query("rows", DEFAULT_ROW_LIMIT).query("asId", hakuOid).query("aoOid", hakukohdeOids);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        });
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOidsWithPOST(String hakuOid, Collection<String> hakukohdeOids) {
        Map<String, List<String>> requestBody = new HashMap<>();
        requestBody.put("states", DEFAULT_STATES);
        requestBody.put("asIds", Collections.singletonList(hakuOid));
        requestBody.put("aoOids", Lists.newArrayList(hakukohdeOids));
        requestBody.put("keys", ApplicationAsyncResource.DEFAULT_KEYS);
        return postAsObservableLazily(getUrl("haku-app.applications.listfull"), new TypeToken<List<Hakemus>>() {}.getType(),
                Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    LOG.info("Calling url {} with asIds {} and aoOids {}", client.getCurrentURI(), requestBody.get("asIds"), requestBody.get("aoOids"));
                    return client;
                });
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByHakemusOids(List<String> hakemusOids) {
        return getApplicationsByHakemusOids(null, hakemusOids, Collections.emptyList());
    }

    private Observable<List<Hakemus>> getApplicationsByHakemusOids(String hakuOid, Collection<String> hakemusOids, Collection<String> keys) {
        Func1<List<Hakemus>, List<Hakemus>> filterApplicationsInDefaultStates = hs ->
            hs.stream().filter(h ->
                    StringUtils.isEmpty(h.getState()) ||
                    DEFAULT_STATES.contains(h.getState())
            ).collect(Collectors.toList());
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
    public Observable<List<Hakemus>> getApplicationsByhakemusOidsInParts(String hakuOid, List<String> hakemusOids, Collection<String> keys) {
        LOG.info("Haetaan " + hakemusOids.size() + " hakemusta haku-app:sta");
        return Observable.from(Lists.partition(hakemusOids, DEFAULT_ROW_LIMIT))
                .concatMap(oids -> getApplicationsByHakemusOids(hakuOid, oids, keys))
                .concatMap(Observable::from)
                .toList();
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids) {
        return postAsObservableLazily(getUrl("haku-app.applications.list"),
            new GenericType<List<Hakemus>>() {}.getType(),
            Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE),
            webClient -> webClient.query("rows", DEFAULT_ROW_LIMIT).accept(MediaType.APPLICATION_JSON_TYPE));
    }

    @Override
    public Observable<Hakemus> getApplication(String hakemusOid) {
        return getAsObservableLazily(getUrl("haku-app.applications", hakemusOid), Hakemus.class);
    }

    @Override
    public Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOids, String reason) {
        return postAsObservableLazily(getUrl("haku-app.applications.state.passivate"), Entity.json(new ApplicationOidsAndReason(hakemusOids, reason)));
    }
}
