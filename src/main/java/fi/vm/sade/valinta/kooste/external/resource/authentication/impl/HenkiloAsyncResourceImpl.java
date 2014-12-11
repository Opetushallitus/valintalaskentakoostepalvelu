package fi.vm.sade.valinta.kooste.external.resource.authentication.impl;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.valinta.kooste.external.resource.AsennaCasFilter;
import fi.vm.sade.valinta.kooste.external.resource.authentication.HenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;

/**
 * @author Jussi Jartamo
 */
@Service
public class HenkiloAsyncResourceImpl implements HenkiloAsyncResource {
    private final WebClient webClient;

    @Autowired
    public HenkiloAsyncResourceImpl(@Value("${web.url.cas}") String webCasUrl, @Value("${cas.service.authentication-service}/j_spring_cas_security_check") String targetService, @Value("${valintalaskentakoostepalvelu.app.username.to.haku}") String appClientUsername, @Value("${valintalaskentakoostepalvelu.app.password.to.haku}") String appClientPassword, @Value("${valintalaskentakoostepalvelu.authentication.rest.url}") String address, ApplicationContext context) {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(address);
        bean.setThreadSafe(true);
        List<Object> providers = Lists.newArrayList();
        providers.add(new com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider());
        providers.add(new fi.vm.sade.valinta.kooste.ObjectMapperProvider());
        bean.setProviders(providers);
        AsennaCasFilter.asennaCasFilter(webCasUrl, targetService, appClientUsername, appClientPassword, bean, context);
        this.webClient = bean.createWebClient();
        ClientConfiguration c = WebClient.getConfig(webClient);
        c.getHttpConduit().getClient().setReceiveTimeout(TimeUnit.HOURS.toMillis(1));
        // org.apache.cxf.transport.http.async.SO_TIMEOUT
    }

    public Future<List<Henkilo>> haeTaiLuoHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit) {
        String url = "/resources/s2s/koostepalvelu";
        return WebClient.fromClient(webClient)
            .path(url)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .async()
            .post(Entity.entity(henkiloPrototyypit, MediaType.APPLICATION_JSON_TYPE), new GenericType<List<Henkilo>>() {
        });
    }
}
