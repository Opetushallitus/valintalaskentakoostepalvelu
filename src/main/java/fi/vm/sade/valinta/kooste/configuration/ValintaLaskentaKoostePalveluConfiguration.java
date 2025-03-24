package fi.vm.sade.valinta.kooste.configuration;

import com.fasterxml.jackson.datatype.joda.JodaModule;
import fi.vm.sade.valinta.dokumenttipalvelu.Dokumenttipalvelu;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

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
  @Bean
  public KoosteAudit koosteAudit() {
    return new KoosteAudit();
  }

  @Bean
  public UrlConfiguration urlConfiguration() {
    return UrlConfiguration.getInstance();
  }

  @Bean
  public Dokumenttipalvelu dokumenttipalvelu(
      @Value("${aws.region}") final String region,
      @Value("${aws.bucket.name}") final String bucketName) {
    return new Dokumenttipalvelu(region, bucketName);
  }

  @Bean
  public com.fasterxml.jackson.databind.Module jodaModule() {
    return new JodaModule();
  }
}
