package fi.vm.sade.valinta.kooste;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * @author Jussi Jartamo
 */
public class ValintaKoosteJetty {
    public final static int port = PortChecker.findFreeLocalPort();
    private static Server server = new Server(port);

    public static void main(String[] args) throws Exception{
        startShared();
    }
    public static void startShared() {
        try {
            if (server.isStopped()) {
                String root = ProjectRootFinder.findProjectRoot() + "/valintalaskentakoostepalvelu";
                WebAppContext wac = new WebAppContext();
                wac.setResourceBase(root);
                wac.setDescriptor(root + "/src/test/resources/it-profile-web.xml");
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

    public static void stop() throws Exception {
        server.stop();
    }

}
