package fi.vm.sade.valinta.kooste.erillishaku.resource;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.integrationtest.SharedTomcat;
import fi.vm.sade.valinta.kooste.ValintaKoosteTomcat;

public class ErillishakuResourceTest {
    String hakuOid = "1.2.246.562.5.2013080813081926341928";
    String hakukohdeOid = "1.2.246.562.5.72607738902";
    String hakemusOid = "1.2.246.562.11.00000441369";
    String tarjoajaOid = "1.2.246.562.10.591352080610";
    String valintatapajonoOid = "14090336922663576781797489829886";
    String hakijaOid = "1.2.246.562.24.14229104472";


    @Before
    public void startServer() {
        ValintaKoosteTomcat.startShared();
    }
    @Test
    public void smokeTest() {
        final String url = "http://localhost:" + SharedTomcat.port + "/valintalaskentakoostepalvelu/resources/erillishaku/vienti";
        final Response response = createClient(url)
            .query("hakutyyppi", "KORKEAKOULU")
            .query("hakuOid", hakuOid)
            .query("hakikohdeOid", hakukohdeOid)
            .query("tarjoajaOid", tarjoajaOid)
            .query("valintatapajonoOid", valintatapajonoOid)
            .query("valintatapajononNimi", "varsinainen jono")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(Arrays.asList(), MediaType.APPLICATION_JSON));
        assertEquals(200, response.getStatus());

    }

    private WebClient createClient(String url) {
        return new HttpResource(url, 1000).webClient;
    }

}
