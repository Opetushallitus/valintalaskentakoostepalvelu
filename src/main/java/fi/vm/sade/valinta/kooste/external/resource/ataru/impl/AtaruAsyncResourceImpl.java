package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
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

    private Observable<List<AtaruHakemus>> getApplications(String hakukohdeOid, List<String> hakemusOids) {
        return postAsObservableLazily(
                getUrl("ataru.applications.by-hakukohde"),
                new TypeToken<List<AtaruHakemus>>() {}.getType(),
                Entity.entity(gson().toJson(hakemusOids), MediaType.APPLICATION_JSON),
                client -> {
                    if (hakukohdeOid != null) {
                        client.query("hakukohdeOid", hakukohdeOid);
                    }
                    LOG.info("Calling url {}", client.getCurrentURI());
                    return client;
                });
    }

    @Override
    public Observable<List<AtaruHakemus>> getApplicationsByHakukohde(String hakukohdeOid) {
        return getApplications(hakukohdeOid, Lists.newArrayList());
    }

    @Override
    public Observable<List<AtaruHakemus>> getApplicationsByOids(List<String> oids) {
        return getApplications(null, oids);
    }
}