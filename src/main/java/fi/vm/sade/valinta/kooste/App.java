package fi.vm.sade.valinta.kooste;

import java.io.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, MongoAutoConfiguration.class})
public class App {

  public static final String CONTEXT_PATH = "/valintalaskentakoostepalvelu";

  public static void main(String[] args) {
    start();
  }

  public static void start() {
    System.setProperty("server.servlet.context-path", CONTEXT_PATH);
    System.setProperty("spring.task.scheduling.pool.size", "10");

    SpringApplication app = new SpringApplication(App.class);
    app.setAllowBeanDefinitionOverriding(true);
    app.run(new String[] {});
  }
}
