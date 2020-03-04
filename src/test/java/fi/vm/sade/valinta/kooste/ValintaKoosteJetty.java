package fi.vm.sade.valinta.kooste;

import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;
import fi.vm.sade.valinta.sharedutils.FakeAuthenticationInitialiser;
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
 *            LocalForward 18888 alb.testiopintopolku.fi:80
 *
 *    * add a fully qualified domain name to /etc/hosts
 *        127.0.0.1       kehittajan-oma-kone.testiopintopolku.fi
 *
 *    * copy oph-configuration directory from the container of the target environment (pallero) to your machine
 *        * you need at least common.properties, ehcache.xml and security-context-backend.xml
 *
 *    * tweak your oph-configuration for local usage as follows:
 *        diff oph-configuration.qa/common.properties oph-configuration/common.properties
 *        14,15c14,15
 *        < host.ilb=https://virkailija.testiopintopolku.fi
 *        < host.alb=http://alb.testiopintopolku.fi
 *        ---
 *        > host.ilb=http://localhost:18888
 *        > host.alb=http://kehittajan-oma-kone.testiopintopolku.fi:18888
 *        82c82
 *        < cas.service.valintalaskentakoostepalvelu=https://${host.virkailija}/valintalaskentakoostepalvelu
 *        ---
 *        > cas.service.valintalaskentakoostepalvelu=http://localhost:56748/valintalaskentakoostepalvelu
 *        128c128
 *        < valintalaskentakoostepalvelu.maxWorkerCount=4
 *        ---
 *        > valintalaskentakoostepalvelu.maxWorkerCount=0
 *
 *    * If you want to use local valintalaskenta, configure it to common.properties like this:
 *
 *       valintalaskentakoostepalvelu.valintalaskenta-laskenta-service.baseurl=http://localhost:8080
 *
 *    * Run this class directly, or with https://github.com/Opetushallitus/local-environment
 *
 *    * Now the swagger documentation for this service is running at
 *        http://localhost:56748/valintalaskentakoostepalvelu/swagger/
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
                String root =  ProjectRootFinder.findProjectRoot().toString();
                WebAppContext wac = new WebAppContext();
                if (useMocks) {
                    wac.setResourceBase(root);
                    wac.setDescriptor(root + "/src/test/resources/it-profile-web.xml");
                } else {
                    wac.setResourceBase(root + "/target/classes/webapp");
                }
                KoosteProductionJetty.JETTY.start(wac, server, KoosteProductionJetty.contextPath);
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
