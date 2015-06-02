package fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.GenericType;
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
            @Value("${web.url.cas}") String webCasUrl,
            @Value("https://${host.virkailija}/suoritusrekisteri/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
            @Value("https://${host.virkailija}") String address,
            ApplicationContext context
    ) {
        super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.MINUTES.toMillis(10));
    }

    public Peruutettava getOppijatByHakukohde(String hakukohdeOid, String referenssiPvm, Consumer<List<Oppija>> callback, Consumer<Throwable> failureCallback) {
        String url = "/suoritusrekisteri/rest/v1/oppijat";
        try {
            WebClient client = getWebClient()
                    .path(url);
            if (referenssiPvm != null) {
                client.query("ensikertalaisuudenReferenssiPvm", referenssiPvm);
            }
            return new PeruutettavaImpl(client
                    .query("hakukohde", hakukohdeOid)
                    .async()
                    .get(new Callback<List<Oppija>>(address, url + "?hakukohde=" + hakukohdeOid + "&ensikertalaisuudenReferenssiPvm=" + referenssiPvm, callback, failureCallback, new TypeToken<List<Oppija>>() {
                    }.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    public Future<Response> getSuorituksetByOppija(String opiskelijaOid, Consumer<Oppija> callback, Consumer<Throwable> failureCallback) {
        String url = "/suoritusrekisteri/rest/v1/oppijat/" + opiskelijaOid;
        return getWebClient()
                .path(url)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .get(new Callback<Oppija>(address, url, callback, failureCallback, new TypeToken<Oppija>() {
                }.getType()));
    }
}
