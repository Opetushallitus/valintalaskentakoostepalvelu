package fi.vm.sade.valinta.kooste.external.resource.koodisto.impl;

import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;

import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class KoodistoAsyncResourceImpl extends HttpResource implements KoodistoAsyncResource {

    @Autowired
    public KoodistoAsyncResourceImpl(@Value("${valintalaskentakoostepalvelu.koodisto.url:https://${host.virkailija}}") String address) {
        super(address, TimeUnit.HOURS.toMillis(20));
    }

    @Override
    public Peruutettava haeKoodisto(String koodistoUri, Consumer<List<Koodi>> callback, Consumer<Throwable> failureCallback) {
        String url = "/koodisto-service/rest/json/" + koodistoUri + "/koodi";
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .query("onlyValidKoodis", true)
                            //.query("koodistoVersio", 1)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .async()
                    .get(new GsonResponseCallback<List<Koodi>>(gson(), address, url, callback, failureCallback, new TypeToken<List<Koodi>>() {
                    }.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }

    @Override
    public Future<List<Koodi>> haeKoodisto(String koodistoUri) {
        try {
            WebClient client = getWebClient();
            return getWebClient()
                    .path("/koodisto-service/rest/json/" + koodistoUri + "/koodi")
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

