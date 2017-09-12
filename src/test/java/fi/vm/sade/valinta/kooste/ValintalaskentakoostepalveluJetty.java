package fi.vm.sade.valinta.kooste;

import com.google.common.io.Files;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;
import fi.vm.sade.integrationtest.util.SpringProfile;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;

/**
 * @author Jussi Jartamo
 *
 * Tuotantoa vastaava konfiguraatio
 */
public class ValintalaskentakoostepalveluJetty {
    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentakoostepalveluJetty.class);

    public final static int port;
    private static Server server;

    static {
        try {
            // -Dport=8090
            Integer portFlagOrZero = Integer.parseInt(Optional.ofNullable(System.getProperty("port")).orElse("0"));
            ServerSocket s = new ServerSocket(portFlagOrZero);
            port = s.getLocalPort();
            s.close();
            server = new Server(port);
            LOG.info("Starting server to port {}", port);
        } catch (IOException e) {
            throw new RuntimeException("free port not found");
        }
    }

    public static void main(String[] args) throws Exception{
        startShared();
        //MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_VALINTAPERUSTEET_READ_1.2.246.562.10.00000000001");
    }

    public final static String resourcesAddress = "http://localhost:" + ValintalaskentakoostepalveluJetty.port + "/valintalaskentakoostepalvelu/resources";
    private static void mockUserHomeWithCommonProperties() {
        try {
            final File tempDir = Files.createTempDir();
            final File ophConfiguration = new File(tempDir,"oph-configuration");
            ophConfiguration.mkdir();
            final File commonProperties = new File(ophConfiguration, "common.properties");
            final FileOutputStream output = new FileOutputStream(commonProperties);
            System.setProperty("user.home", tempDir.getAbsolutePath());
            IOUtils.writeLines(Arrays.asList("web.url.cas", "some_cas_url"),System.lineSeparator(),output);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void startShared() {
        Integraatiopalvelimet.mockServer.reset();
        SpringProfile.setProfile("test");
        mockUserHomeWithCommonProperties();
        int maxTriesToStart = 10;
        if (server.isStopped()) {
            int startTriesLeft = maxTriesToStart;
            boolean startSucceeded = false;
            while (startTriesLeft-- > 0 && !startSucceeded) {
                try {
                    startServer();
                    startSucceeded = true;
                } catch (Exception e) {
                    System.err.println(ValintalaskentakoostepalveluJetty.class.getName() +
                        " Warning: could not start server, trying again " + startTriesLeft + " times. Exception was:");
                    e.printStackTrace();
                }
            }
            if (!startSucceeded) {
                throw new IllegalStateException("Could not get the server started with " + maxTriesToStart + " attempts.");
            }
        }
        MockOpintopolkuCasAuthenticationFilter.clear();
        UrlConfiguration.getInstance()
                .addOverride("url-virkailija", Integraatiopalvelimet.mockServer.getUrl())
                .addOverride("url-ilb", Integraatiopalvelimet.mockServer.getUrl());
    }

    private static void startServer() throws Exception {
        KoosteTestProfileConfiguration.PROXY_SERVER.set(Integraatiopalvelimet.mockServer.getHost() + ":" + Integraatiopalvelimet.mockServer.getPort());
        String root =  ProjectRootFinder.findProjectRoot() + "/valintalaskentakoostepalvelu";
        WebAppContext wac = new WebAppContext();
        wac.addFilter(MockOpintopolkuCasAuthenticationFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.INCLUDE));
        wac.setResourceBase(root + "/src/main/webapp");
        wac.setContextPath("/valintalaskentakoostepalvelu");
        wac.setParentLoaderPriority(true);
        server.setHandler(wac);
        server.setStopAtShutdown(true);
        server.start();
    }

}
