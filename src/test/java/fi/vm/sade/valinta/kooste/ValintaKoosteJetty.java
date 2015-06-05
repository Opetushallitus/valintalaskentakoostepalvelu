package fi.vm.sade.valinta.kooste;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;

/**
 *  Starts valintalaskentakoostepalvelu locally.
 *
 *  Use -Dvalintakooste.profile=it for local mocks.
 */
public class ValintaKoosteJetty {
    public final static int port = Integer.parseInt(System.getProperty("valintakooste.port", String.valueOf(PortChecker.findFreeLocalPort())));
    private static Server server = new Server(port);

    public static void main(String[] args) throws Exception{
        startShared("it".equals(System.getProperty("valintakooste.profile")));
    }

    public static void startShared() {
        startShared(true);
    }

    public static void startShared(boolean useMocks) {
        try {
            if (server.isStopped()) {
                String root =  ProjectRootFinder.findProjectRoot() + "/valintalaskentakoostepalvelu";
                WebAppContext wac = new WebAppContext();
                if (useMocks) {
                    wac.setResourceBase(root);
                    wac.setDescriptor(root + "/src/test/resources/it-profile-web.xml");
                } else {
                    wac.setResourceBase(root + "/src/main/webapp");
                }

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
