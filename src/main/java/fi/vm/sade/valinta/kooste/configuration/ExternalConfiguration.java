package fi.vm.sade.valinta.kooste.configuration;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import fi.vm.sade.javautils.cxf.OphRequestHeadersCxfInterceptor;
import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.valinta.kooste.ObjectMapperProvider;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintatietoResource;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import java.util.*;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalConfiguration {

  @Autowired private JacksonJsonProvider jacksonJsonProvider;

  @Autowired private ObjectMapperProvider objectMapperProvider;

  @Autowired private OphRequestHeadersCxfInterceptor<Message> requestHeaders;

  @Autowired
  @Qualifier("SeurantaRestClientCasInterceptor")
  public AbstractPhaseInterceptor<Message> seurantaRestClientCasInterceptor;

  @Autowired
  @Qualifier("viestintapalveluClientCasInterceptor")
  public AbstractPhaseInterceptor<Message> viestintapalveluClientCasInterceptor;

  @Autowired
  @Qualifier("HakemusServiceRestClientAsAdminCasInterceptor")
  public AbstractPhaseInterceptor<Message> hakemusServiceRestClientAsAdminCasInterceptor;

  @Autowired
  @Qualifier("ValintalaskentaCasInterceptor")
  public AbstractPhaseInterceptor<Message> valintalaskentaCasInterceptor;

  @Autowired
  @Qualifier("koodiServiceCasInterceptor")
  public AbstractPhaseInterceptor<Message> koodiServiceCasInterceptor;

  @Bean
  public SpringBus springBus() {
    SpringBus springBus = new SpringBus();
    springBus.setOutInterceptors(List.of(requestHeaders));
    return springBus;
  }

  @Bean
  public SijoittelunSeurantaResource sijoittelunSeurantaResource(
      @Value("${valintalaskentakoostepalvelu.seuranta.rest.url}") String address) {
    Class cls = SijoittelunSeurantaResource.class;
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setInheritHeaders(true);
    bean.setAddress(address);
    bean.setOutInterceptors(List.of(seurantaRestClientCasInterceptor, requestHeaders));
    bean.setInInterceptors(List.of(seurantaRestClientCasInterceptor));
    bean.setProviders(List.of(jacksonJsonProvider, objectMapperProvider));
    bean.setServiceClass(cls);
    return (SijoittelunSeurantaResource) bean.create(cls);
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
  ViestintapalveluResource viestintapalveluResource(
      @Value("${valintalaskentakoostepalvelu.viestintapalvelu.url}") String address) {
    Class cls = ViestintapalveluResource.class;
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setInheritHeaders(true);
    bean.setAddress(address);
    bean.setOutInterceptors(List.of(viestintapalveluClientCasInterceptor, requestHeaders));
    bean.setInInterceptors(List.of(viestintapalveluClientCasInterceptor));
    bean.setProviders(List.of(jacksonJsonProvider, objectMapperProvider));
    bean.setServiceClass(cls);
    return (ViestintapalveluResource) bean.create(cls);
  }

  @Bean
  public ApplicationResource applicationResource(
      @Value("${valintalaskentakoostepalvelu.hakemus.rest.url}") String address) {
    Class cls = ApplicationResource.class;
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setInheritHeaders(true);
    bean.setAddress(address);
    bean.setOutInterceptors(List.of(hakemusServiceRestClientAsAdminCasInterceptor, requestHeaders));
    bean.setInInterceptors(List.of(hakemusServiceRestClientAsAdminCasInterceptor));
    bean.setProviders(List.of(jacksonJsonProvider, objectMapperProvider));
    bean.setServiceClass(cls);
    return (ApplicationResource) bean.create(cls);
  }

  @Bean
  public HakukohdeResource hakukohdeResource(@Value("https://${host.virkailija}") String address) {
    Class cls = HakukohdeResource.class;
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setInheritHeaders(true);
    bean.setAddress(address);
    bean.setOutInterceptors(List.of(valintalaskentaCasInterceptor, requestHeaders));
    bean.setInInterceptors(List.of(valintalaskentaCasInterceptor));
    bean.setProviders(List.of(jacksonJsonProvider, objectMapperProvider));
    bean.setServiceClass(cls);
    return (HakukohdeResource) bean.create(cls);
  }

  @Bean
  public ValintatietoResource valintatietoResource(
      @Value("${valintalaskentakoostepalvelu.valintalaskenta.rest.url}") String address) {
    Class cls = ValintatietoResource.class;
    JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
    bean.setInheritHeaders(true);
    bean.setAddress(address);
    bean.setOutInterceptors(List.of(valintalaskentaCasInterceptor, requestHeaders));
    bean.setInInterceptors(List.of(valintalaskentaCasInterceptor));
    bean.setProviders(List.of(jacksonJsonProvider, objectMapperProvider));
    bean.setServiceClass(cls);
    return (ValintatietoResource) bean.create(cls);
  }

  @Bean
  public KoodiService koodiService(
      @Value("${valintalaskentakoostepalvelu.koodiService.url}") String address) {
    Class cls = KoodiService.class;
    JaxWsProxyFactoryBean bean = new JaxWsProxyFactoryBean();
    bean.setAddress(address);
    bean.setOutInterceptors(List.of(koodiServiceCasInterceptor, requestHeaders));
    bean.setInInterceptors(List.of(koodiServiceCasInterceptor));
    bean.setServiceClass(cls);
    return (KoodiService) bean.create(cls);
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
