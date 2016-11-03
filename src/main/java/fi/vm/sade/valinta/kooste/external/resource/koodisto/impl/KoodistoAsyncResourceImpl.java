package fi.vm.sade.valinta.kooste.external.resource.koodisto.impl;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;

import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class KoodistoAsyncResourceImpl extends UrlConfiguredResource implements KoodistoAsyncResource {

    @Autowired
    public KoodistoAsyncResourceImpl(UrlConfiguration urlConfiguration) {
        super(urlConfiguration, TimeUnit.HOURS.toMillis(20));
    }

    @Override
    public Peruutettava haeKoodisto(String koodistoUri, Consumer<List<Koodi>> callback, Consumer<Throwable> failureCallback) {
        String url = getUrl("koodisto-service.json.oid.koodi", koodistoUri);
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .query("onlyValidKoodis", true)
                            //.query("koodistoVersio", 1)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GsonResponseCallback<List<Koodi>>(gson(), url, callback, failureCallback, new TypeToken<List<Koodi>>() {
                    }.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    @Override
    public Future<List<Koodi>> haeKoodisto(String koodistoUri) {
        try {
            return getWebClient()
                    .path(getUrl("koodisto-service.json.oid.koodi", koodistoUri))
                    .query("onlyValidKoodis", true)
                            //.query("koodistoVersio", 1)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GenericType<List<Koodi>>() {
                    });
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }
}

