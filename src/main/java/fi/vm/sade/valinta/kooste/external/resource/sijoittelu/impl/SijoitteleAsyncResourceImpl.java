package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;

@Service
public class SijoitteleAsyncResourceImpl extends UrlConfiguredResource implements SijoitteleAsyncResource {

    @Autowired
    public SijoitteleAsyncResourceImpl(UrlConfiguration urlConfiguration) {
        super(urlConfiguration, TimeUnit.MINUTES.toMillis(50));
    }

    @Override
    public void sijoittele(String hakuOid, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        String url = getUrl("sijoittelu-service.sijoittele", hakuOid);
        try {
            getWebClient()
                    .path(url)
                    .accept(MediaType.WILDCARD_TYPE)
                    .async()
                    .get(new GsonResponseCallback<String>(gson(), url, callback,
                            failureCallback, new TypeToken<String>() {
                    }.getType()));
        } catch (Exception e) {
            failureCallback.accept(e);
        }
    }
}
