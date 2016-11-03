package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.impl;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;

import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class OhjausparametritAsyncResourceImpl extends UrlConfiguredResource implements OhjausparametritAsyncResource {

    @Autowired
    public OhjausparametritAsyncResourceImpl(UrlConfiguration urlConfiguration,
                                             @Value("${valintalaskentakoostepalvelu.ohjausparametrit.request.timeout.seconds:20}") int requestTimeoutSeconds) {
        super(urlConfiguration, TimeUnit.SECONDS.toMillis(requestTimeoutSeconds));
    }
    public Observable<ParametritDTO> haeHaunOhjausparametrit(String hakuOid) {
        return getAsObservable(getUrl("ohjausparametrit-service.parametri", hakuOid), ParametritDTO.class);
    }

    public Peruutettava haeHaunOhjausparametrit(String hakuOid, Consumer<ParametritDTO> callback, Consumer<Throwable> failureCallback) {
        String url = getUrl("ohjausparametrit-service.parametri", hakuOid);
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .async()
                    .get(new GsonResponseCallback<ParametritDTO>(gson(), url, callback, failureCallback, new TypeToken<ParametritDTO>() {
                    }.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }
}
