package fi.vm.sade.valinta.kooste.external.resource.sijoittelu.impl;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;

@Service
public class SijoitteleAsyncResourceImpl extends HttpResource implements SijoitteleAsyncResource {
    @Autowired
    public SijoitteleAsyncResourceImpl(@Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}") String address) {
        super(address, TimeUnit.MINUTES.toMillis(50));
    }

    @Override
    public void sijoittele(String hakuOid, Consumer<String> callback, Consumer<Throwable> failureCallback) {
        String url = "/sijoittele/" + hakuOid + "/";
        try {
            getWebClient()
                    .path(url)
                    .accept(MediaType.WILDCARD_TYPE)
                    .async()
                    .get(new GsonResponseCallback<String>(gson(), address, url, callback,
                            failureCallback, new TypeToken<String>() {
                    }.getType()));
        } catch (Exception e) {
            failureCallback.accept(e);
        }
    }
}
