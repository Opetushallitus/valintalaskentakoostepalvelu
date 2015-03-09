package fi.vm.sade.valinta.kooste.external.resource.koodisto.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.Callback;
import fi.vm.sade.valinta.kooste.external.resource.Peruutettava;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.TyhjaPeruutettava;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
@Service
public class KoodistoAsyncResourceImpl extends HttpResource implements KoodistoAsyncResource {

    @Autowired
    public KoodistoAsyncResourceImpl(
            @Value("https://${host.virkailija}") String address
    ) {
        super(address, TimeUnit.HOURS.toMillis(20));
    }

    @Override
    public Peruutettava haeKoodisto(String koodistoUri, Consumer<List<Koodi>> callback, Consumer<Throwable> failureCallback) {
        String url = new StringBuilder("/koodisto-service/rest/json/").append(koodistoUri).append("/koodi").toString();
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .query("onlyValidKoodis", true)
                    //.query("koodistoVersio", 1)
                    .async()
                    .get(new Callback<List<Koodi>>(GSON, address, url, callback, failureCallback, new TypeToken<List<Koodi>>() {
                    }.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }
}

