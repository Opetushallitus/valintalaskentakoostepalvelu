package fi.vm.sade.valinta.kooste;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;
import fi.vm.sade.integrationtest.util.SpringProfile;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.*;
/**
 * @author Jussi Jartamo
 *
 * Tuotantoa vastaava konfiguraatio
 */
public class ValintalaskentakoostepalveluJetty {

    public final static int port = PortChecker.findFreeLocalPort();
    private static Server server = new Server(port);

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
