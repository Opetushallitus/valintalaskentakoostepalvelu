package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SuoritusrekisteriAsyncResourceImpl extends UrlConfiguredResource implements SuoritusrekisteriAsyncResource {
    private static final Logger LOG = LoggerFactory.getLogger(SuoritusrekisteriAsyncResourceImpl.class);

    private int maxOppijatPostSize = 5000;

    @Autowired
    public SuoritusrekisteriAsyncResourceImpl(
            @Qualifier("SuoritusrekisteriRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.MINUTES.toMillis(10), casInterceptor);
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
    public Observable<List<Oppija>> getOppijatByHakukohdeWithoutEnsikertalaisuus(String hakukohdeOid, String hakuOid) {
        return getAsObservableLazily(
                getUrl("suoritusrekisteri.oppijat"),
                new TypeToken<List<Oppija>>() { }.getType(),
                client -> {
                    client.query("hakukohde", hakukohdeOid);
                    client.query("haku", hakuOid);
                    client.query("ensikertalaisuudet", "false");
                    return client;
                }
        );
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
        List<List<String>> oidBatches = Lists.partition(opiskelijaOids, maxOppijatPostSize);
        LOG.info("Batched POST: {} oids partitioned into {} batches", opiskelijaOids.size(), oidBatches.size());

        Observable<Observable<List<Oppija>>> obses = Observable.fromIterable(oidBatches).map(oidBatch ->
                postAsObservableLazily(url,
                        new TypeToken<List<Oppija>>() { }.getType(),
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

        allOppijas.subscribe(l -> {
            LOG.info("Finished batched POST with {} results", l.size());
        });

        return allOppijas;
    }

    @Override
    public Observable<Suoritus> postSuoritus(Suoritus suoritus) {
        return postAsObservableLazily(
                getUrl("suoritusrekisteri.suoritukset"),
                new TypeToken<Suoritus>(){
                }.getType(),
                Entity.entity(gson().toJson(suoritus), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<Arvosana> postArvosana(Arvosana arvosana) {
        return postAsObservableLazily(
                getUrl("suoritusrekisteri.arvosanat"),
                new TypeToken<Arvosana>(){
                }.getType(),
                Entity.entity(gson().toJson(arvosana), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<Arvosana> updateExistingArvosana(String arvosanaId, Arvosana arvosanaWithUpdatedValues) {
        return postAsObservableLazily(
                getUrl("suoritusrekisteri.arvosanat.id",  arvosanaId),
                new TypeToken<Arvosana>(){}.getType(),
                Entity.entity(gson().toJson(arvosanaWithUpdatedValues), MediaType.APPLICATION_JSON_TYPE),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<String> deleteSuoritus(String suoritusId) {
        return deleteAsObservableLazily(
                getUrl("suoritusrekisteri.suoritukset.id", suoritusId),
                new TypeToken<Suoritus>(){
                }.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }

    @Override
    public Observable<String> deleteArvosana(String arvosanaId) {
        return deleteAsObservableLazily(
                getUrl("suoritusrekisteri.arvosanat.id", arvosanaId), new TypeToken<Arvosana>(){}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }
}
