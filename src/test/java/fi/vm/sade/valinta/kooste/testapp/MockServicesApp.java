package fi.vm.sade.valinta.kooste.testapp;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.valinta.kooste.App;
import fi.vm.sade.valinta.kooste.Integraatiopalvelimet;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;

/**
 * Starts a spring boot application for running integration tests that call external services mocked
 * with a mock server.
 */
public class MockServicesApp {

  public static final int port =
      Integer.parseInt(
          System.getProperty(
              "valintakooste.port", String.valueOf(PortChecker.findFreeLocalPort())));

  public static final String resourcesAddress =
      "http://localhost:" + port + "/valintalaskentakoostepalvelu/resources";

  private static boolean isRunning = false;

  public static void main(String[] args) {
    start();
  }

  public static void start() {
    System.setProperty("mockserver.logLevel", "WARN");
    Integraatiopalvelimet.mockServer.reset();

    if (isRunning) {
      return;
    }
    isRunning = true;

    UrlConfiguration.getInstance()
        .addOverride("url-virkailija", Integraatiopalvelimet.mockServer.getUrl())
        .addOverride("url-ilb", Integraatiopalvelimet.mockServer.getUrl())
        .addOverride(
            "valintalaskentakoostepalvelu.valintalaskenta-laskenta-service.baseurl",
            Integraatiopalvelimet.mockServer.getUrl())
        .addOverride(
            "valintalaskentakoostepalvelu.valintaperusteet-service.baseurl",
            Integraatiopalvelimet.mockServer.getUrl());

    System.setProperty("server.port", port + "");
    System.setProperty("spring.profiles.active", "mockservices");

    App.start();
  }
}
