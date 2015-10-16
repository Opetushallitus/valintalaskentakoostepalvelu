package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class SuoritusrekisteriAsyncResourceImpl extends AsyncResourceWithCas implements SuoritusrekisteriAsyncResource {

    @Autowired
    public SuoritusrekisteriAsyncResourceImpl(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("https://${host.virkailija}/suoritusrekisteri/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
            @Value("${host.scheme:https}://${host.virkailija}") String address,
            ApplicationContext context
    ) {
        super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.MINUTES.toMillis(10));
    }

    @Override
    public Observable<List<Oppija>> getOppijatByHakukohde(String hakukohdeOid,
                                                          String ensikertalaisuudenRajapvm) {
        return getAsObservable(
                "/suoritusrekisteri/rest/v1/oppijat",
                new TypeToken<List<Oppija>>() { }.getType(),
                client -> {
                    client.query("hakukohde", hakukohdeOid);
                    if (ensikertalaisuudenRajapvm != null) {
                        client.query("ensikertalaisuudenRajapvm", ensikertalaisuudenRajapvm);
                    }
                    return client;
                }
        );
    }

    @Override
    public Peruutettava getOppijatByHakukohde(String hakukohdeOid,
                                              String ensikertalaisuudenRajapvm,
                                              Consumer<List<Oppija>> callback,
                                              Consumer<Throwable> failureCallback) {
        String url = "/suoritusrekisteri/rest/v1/oppijat?hakukohde=" + hakukohdeOid + "&ensikertalaisuudenRajapvm=" + ensikertalaisuudenRajapvm;
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .async()
                    .get(new GsonResponseCallback<>(
                            address,
                            url,
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
    public Future<Response> getSuorituksetByOppija(String opiskelijaOid,
                                                   String ensikertalaisuudenRajapvm,
                                                   Consumer<Oppija> callback,
                                                   Consumer<Throwable> failureCallback) {
        String url = "/suoritusrekisteri/rest/v1/oppijat/" + opiskelijaOid + "?ensikertalaisuudenRajapvm=" + ensikertalaisuudenRajapvm;
        return getWebClient()
                .path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(new GsonResponseCallback<Oppija>(
                        address,
                        url,
                        callback,
                        failureCallback,
                        new TypeToken<Oppija>() {
                        }.getType()
                ));
    }
}
