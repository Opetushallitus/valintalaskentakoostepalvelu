package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
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
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import com.google.common.collect.Lists;

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
        return getAsObservable(
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
        return getAsObservable(
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
    public Peruutettava getOppijatByHakukohde(String hakukohdeOid,
                                              String hakuOid,
                                              Consumer<List<Oppija>> callback,
                                              Consumer<Throwable> failureCallback) {
        String url = getUrl("suoritusrekisteri.oppijat");
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .query("hakukohde", hakukohdeOid)
                    .query("haku", hakuOid)
                    .async()
                    .get(new GsonResponseCallback<>(gson(),
                            url + "?hakukohde=" + hakukohdeOid + "&haku=" + hakuOid,
                            callback,
                            failureCallback, new TypeToken<List<Oppija>>() {
                            }.getType()
                    ))
            );
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    @Override
    public Observable<Oppija> getSuorituksetByOppija(String opiskelijaOid, String hakuOid) {
        return getAsObservable(getUrl("suoritusrekisteri.oppijat.opiskelijaoid", opiskelijaOid), Oppija.class, client -> {
            client.accept(MediaType.APPLICATION_JSON_TYPE);
            client.query("haku", hakuOid);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        });
    }

    @Override
    public Observable<List<Oppija>> getSuorituksetByOppijas(List<String> opiskelijaOids, String hakuOid) {
        String url = getUrl("suoritusrekisteri.oppijat") + "/?ensikertalaisuudet=true" + "&haku=" + hakuOid;

        List<List<String>> opiskelijaOidBatches = Lists.partition(opiskelijaOids, maxOppijatPostSize);

        Observable<Observable<List<Oppija>>> obses = Observable.from(opiskelijaOidBatches).map(oidBatch ->
                postAsObservable(url,
                        new TypeToken<List<Oppija>>() { }.getType(),
                        Entity.entity(opiskelijaOids, MediaType.APPLICATION_JSON_TYPE),
                        client -> {
                            client.accept(MediaType.APPLICATION_JSON_TYPE);
                            LOG.info("Calling POST url {} with {} opiskelijaOids", client.getCurrentURI(), oidBatch.size());
                            return client;
                })
        );

        return Observable.concat(obses);
    }

    @Override
    public Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(String opiskelijaOid) {
        return getAsObservable(getUrl("suoritusrekisteri.oppijat.opiskelijaoid", opiskelijaOid), Oppija.class, client -> {
            client.accept(MediaType.APPLICATION_JSON_TYPE);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        });
    }

    @Override
    public
    Observable<List<Oppija>> getSuorituksetWithoutEnsikertalaisuus(List<String> opiskelijaOids) {
        String url = getUrl("suoritusrekisteri.oppijat") + "/?ensikertalaisuudet=false";

        List<List<String>> opiskelijaOidBatches = Lists.partition(opiskelijaOids, maxOppijatPostSize);

        Observable<Observable<List<Oppija>>> obses = Observable.from(opiskelijaOidBatches).map(oidBatch ->
                postAsObservable(url,
                    new TypeToken<List<Oppija>>() { }.getType(),
                    Entity.entity(opiskelijaOids, MediaType.APPLICATION_JSON_TYPE),
                    client -> {
                        client.accept(MediaType.APPLICATION_JSON_TYPE);
                        LOG.info("Calling POST url {} with {} opiskelijaOids", client.getCurrentURI(), oidBatch.size());
                        return client;
                })
        );

        return Observable.concat(obses);
    }

    @Override
    public Future<Response> getSuorituksetByOppija(String opiskelijaOid,
                                                   String hakuOid,
                                                   Consumer<Oppija> callback,
                                                   Consumer<Throwable> failureCallback) {
        String url = getUrl("suoritusrekisteri.oppijat.opiskelijaoid", opiskelijaOid);
        return getWebClient()
                .path(url)
                .query("haku", hakuOid)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(new GsonResponseCallback<Oppija>(gson(),
                        url  + "?haku=" + hakuOid,
                        callback,
                        failureCallback,
                        new TypeToken<Oppija>() {
                        }.getType()
                ));
    }

    @Override
    public Observable<Suoritus> postSuoritus(Suoritus suoritus) {
        return postAsObservable(
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
        return postAsObservable(
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
        return postAsObservable(
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
    public Observable<Void> deleteSuoritus(String suoritusId) {
        return deleteAsObservable(
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
    public Observable<Void> deleteArvosana(String arvosanaId) {
        return deleteAsObservable(
                getUrl("suoritusrekisteri.arvosanat.id", arvosanaId), new TypeToken<Arvosana>(){}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                }
        );
    }
}
