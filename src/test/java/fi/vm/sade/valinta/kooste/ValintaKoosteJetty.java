package fi.vm.sade.valinta.kooste;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;
import fi.vm.sade.sharedutils.FakeAuthenticationInitialiser;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 *  Starts valintalaskentakoostepalvelu locally.
 *
 *  Use -Dvalintakooste.profile=it for local mocks.
 *  It is practical to set a stable HTTP port with e.g. -Dvalintakooste.port=56748
 *
 *  Remote usage with a test environment:
 *
 *    * add necessary tunnels to your test environments, e.g. have something like this in you ~/.ssh/config :
 *        Host bastion.pallero
 *            HostName bastion.testiopintopolku.fi
 *            ...
 *            LocalForward 2389 ldap.ldap.testiopintopolku.fi:389
 *            LocalForward 18888 alb.testiopintopolku.fi:80
 *
 *    * copy oph-configuration directory from the container of valinta to your machine
 *
 *    * tweak your oph-configuration for local usage as follows:
 *        diff -r valinta-pallero-oph-configuration/common.properties local-valinta-pallero-oph-configuration/common.properties
 *        12a13
 *        14,16c15,19
 *        < host.ilb=https://virkailija.testiopintopolku.fi
 *        < host.alb=http://alb.testiopintopolku.fi
 *        < host.ldap=ldap.ldap.testiopintopolku.fi
 *        ---
 *        > host.ilb=http://localhost:18888
 *        > host.alb=http://localhost:18888
 *        > host.ldap=localhost:2389
 *        84c87,88
 *        < cas.service.valintalaskentakoostepalvelu=https://${host.virkailija}/valintalaskentakoostepalvelu
 *        ---
 *        > cas.service.valintalaskentakoostepalvelu=http://localhost:56748/valintalaskentakoostepalvelu
 *        115a120
 *        > jatkuvasijoittelu.autostart=false
 *        120c125
 *        < valintalaskentakoostepalvelu.valintalaskentaService.url=https://${host.virkailija}/valintalaskenta-laskenta-service/services/valintalaskentaService
 *        ---
 *        > valintalaskentakoostepalvelu.valintalaskentaService.url=${host.ilb}/valintalaskenta-laskenta-service/services/valintalaskentaService
 *        143c148
 *        < valintalaskentakoostepalvelu.maxWorkerCount=4
 *        ---
 *        > valintalaskentakoostepalvelu.maxWorkerCount=0
 *
 *
 *    * If something does not work as expected, make it work and improve this documentation accordingly ;)
 */
public class ValintaKoosteJetty {
    public final static int port = Integer.parseInt(System.getProperty("valintakooste.port", String.valueOf(PortChecker.findFreeLocalPort())));
    private static Server server = new Server(port);

    public static void main(String[] args) {
        startShared("it".equals(System.getProperty("valintakooste.profile")));
    }

    public static void startShared() {
        startShared(true);
    }

    public static void startShared(boolean useMocks) {
        try {
            if (useMocks) {
                FakeAuthenticationInitialiser.fakeAuthentication();
            }
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
