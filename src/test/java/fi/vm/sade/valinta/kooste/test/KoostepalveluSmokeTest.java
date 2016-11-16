package fi.vm.sade.valinta.kooste.test;

import fi.vm.sade.integrationtest.tomcat.EmbeddedTomcat;
import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.integrationtest.util.ProjectRootFinder;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

/**
 * @author Jussi Jartamo
 *
 * @Ignore Johtui Glassfishistä, minkä käytöstä on luovuttu. Ignoreen koska hidas testi eikä ole uusiutumassa, ellei implementaatiota taas vaihdeta.
 */
@Ignore
public class KoostepalveluSmokeTest {
    static final Logger LOG = LoggerFactory.getLogger(KoostepalveluSmokeTest.class);
    static final String VALINTAKOOSTE_MODULE_ROOT = ProjectRootFinder.findProjectRoot() + "/valintalaskentakoostepalvelu/smoketest";
    static final String VALINTAKOOSTE_CONTEXT_PATH = "/valintalaskentakoostepalvelu";
    final int port = PortChecker.findFreeLocalPort();
    private EmbeddedTomcat tomcat;
    @Before
    public void start() {
        tomcat = new EmbeddedTomcat(port, VALINTAKOOSTE_MODULE_ROOT, VALINTAKOOSTE_CONTEXT_PATH);
        tomcat.start();
    }
    @After
    public void stop() {
        tomcat.stop();
    }

    @Test
    public void testSpringSecurity() throws Exception {
        //TimeUnit.DAYS.sleep(1L);
        final String root = "http://localhost:" + port + "/valintalaskentakoostepalvelu/resources";
        final HttpResource smoketestResource = new HttpResourceBuilder()
                .address(root + "/smoketest")
                .build();
        {
            Response response = smoketestResource.getWebClient().path("/unsecured_service_call").get();
            Assert.assertEquals(200, response.getStatus());
            // TESTATAAN VIELA ETTA KUTSU MENI LAPI
            Assert.assertEquals(1, SmoketestResource.UNSECURED_SERVICE_CALL_COUNTER.get());
        }
        {
            Response response = smoketestResource.getWebClient().path("/secured_service_call").get();
            Assert.assertEquals(401, response.getStatus()); // NOT AUTHENTICATED
            // TESTATAAN VIELA ETTA KUTSU EI MENE PERILLE
            Assert.assertEquals(0, SmoketestResource.SECURED_SERVICE_CALL_COUNTER.get());
        }
    }
}
