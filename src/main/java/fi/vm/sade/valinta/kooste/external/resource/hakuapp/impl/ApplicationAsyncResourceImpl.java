package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import static rx.observables.BlockingObservable.from;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusOid;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusPrototyyppiBatch;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ListFullSearchDTO;
import fi.vm.sade.valinta.kooste.hakemus.dto.ApplicationOidsAndReason;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ApplicationAsyncResourceImpl extends UrlConfiguredResource implements ApplicationAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    public ApplicationAsyncResourceImpl(
            @Qualifier("HakemusServiceRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
    }

    @Override
    public Observable<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        String url = getUrl("haku-app.applications.syntheticapplication");
        Entity<HakemusPrototyyppiBatch> entity = Entity.entity(new HakemusPrototyyppiBatch(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit), MediaType.APPLICATION_JSON);
        return this.putAsObservableLazily(url, new GenericType<List<Hakemus>>() {}.getType(), entity);
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid) {
        return getApplicationsByOids(hakuOid, Arrays.asList(hakukohdeOid));
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
        return this.<ListFullSearchDTO, List<HakemusOid>>postAsObservable(
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
        requestBody.put("asIds", Arrays.asList(hakuOid));
        requestBody.put("aoOids", Lists.newArrayList(hakukohdeOids));
        requestBody.put("keys", ApplicationAsyncResource.DEFAULT_KEYS);
        return postAsObservable(getUrl("haku-app.applications.listfull"), new TypeToken<List<Hakemus>>() {}.getType(),
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
        return postAsObservableLazily(getUrl("haku-app.applications.list"), new TypeToken<List<Hakemus>>() {}.getType(),
                Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("rows", DEFAULT_ROW_LIMIT);
                    if(!keys.isEmpty()) {
                        client.query("asIds", hakuOid);
                        client.query("state", DEFAULT_STATES.toArray());
                        client.query("keys", keys.toArray());
                    }
                    return client;
                });
    }

    @Override
    public List<Hakemus> getApplicationsByhakemusOidsInParts(String hakuOid, List<String> hakemusOids, Collection<String> keys) {
        LOG.info("Haetaan " + hakemusOids.size() + " hakemusta haku-app:sta");
        List<Hakemus> allApplications = new ArrayList<>();
        List<List<String>> partialIdLists = Lists.partition(hakemusOids, DEFAULT_PART_ROW_LIMIT);
        for (int patchNo = 1; patchNo <= partialIdLists.size(); patchNo++) {
            List<Hakemus> applicationBatch = from(getApplicationsByHakemusOids(hakuOid, partialIdLists.get(patchNo - 1), keys)).first();
            allApplications.addAll(applicationBatch);
            if(patchNo < partialIdLists.size()) {
                LOG.info("Haettu " + allApplications.size() + " hakemusta. Haetaan lisää.");
            }
        }
        LOG.info("Haettiin " + allApplications.size() + " hakemusta haku-app:sta onnistuneesti.");
        return allApplications;
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
        return getAsObservable(getUrl("haku-app.applications", hakemusOid), Hakemus.class);
    }

    @Override
    public Peruutettava getApplicationsByOids(Collection<String> hakemusOids, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback) {
        String url = getUrl("haku-app.applications.list");
        return new PeruutettavaImpl(getWebClient()
                .path(url)
                .query("rows", DEFAULT_ROW_LIMIT)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(Lists.newArrayList(hakemusOids),
                        MediaType.APPLICATION_JSON_TYPE),
                        new GsonResponseCallback<List<Hakemus>>(gson(),
                        url + "?rows=" + DEFAULT_ROW_LIMIT,
                        callback,
                        failureCallback,
                        new TypeToken<List<Hakemus>>() {}.getType())));
    }

    public Peruutettava getApplicationsByOid(String hakuOid, String hakukohdeOid, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback) {
        String url = getUrl("haku-app.applications.listfull");
        try {
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .query("appState", "ACTIVE", "INCOMPLETE")
                            .query("rows", DEFAULT_ROW_LIMIT)
                            .query("asId", hakuOid)
                            .query("aoOid", hakukohdeOid)
                            .async()
                            .get(new GsonResponseCallback<List<Hakemus>>(gson(),
                                    url + "?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid=" + hakukohdeOid + "&asId=" + hakuOid,
                                    callback,
                                    failureCallback,
                                    new TypeToken<List<Hakemus>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    @Override
    public Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOids, String reason) {
        return postAsObservable(getUrl("haku-app.applications.state.passivate"), Entity.json(new ApplicationOidsAndReason(hakemusOids, reason)));
    }
}
