package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class SuoritusrekisteriAsyncResourceImpl extends AsyncResourceWithCas implements SuoritusrekisteriAsyncResource {

    @Autowired
    public SuoritusrekisteriAsyncResourceImpl(
            @Qualifier("SuoritusrekisteriRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
            @Value("${host.scheme:https}://${host.virkailija}") String address,
            ApplicationContext context
    ) {
        super(casInterceptor, address, context, TimeUnit.MINUTES.toMillis(10));
    }

    @Override
    public Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid,
                                                          String hakuOid) {
        return getAsObservable(
                "/suoritusrekisteri/rest/v1/oppijat",
                new TypeToken<List<Oppija>>() { }.getType(),
                client -> {
                    client.query("hakukohde", hakukohdeOid);
                    client.query("haku", hakuOid);
                    return client;
                }
        );
    }

    @Override
    public Peruutettava getOppijatByHakukohde(String hakukohdeOid,
                                              String hakuOid,
                                              Consumer<List<Oppija>> callback,
                                              Consumer<Throwable> failureCallback) {
        String url = "/suoritusrekisteri/rest/v1/oppijat";
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .query("hakukohde", hakukohdeOid)
                    .query("haku", hakuOid)
                    .async()
                    .get(new GsonResponseCallback<>(
                            address,
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
    public Observable<Oppija> getSuorituksetByOppija(String opiskelijaOid, String hakuOid) {
        return getAsObservable("/suoritusrekisteri/rest/v1/oppijat/" + opiskelijaOid, Oppija.class, client -> {
            client.accept(MediaType.APPLICATION_JSON_TYPE);
            client.query("haku", hakuOid);
            return client;
        });
    }

    @Override
    public Observable<Oppija> getSuorituksetWithoutEnsikertalaisuus(String opiskelijaOid) {
        return getAsObservable("/suoritusrekisteri/rest/v1/oppijat/" + opiskelijaOid, Oppija.class, client -> {
            client.accept(MediaType.APPLICATION_JSON_TYPE);
            return client;
        });
    }

    @Override
    public Future<Response> getSuorituksetByOppija(String opiskelijaOid,
                                                   String hakuOid,
                                                   Consumer<Oppija> callback,
                                                   Consumer<Throwable> failureCallback) {
        String url = "/suoritusrekisteri/rest/v1/oppijat/" + opiskelijaOid;
        return getWebClient()
                .path(url)
                .query("haku", hakuOid)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(new GsonResponseCallback<Oppija>(
                        address,
                        url  + "?haku=" + hakuOid,
                        callback,
                        failureCallback,
                        new TypeToken<Oppija>() {
                        }.getType()
                ));
    }
}
