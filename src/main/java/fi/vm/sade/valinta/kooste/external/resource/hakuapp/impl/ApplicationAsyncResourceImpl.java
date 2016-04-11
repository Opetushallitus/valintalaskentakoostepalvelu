package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusPrototyyppiBatch;
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

@Service
public class ApplicationAsyncResourceImpl extends AsyncResourceWithCas implements ApplicationAsyncResource {
    @Autowired
    public ApplicationAsyncResourceImpl(
            @Qualifier("HakemusServiceRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}") String address,
            ApplicationContext context) {
        super(casInterceptor, address, context, TimeUnit.HOURS.toMillis(1));
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
        return getAsObservable("/applications/listfull", new TypeToken<List<Hakemus>>() {}.getType(), client -> {
            client.query("appState", "ACTIVE", "INCOMPLETE");
            client.query("rows", 100000).query("asId", hakuOid).query("aoOid", hakukohdeOid);
            return client;
        });
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOids(String hakuOid, Collection<String> hakukohdeOids) {
        return getAsObservable("/applications/listfull", new TypeToken<List<Hakemus>>() {}.getType(), client -> {
            client.query("appState", "ACTIVE", "INCOMPLETE");
            client.query("rows", 100000).query("asId", hakuOid).query("aoOid", hakukohdeOids);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        });
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByOidsWithPOST(String hakuOid, Collection<String> hakukohdeOids) {
        Map<String, List<String>> requestBody = new HashMap();
        requestBody.put("states", Arrays.asList("ACTIVE", "INCOMPLETE"));
        requestBody.put("asIds", Arrays.asList(hakuOid));
        requestBody.put("aoOids", Lists.newArrayList(hakukohdeOids));
        /*return postAsObservable("/applications/listfull", new TypeToken<List<Hakemus>>() {}.getType(),
                Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    LOG.info("Calling url {} with asIds {} and aoOids {}", client.getCurrentURI(), requestBody.get("asIds"), requestBody.get("aoOids"));
                    return client;
                });*/
        return postAsObservable("/applications/listfull", new TypeToken<List<Hakemus>>() {}.getType(), Entity.json(requestBody));
    }

    @Override
    public Observable<List<Hakemus>> getApplicationsByHakemusOids(Collection<String> hakemusOids) {
        return postAsObservable("/applications/list", new TypeToken<List<Hakemus>>() {}.getType(),
                Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("rows", 100000);
                    return client;
                });
    }

    @Override
    public Future<List<Hakemus>> getApplicationsByOids(Collection<String> hakemusOids) {
        return getWebClient()
                .path("/applications/list")
                .query("rows", 100000)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE), new GenericType<List<Hakemus>>() {});
    }

    @Override
    public Observable<Hakemus> getApplication(String hakemusOid) {
        return getAsObservable("/applications/" + hakemusOid, Hakemus.class);
    }

    @Override
    public Peruutettava getApplicationsByOids(Collection<String> hakemusOids, Consumer<List<Hakemus>> callback, Consumer<Throwable> failureCallback) {
        String url = "/applications/list";
        return new PeruutettavaImpl(getWebClient()
                .path(url)
                .query("rows", 100000)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(Lists.newArrayList(hakemusOids), MediaType.APPLICATION_JSON_TYPE), new GsonResponseCallback<List<Hakemus>>(
                        address,
                        url + "?rows=100000",
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
                            .query("rows", 100000)
                            .query("asId", hakuOid)
                            .query("aoOid", hakukohdeOid)
                            .async()
                            .get(new GsonResponseCallback<List<Hakemus>>(
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

    public Peruutettava getApplicationAdditionalData(String hakuOid, String hakukohdeOid, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback) {
        String url = "/applications/additionalData/" + hakuOid + "/" + hakukohdeOid;
        try {
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .async()
                            .get(new GsonResponseCallback<List<ApplicationAdditionalDataDTO>>(
                                    address,
                                    url,
                                    callback,
                                    failureCallback,
                                    new TypeToken<List<ApplicationAdditionalDataDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Peruutettava getApplicationAdditionalData(Collection<String> hakemusOids, Consumer<List<ApplicationAdditionalDataDTO>> callback, Consumer<Throwable> failureCallback) {
        String url = "/applications/additionalData";
        try {
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .async()
                            .post(Entity.json(hakemusOids), new GsonResponseCallback<List<ApplicationAdditionalDataDTO>>(
                                    address,
                                    url,
                                    callback,
                                    failureCallback,
                                    new TypeToken<List<ApplicationAdditionalDataDTO>>() {}.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    @Override
    public Observable<Response> putApplicationAdditionalData(String hakuOid, String hakukohdeOid, List<ApplicationAdditionalDataDTO> additionalData) {
        return putAsObservable("/applications/additionalData/" + hakuOid + "/" + hakukohdeOid, Entity.json(additionalData));
    }

    @Override
    public Observable<Response> changeStateOfApplicationsToPassive(List<String> hakemusOids, String reason) {
        return postAsObservable("/applications/state/passivate", Entity.json(new ApplicationOidsAndReason(hakemusOids, reason)));
    }
}
