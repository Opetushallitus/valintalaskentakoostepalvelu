package fi.vm.sade.valinta.kooste.proxy.resource.hakemus;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.Integraatiopalvelimet;
import fi.vm.sade.valinta.kooste.server.SeurantaServerMock;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.Response;
import java.io.InputStream;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnString;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static javax.ws.rs.HttpMethod.GET;
import static junit.framework.Assert.assertEquals;

public class OmatSivutHakemusResourceTest {
    private static final String hakemusOid = "1.2.246.562.11.00003935855";
    private static final String hakuOid = "1.2.246.562.29.11735171271";
    public static final String PROXY_VALINTA_TULOS_SERVICE_JSON = "/proxy/vts/1.2.246.562.11.00003935855.json";

    @Before
    public void startServer() {
        startShared();
    }

    @After
    public void reset() {
        Integraatiopalvelimet.mockServer.reset();
    }

    @Test
    public void hakemusResourceTest() throws Exception {
        final String valintatulos = classpathResourceAsString(PROXY_VALINTA_TULOS_SERVICE_JSON);
        mockToReturnString(GET, "/valinta-tulos-service/haku/" + hakuOid + "/hakemus/" + hakemusOid, valintatulos);
        final HttpResource proxyResource = new HttpResource(resourcesAddress + "/proxy/valintatulos/haku/" + hakuOid + "/hakemusOid/" + hakemusOid);
        Response response = proxyResource.getWebClient().get();
        assertEquals(200, response.getStatus());
        assertEquals(valintatulos, IOUtils.toString((InputStream) response.getEntity()));
    }

    private static String classpathResourceAsString(String path) throws Exception {
        return IOUtils.toString(new ClassPathResource(path).getInputStream());
    }

}
