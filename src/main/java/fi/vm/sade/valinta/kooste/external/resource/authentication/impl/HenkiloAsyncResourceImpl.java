package fi.vm.sade.valinta.kooste.external.resource.authentication.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;

@Service
public class HenkiloAsyncResourceImpl extends UrlConfiguredResource implements HenkiloAsyncResource {

    @Autowired
    public HenkiloAsyncResourceImpl(
            @Qualifier("AuthenticationServiceRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor) {
        super(TimeUnit.HOURS.toMillis(1), casInterceptor);
    }

    public Future<List<Henkilo>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit) {
        return getWebClient()
                .path(getUrl("authentication-service.s2s.koostepalvelu"))
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .async()
                .post(Entity.entity(henkiloPrototyypit, MediaType.APPLICATION_JSON_TYPE), new GenericType<List<Henkilo>>() {
                });
    }
}
