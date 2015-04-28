package fi.vm.sade.valinta.kooste.koekutsukirjeet;

import fi.vm.sade.integrationtest.tomcat.SharedTomcat;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Jussi Jartamo
 */
public class KoekutsukirjeetTest {
    final static Logger LOG = LoggerFactory.getLogger(KoekutsukirjeetTest.class);
    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource koekutsukirjeResource = new HttpResource(root + "/viestintapalvelu/koekutsukirjeet/aktivoi");

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void kaikkiKutsutaanHakijanValinta() {
        /*
        @QueryParam(OPH.HAKUOID) String hakuOid,
        @QueryParam(OPH.HAKUKOHDEOID) String hakukohdeOid,
        @QueryParam(OPH.TARJOAJAOID) String tarjoajaOid,
        @QueryParam("templateName") String templateName,
        @QueryParam("valintakoeOids") List<String> valintakoeOids,
        DokumentinLisatiedot hakemuksillaRajaus)
        */
        Response r =
        koekutsukirjeResource.getWebClient()
                .query("hakuOid", "H0")
                .query("hakukohdeOid","HK0")
                .query("tarjoajaOid", "T0")
                .query("templateName", "tmpl")
                .query("valintakoeOids", "VK0")
        .post(Entity.json(new DokumentinLisatiedot("tag", "Letterbodytext", "FI", Arrays.asList(), Arrays.asList())));
        Assert.assertEquals(200, r.getStatus());
    }

}
