package fi.vm.sade.valinta.kooste.external.resource.authentication.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.AsyncResourceWithCas;

@Service
public class HenkiloAsyncResourceImpl extends AsyncResourceWithCas implements HenkiloAsyncResource {
    @Autowired
    public HenkiloAsyncResourceImpl(@Qualifier("AuthenticationServiceRestClientCasInterceptor") AbstractPhaseInterceptor casInterceptor,
                                    @Value("${valintalaskentakoostepalvelu.authentication.rest.url}") String address, ApplicationContext context) {
        super(casInterceptor, address, context, TimeUnit.HOURS.toMillis(1));
    }

    public Future<List<Henkilo>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit) {
        String url = "/resources/s2s/koostepalvelu";
        return getWebClient()
            .path(url)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .async()
            .post(Entity.entity(henkiloPrototyypit, MediaType.APPLICATION_JSON_TYPE), new GenericType<List<Henkilo>>() {
        });
    }
}
