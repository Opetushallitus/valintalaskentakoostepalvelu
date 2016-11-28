package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.*;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.hakemus.dto.ApplicationOidsAndReason;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static rx.observables.BlockingObservable.from;

@Service
public class ApplicationAsyncResourceImpl extends AsyncResourceWithCas implements ApplicationAsyncResource {

    private static final Gson GSON = DateDeserializer.gsonBuilder()
            .create();

    @Autowired
    public ApplicationAsyncResourceImpl(
            @Qualifier("HakemusServiceRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}") String address,
            ApplicationContext context) {
        super(casInterceptor, address, context, TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public Gson gson() {
        return GSON;
    }

    @Override
    public Future<List<Hakemus>> putApplicationPrototypes(String hakuOid, String hakukohdeOid, String tarjoajaOid, Collection<HakemusPrototyyppi> hakemusPrototyypit) {
        String url = "/applications/syntheticApplication";
        return getWebClient().path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .put(Entity.entity(new HakemusPrototyyppiBatch(hakuOid, hakukohdeOid, tarjoajaOid, hakemusPrototyypit), MediaType.APPLICATION_JSON), new GenericType<List<Hakemus>>() {});
    }

    /**
     * /haku-app/applications/listfull?appState=ACTIVE&appState=INCOMPLETE&rows=100000&asId={hakuOid}&aoOid={hakukohdeOid}
     */
    @Override
    public Observable<List<Hakemus>> getApplicationsByOid(String hakuOid, String hakukohdeOid) {
        return getApplicationsByOids(hakuOid, Arrays.asList(hakukohdeOid));
    }

    @Override
    public Observable<List<ShortHakemus>> getShortApplicationsByOid(String hakuOid, String hakukohdeOid) {
        return getAsObservable("/applications/listshort", new TypeToken<List<ShortHakemus>>() {}.getType(), client -> {
            client.query("asId", hakuOid);
            client.query("aoOid", hakukohdeOid);
            return client;
        });
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids) {
        return getAsObservable("/applications/listfull", new TypeToken<List<Hakemus>>() {}.getType(), client -> {
            client.query("appState", DEFAULT_STATES.toArray());
            client.query("rows", DEFAULT_ROW_LIMIT).query("asId", hakuOid).query("aoOid", hakukohdeOids);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        });
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOidsWithPOST(String hakuOid, Collection<String> hakukohdeOids) {
        Map<String, List<String>> requestBody = new HashMap();
        requestBody.put("states", DEFAULT_STATES);
        requestBody.put("asIds", Arrays.asList(hakuOid));
        requestBody.put("aoOids", Lists.newArrayList(hakukohdeOids));
        requestBody.put("keys", ApplicationAsyncResource.DEFAULT_KEYS);
        return postAsObservable("/applications/listfull", new TypeToken<List<Hakemus>>() {}.getType(),
                Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    LOG.info("Calling url {} with asIds {} and aoOids {}", client.getCurrentURI(), requestBody.get("asIds"), requestBody.get("aoOids"));
                    return client;
                });
        //return postAsObservable("/applications/listfull", new TypeToken<List<Hakemus>>() {}.getType(), Entity.json(requestBody));
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByHakemusOids(List<String> hakemusOids) {
        return getApplicationsByHakemusOids(null, hakemusOids, Collections.emptyList());
    }

    private Observable<List<Hakemus>> getApplicationsByHakemusOids(String hakuOid, Collection<String> hakemusOids, Collection<String> keys) {
        return postAsObservable("/applications/list", new TypeToken<List<Hakemus>>() {}.getType(),
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
    public Future<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids) {
        return getWebClient()
                .path("/applications/list")
                .query("rows", DEFAULT_ROW_LIMIT)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE), new GenericType<List<Hakemus>>() {});
    }

    @Override
    public Observable<Hakemus> getApplication(String hakemusOid) {
        return getAsObservable("/applications/" + hakemusOid, Hakemus.class);
    }

    @Override
    public Observable<List<ApplicationAdditionalDataDTO>> getApplicationAdditionalData(Collection<String> hakemusOids) {
        return postAsObservable(
                "/applications/additionalData",
                new TypeToken<List<ApplicationAdditionalDataDTO>>(){
                }.getType(),
                Entity.entity(gson().toJson(hakemusOids), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Peruutettava getApplicationsByOids(Collection<String> hakemusOids, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback) {
        String url = "/applications/list";
        return new PeruutettavaImpl(getWebClient()
                .path(url)
                .query("rows", DEFAULT_ROW_LIMIT)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE), new GsonResponseCallback<List<Hakemus>>(gson(),
                        address,
                        url + "?rows=" + DEFAULT_ROW_LIMIT,
                        callback,
                        failureCallback,
                        new TypeToken<List<Hakemus>>() {}.getType())));
    }

    public Peruutettava getApplicationsByOid(String hakuOid, String hakukohdeOid, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback) {
        String url = "/applications/listfull";
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
                                    address,
                                    url + "?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid=" + hakukohdeOid + "&asId=" + hakuOid,
                                    callback,
                                    failureCallback,
                                    new TypeToken<List<Hakemus>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Observable<List<ApplicationAdditionalDataDTO>> getApplicationAdditionalData(String hakuOid, String hakukohdeOid) {
        return getAsObservable(
                "/applications/additionalData/" + hakuOid + "/" + hakukohdeOid,
                new TypeToken<List<ApplicationAdditionalDataDTO>>() {}.getType()
        );
    }

    @Override
    public Observable<Response> putApplicationAdditionalData(String hakuOid, String hakukohdeOid, List<ApplicationAdditionalDataDTO> additionalData) {
        return putAsObservable("/applications/additionalData/" + hakuOid + "/" + hakukohdeOid, Entity.json(additionalData));
    }

    @Override
    public Observable<Response> putApplicationAdditionalData(String hakuOid, List<ApplicationAdditionalDataDTO> additionalData) {
        return putAsObservable("/applications/additionalData/" + hakuOid, Entity.json(additionalData));
    }

    @Override
    public Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOids, String reason) {
        return postAsObservable("/applications/state/passivate", Entity.json(new ApplicationOidsAndReason(hakemusOids, reason)));
    }
}
