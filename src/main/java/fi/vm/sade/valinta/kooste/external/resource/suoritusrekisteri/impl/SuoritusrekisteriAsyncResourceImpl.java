package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import io.mikael.urlbuilder.UrlBuilder;
import io.reactivex.Observable;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.lang.reflect.Type;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SuoritusrekisteriAsyncResourceImpl extends UrlConfiguredResource implements SuoritusrekisteriAsyncResource {
    private static final Logger LOG = LoggerFactory.getLogger(SuoritusrekisteriAsyncResourceImpl.class);
    private final HttpClient httpClient;
    private int maxOppijatPostSize = 5000;

    @Autowired
    public SuoritusrekisteriAsyncResourceImpl(
        @Qualifier("SuoritusrekisteriRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
        @Qualifier("SuoritusrekisteriHttpClient") HttpClient httpClient) {
        super(TimeUnit.MINUTES.toMillis(10), casInterceptor);
        this.httpClient = httpClient;
    }

    @Override
    public Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid,
                                                          String hakuOid) {
        return getAsObservableLazily(
                getUrl("suoritusrekisteri.oppijat"),
                new TypeToken<List<Oppija>>() { }.getType(),
                client -> {
                    client.query("hakukohde", hakukohdeOid);
                    client.query("haku", hakuOid);
                    return client;
                }
        );
    }

    @Override
    public CompletableFuture<List<Oppija>> getOppijatByHakukohdeWithoutEnsikertalaisuus(String hakukohdeOid, String hakuOid) {
        URI uri = UrlBuilder.fromString(getUrl("suoritusrekisteri.oppijat"))
            .addParameter("hakukohde", hakukohdeOid)
            .addParameter("haku", hakuOid)
            .addParameter("ensikertalaisuudet", "false").toUri();
        return httpClient.getJson(uri.toString(), Duration.ofMinutes(5), new com.google.gson.reflect.TypeToken<List<Oppija>>() { }.getType());
    }

    @Override
    public Observable<Oppija> getSuorituksetByOppija(String opiskelijaOid, String hakuOid) {
        return getAsObservableLazily(getUrl("suoritusrekisteri.oppijat.opiskelijaoid", opiskelijaOid), Oppija.class, client -> {
            client.accept(MediaType.APPLICATION_JSON_TYPE);
            client.query("haku", hakuOid);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        });
    }

    @Override
    public Observable<List<Oppija>> getSuorituksetByOppijas(List<String> opiskelijaOids, String hakuOid) {
        String url = getUrl("suoritusrekisteri.oppijat") + "/?ensikertalaisuudet=true" + "&haku=" + hakuOid;
        return batchedPostOppijas(opiskelijaOids, url);
    }

    @Override
    public Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(String opiskelijaOid) {
        return getAsObservableLazily(getUrl("suoritusrekisteri.oppijat.opiskelijaoid", opiskelijaOid), Oppija.class, client -> {
            client.accept(MediaType.APPLICATION_JSON_TYPE);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        });
    }

    @Override
    public
    Observable<List<Oppija>> getSuorituksetWithoutEnsikertalaisuus(List<String> opiskelijaOids) {
        String url = getUrl("suoritusrekisteri.oppijat") + "/?ensikertalaisuudet=false";
        return batchedPostOppijas(opiskelijaOids, url);
    }

    private Observable<List<Oppija>> batchedPostOppijas(List<String> opiskelijaOids, String url) {
        if (opiskelijaOids.isEmpty()) {
            LOG.info("Batched POST: empty list of oids provided. Returning an empty set without api call.");
            return Observable.just(Collections.emptyList());
        }
        List<List<String>> oidBatches = Lists.partition(opiskelijaOids, maxOppijatPostSize);
        LOG.info("Batched POST: {} oids partitioned into {} batches", opiskelijaOids.size(), oidBatches.size());

        Observable<Observable<List<Oppija>>> obses = Observable.fromIterable(oidBatches).map(oidBatch ->
                postAsObservableLazily(url,
                        new TypeToken<List<Oppija>>() {
                        }.getType(),
                        Entity.entity(oidBatch, MediaType.APPLICATION_JSON_TYPE),
                        client -> {
                            client.accept(MediaType.APPLICATION_JSON_TYPE);
                            LOG.info("Calling POST url {} with {} opiskelijaOids", client.getCurrentURI(), oidBatch.size());
                            return client;
                        })
        );

        // Add the elements returned by each response to one master list
        Observable<List<Oppija>> allOppijas = Observable
                .concat(obses);
        return allOppijas;
    }

    @Override
    public CompletableFuture<Suoritus> postSuoritus(Suoritus suoritus) {
        Type suoritusType = new com.google.gson.reflect.TypeToken<Suoritus>() {}.getType();
        return httpClient.postJson(getUrl("suoritusrekisteri.suoritukset"), Duration.ofMinutes(10), suoritus, suoritusType, suoritusType);
    }

    @Override
    public CompletableFuture<Arvosana> postArvosana(Arvosana arvosana) {
        Type arvosanaType = new com.google.gson.reflect.TypeToken<Arvosana>() {}.getType();
        return httpClient.postJson(getUrl("suoritusrekisteri.arvosanat"), Duration.ofMinutes(10), arvosana, arvosanaType, arvosanaType);
    }

    @Override
    public CompletableFuture<Arvosana> updateExistingArvosana(String arvosanaId, Arvosana arvosanaWithUpdatedValues) {
        Type arvosanaType = new com.google.gson.reflect.TypeToken<Arvosana>() {}.getType();
        return httpClient.postJson(
            getUrl("suoritusrekisteri.arvosanat.id", arvosanaId),
            Duration.ofMinutes(10),
            arvosanaWithUpdatedValues,
            arvosanaType,
            arvosanaType);
    }

    @Override
    public CompletableFuture<String> deleteSuoritus(String suoritusId) {
        return httpClient.delete(getUrl("suoritusrekisteri.suoritukset.id", suoritusId), Duration.ofMinutes(1));
    }

    @Override
    public CompletableFuture<String> deleteArvosana(String arvosanaId) {
        return httpClient.delete(getUrl("suoritusrekisteri.arvosanat.id", arvosanaId), Duration.ofMinutes(1));
    }
}
