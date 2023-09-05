package fi.vm.sade.valinta.kooste.configuration;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import fi.vm.sade.javautils.cxf.OphRequestHeadersCxfInterceptor;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.valinta.kooste.ObjectMapperProvider;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import java.util.*;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalConfiguration {

  @Autowired private JacksonJsonProvider jacksonJsonProvider;

  @Autowired private ObjectMapperProvider objectMapperProvider;

  @Autowired private OphRequestHeadersCxfInterceptor<Message> requestHeaders;

  @Bean
  public SpringBus springBus() {
    SpringBus springBus = new SpringBus();
    springBus.setOutInterceptors(List.of(requestHeaders));
    return springBus;
  }

  @Bean
  public OrganisaatioResource organisaatioResource(
      @Value("${valintalaskentakoostepalvelu.organisaatioService.rest.url}") String address) {
    Class cls = OrganisaatioResource.class;
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setInheritHeaders(true);
    bean.setAddress(address);
    bean.setOutInterceptors(List.of(requestHeaders));
    bean.setProviders(List.of(jacksonJsonProvider, objectMapperProvider));
    bean.setServiceClass(cls);
    return (OrganisaatioResource) bean.create(cls);
  }

  @Bean
  public TarjontaPublicService tarjontaPublicService(
      @Value("${valintalaskentakoostepalvelu.tarjontaService.url}") String address) {
    Class cls = TarjontaPublicService.class;
    JaxWsProxyFactoryBean bean = new JaxWsProxyFactoryBean();
    bean.setAddress(address);
    bean.setOutInterceptors(List.of(requestHeaders));
    bean.setServiceClass(cls);
    return (TarjontaPublicService) bean.create(cls);
  }
}
