package fi.vm.sade.valinta.kooste.configuration;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ServletContainerConfiguration {

  /**
   * Konfiguraatio kun palvelua ajetaan HTTPS proxyn läpi. Käytännössä tämä muuttaa {@link
   * javax.servlet.ServletRequest#getScheme()} palauttamaan `https` jolloin palvelun kautta luodut
   * urlit muodostuvat oikein.
   *
   * @return EmbeddedServletContainerCustomizer jonka Spring automaattisesti tunnistaa ja lisää
   *     servlet containerin konfigurointiin
   */
  @Bean
  @Profile("!dev")
  public WebServerFactoryCustomizer sslProxyCustomizer() {
    return (WebServerFactory container) -> {
      if (container instanceof ConfigurableServletWebServerFactory) {
        TomcatServletWebServerFactory tomcat = (TomcatServletWebServerFactory) container;
        tomcat.addConnectorCustomizers(
            (Connector connector) -> {
              connector.setScheme("https");
              connector.setSecure(true);
            });
      }
    };
  }
}
