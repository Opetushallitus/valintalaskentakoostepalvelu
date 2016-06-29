package fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.impl;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Service
public class OhjausparametritAsyncResourceImpl extends HttpResource implements OhjausparametritAsyncResource {

    @Autowired
    public OhjausparametritAsyncResourceImpl(@Value("${host.scheme:https}://${host.virkailija}") String address) {
        super(address, TimeUnit.SECONDS.toMillis(10));
    }
    public Observable<ParametritDTO> haeHaunOhjausparametrit(String hakuOid) {
        return getAsObservable("/ohjausparametrit-service/api/v1/rest/parametri/" + hakuOid, ParametritDTO.class);
    }

    public Peruutettava haeHaunOhjausparametrit(String hakuOid, Consumer<ParametritDTO> callback, Consumer<Throwable> failureCallback) {
        String url = "/ohjausparametrit-service/api/v1/rest/parametri/" + hakuOid;
        try {
            return new PeruutettavaImpl(getWebClient()
                    .path(url)
                    .async()
                    .get(new GsonResponseCallback<ParametritDTO>(gson(), address, url, callback, failureCallback, new TypeToken<ParametritDTO>() {
                    }.getType())));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }
}
