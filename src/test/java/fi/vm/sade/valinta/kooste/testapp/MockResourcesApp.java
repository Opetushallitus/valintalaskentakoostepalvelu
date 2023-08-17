package fi.vm.sade.valinta.kooste.testapp;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.valinta.kooste.App;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Spring boot application for running integration tests that mock classes that expose external
 * services.
 *
 * <p>Located in a separate testapp subpackage so that we avoid component scanning all packages and
 * can thus use mocks where necessary.
 */
@Profile("mockresources")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MongoAutoConfiguration.class})
public class MockResourcesApp {

  public static final int port =
      Integer.parseInt(
          System.getProperty(
              "valintakooste.port", String.valueOf(PortChecker.findFreeLocalPort())));

  private static boolean isRunning = false;

  public static void main(String[] args) {
    start();
  }

  public static void start() {
    if (!isRunning) {
      System.setProperty("server.port", port + "");

      System.setProperty("spring.profiles.active", "mockresources");

      System.setProperty("server.servlet.context-path", App.CONTEXT_PATH);
      SpringApplication app = new SpringApplication(MockResourcesApp.class);
      app.setAllowBeanDefinitionOverriding(true);
      app.run(new String[] {});

      isRunning = true;
    }
  }
}
