package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import rx.Observable;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AtaruAsyncResourceImpl extends UrlConfiguredResource implements AtaruAsyncResource {
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Autowired
    public AtaruAsyncResourceImpl(
            @Qualifier("AtaruRestClientAsAdminCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
    }

    @Override
    public Observable<List<AtaruHakemus>> getApplicationsByHakukohde(String hakukohdeOid) {
        return getAsObservable(getUrl("ataru.applications.by-hakukohde"), new TypeToken<List<AtaruHakemus>>() {
        }.getType(), client -> {
            client.query("hakukohdeOid", hakukohdeOid);
            LOG.info("Calling url {}", client.getCurrentURI());
            return client;
        });
    }
}