package fi.vm.sade.valinta.kooste;

import com.google.common.io.Files;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;
import fi.vm.sade.integrationtest.util.SpringProfile;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.sharedutils.FakeAuthenticationInitialiser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import javax.servlet.DispatcherType;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jussi Jartamo
 *     <p>Tuotantoa vastaava konfiguraatio
 */
public class ValintalaskentakoostepalveluJetty {
  private static final Logger LOG =
      LoggerFactory.getLogger(ValintalaskentakoostepalveluJetty.class);

  public static final int port;
  private static Server server;

  static {
    try {
      // -Dport=8090
      Integer portFlagOrZero =
          Integer.parseInt(Optional.ofNullable(System.getProperty("port")).orElse("0"));
      ServerSocket s = new ServerSocket(portFlagOrZero);
      port = s.getLocalPort();
      s.close();
      server = new Server(port);
      LOG.info("Starting server to port {}", port);
    } catch (IOException e) {
      throw new RuntimeException("free port not found");
    }
  }

  public static void main(String[] args) {
    startShared();
    // MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_VALINTAPERUSTEET_READ_1.2.246.562.10.00000000001");
  }

  public static final String resourcesAddress =
      "http://localhost:"
          + ValintalaskentakoostepalveluJetty.port
          + "/valintalaskentakoostepalvelu/resources";

  private static void mockUserHomeWithCommonProperties() {
    try {
      final File tempDir = Files.createTempDir();
      final File ophConfiguration = new File(tempDir, "oph-configuration");
      ophConfiguration.mkdir();
      final File commonProperties = new File(ophConfiguration, "common.properties");
      final FileOutputStream output = new FileOutputStream(commonProperties);
      System.setProperty("user.home", tempDir.getAbsolutePath());
      LOG.info("Set user.home to " + tempDir.getAbsolutePath() + " .");
      LOG.info("Writing properties to " + commonProperties.getAbsolutePath() + " ...");
      IOUtils.writeLines(
          Collections.singletonList("web.url.cas=some_cas_url"), System.lineSeparator(), output);
      LOG.info(
          "...successfully wrote to properties file in "
              + commonProperties.getAbsolutePath()
              + " the following content: "
              + IOUtils.readLines(new FileReader(commonProperties)));
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void startShared() {
    FakeAuthenticationInitialiser.fakeAuthentication();
    Integraatiopalvelimet.mockServer.reset();
    SpringProfile.setProfile("test");
    mockUserHomeWithCommonProperties();
    int maxTriesToStart = 10;
    MockOpintopolkuCasAuthenticationFilter.clear();
    UrlConfiguration.getInstance()
        .addOverride("url-virkailija", Integraatiopalvelimet.mockServer.getUrl())
        .addOverride("url-ilb", Integraatiopalvelimet.mockServer.getUrl())
        .addOverride(
            "valintalaskentakoostepalvelu.valintalaskenta-laskenta-service.baseurl",
            Integraatiopalvelimet.mockServer.getUrl())
        .addOverride(
            "valintalaskentakoostepalvelu.valintaperusteet-service.baseurl",
            Integraatiopalvelimet.mockServer.getUrl());
    if (server.isStopped()) {
      int startTriesLeft = maxTriesToStart;
      boolean startSucceeded = false;
      while (startTriesLeft-- > 0 && !startSucceeded) {
        try {
          startServer();
          startSucceeded = true;
        } catch (Exception e) {
          System.err.println(
              ValintalaskentakoostepalveluJetty.class.getName()
                  + " Warning: could not start server, trying again "
                  + startTriesLeft
                  + " times. Exception was:");
          e.printStackTrace();
        }
      }
      if (!startSucceeded) {
        throw new IllegalStateException(
            "Could not get the server started with " + maxTriesToStart + " attempts.");
      }
    }
  }

  private static void startServer() {
    KoosteTestProfileConfiguration.PROXY_SERVER.set(
        Integraatiopalvelimet.mockServer.getHost()
            + ":"
            + Integraatiopalvelimet.mockServer.getPort());
    String root = ProjectRootFinder.findProjectRoot().toString();
    WebAppContext wac = new WebAppContext();
    wac.addFilter(
        MockOpintopolkuCasAuthenticationFilter.class,
        "/*",
        EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.INCLUDE));
    wac.setResourceBase(root + "/src/main/resources/webapp");
    KoosteProductionJetty.JETTY.start(wac, server, KoosteProductionJetty.contextPath);
  }
}
