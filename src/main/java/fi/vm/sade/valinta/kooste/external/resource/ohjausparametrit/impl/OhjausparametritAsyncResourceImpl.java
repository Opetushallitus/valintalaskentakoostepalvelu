package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.impl;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
@Service
public class OhjausparametritAsyncResourceImpl extends AsyncResourceWithCas implements OhjausparametritAsyncResource {


    @Autowired
    public OhjausparametritAsyncResourceImpl(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("https://${host.virkailija}/ohjausparametrit-service/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintatieto}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintatieto}") String appClientPassword,
            @Value("${host.scheme:https}://${host.virkailija}") String address,
            ApplicationContext context
    ) {
        super(webCasUrl, targetService, appClientUsername, appClientPassword, address, context, TimeUnit.MINUTES.toMillis(10));
    }

    public Peruutettava haeHaunOhjausparametrit(String hakuOid,
                                         Consumer<ParametritDTO> callback,
                                         Consumer<Throwable> failureCallback) {
        String url = new StringBuilder("/ohjausparametrit-service/api/v1/rest/parametri/").append(hakuOid).toString();
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .async()
                    .get(new GsonResponseCallback<ParametritDTO>(GSON, address, url, callback, failureCallback, new TypeToken<ParametritDTO>() {
                    }.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }
}
