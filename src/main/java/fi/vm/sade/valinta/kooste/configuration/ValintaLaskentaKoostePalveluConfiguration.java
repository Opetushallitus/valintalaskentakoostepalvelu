package fi.vm.sade.valinta.kooste.configuration;

import fi.vm.sade.javautils.cxf.OphRequestHeadersCxfInterceptor;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.ObjectMapperProvider;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
@EnableAsync
@PropertySource(
    value = {
      "classpath:META-INF/kela.properties",
      "classpath:META-INF/valintalaskentakoostepalvelu.properties",
      "file:///${user.home:''}/oph-configuration/common.properties",
      "file:///${user.home:''}/oph-configuration/valinta.properties",
      "file:///${user.home:''}/oph-configuration/valintalaskentakoostepalvelu.properties",
      "file:///${user.home:''}/oph-configuration/override.properties"
    },
    ignoreResourceNotFound = true)
public class ValintaLaskentaKoostePalveluConfiguration {

  @Bean(name = "ophRequestHeaders")
  public OphRequestHeadersCxfInterceptor ophRequestHeadersCxfInterceptor() {
    return new OphRequestHeadersCxfInterceptor(
        "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu");
  }

  @Bean
  public ObjectMapperProvider objectMapperProvider() {
    return new ObjectMapperProvider();
  }

  @Bean
  public KoosteAudit koosteAudit() {
    return new KoosteAudit();
  }

  @Bean
  public UrlConfiguration urlConfiguration() {
    return UrlConfiguration.getInstance();
  }
}
