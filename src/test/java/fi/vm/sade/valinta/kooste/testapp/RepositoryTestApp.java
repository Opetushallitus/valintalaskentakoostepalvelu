package fi.vm.sade.valinta.kooste.testapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

/**
 * Spring boot application for running integration tests against the database
 *
 * <p>Located in a separate testapp subpackage so that we avoid component scanning all packages and
 * can thus use mocks where necessary.
 */
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MongoAutoConfiguration.class})
public class RepositoryTestApp {

  public static void start() {
    SpringApplication app = new SpringApplication(RepositoryTestApp.class);
    app.setAllowBeanDefinitionOverriding(true);
    app.run(new String[] {});
  }
}
