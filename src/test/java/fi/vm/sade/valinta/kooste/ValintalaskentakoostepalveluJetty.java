package fi.vm.sade.valinta.kooste;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;
import fi.vm.sade.integrationtest.util.SpringProfile;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.*;
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
    }

    public final static String resourcesAddress = "http://localhost:" + ValintalaskentakoostepalveluJetty.port + "/valintalaskentakoostepalvelu/resources";

    public static void startShared() {
        Integraatiopalvelimet.mockServer.reset();
        SpringProfile.setProfile("test");
        try {
            if (server.isStopped()) {
                KoosteTestProfileConfiguration.PROXY_SERVER.set(Integraatiopalvelimet.mockServer.getHost() + ":" + Integraatiopalvelimet.mockServer.getPort());
                String root =  ProjectRootFinder.findProjectRoot() + "/valintalaskentakoostepalvelu";
                WebAppContext wac = new WebAppContext();

                wac.setResourceBase(root + "/src/main/webapp");
                wac.setContextPath("/valintalaskentakoostepalvelu");
                wac.setParentLoaderPriority(true);
                server.setHandler(wac);
                server.setStopAtShutdown(true);
                server.start();
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
