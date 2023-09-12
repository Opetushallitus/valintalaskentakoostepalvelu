package fi.vm.sade.valinta.kooste.configuration;

import ch.qos.logback.access.tomcat.LogbackValve;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class AccessLogConfiguration {

  private static final String CONFIG_FILE = "logback-access.xml";

  @Bean
  @ConditionalOnProperty(name = "logback.access")
  public WebServerFactoryCustomizer containerCustomizer(
      @Value("${logback.access:}") final String path) {
    return container -> {
      if (container instanceof TomcatServletWebServerFactory) {
        ((TomcatServletWebServerFactory) container)
            .addContextCustomizers(
                context -> {
                  try {
                    // LogbackValve suostuu lukemaan konfiguraation vain classpathilta tai
                    // tomcat.base/tomcat.home -hakemistoista, joten kopioidaan
                    Files.copy(
                        Paths.get(path),
                        Paths.get(context.getCatalinaBase() + "/" + CONFIG_FILE),
                        StandardCopyOption.REPLACE_EXISTING);
                  } catch (IOException e) {
                    throw new RuntimeException(e);
                  }

                  LogbackValve logbackValve = new LogbackValve();
                  logbackValve.setFilename(CONFIG_FILE);
                  context.getPipeline().addValve(logbackValve);
                });
      }
    };
  }
}
