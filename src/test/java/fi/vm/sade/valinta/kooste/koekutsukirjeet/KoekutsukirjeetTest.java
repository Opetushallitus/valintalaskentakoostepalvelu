package fi.vm.sade.valinta.kooste.koekutsukirjeet;

import fi.vm.sade.integrationtest.tomcat.SharedTomcat;
import fi.vm.sade.valinta.http.HttpResource;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Jussi Jartamo
 */
public class KoekutsukirjeetTest {
    final static Logger LOG = LoggerFactory.getLogger(KoekutsukirjeetTest.class);
    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec

    /*
    final String root = "http://localhost:" + SharedTomcat.port + "/valintalaskentakoostepalvelu/resources";
    String hakuOid = "1.2.246.562.5.2013080813081926341928";
    String hakukohdeOid = "1.2.246.562.5.72607738902";
    String tarjoajaOid = "1.2.246.562.10.591352080610";
    String valintatapajonoOid = "14090336922663576781797489829886";
    final HttpResource jsonResource = new HttpResource(root + "/erillishaku/tuonti/json");
    final HttpResource excelResource = new HttpResource(root + "/erillishaku/tuonti");
    final HttpResource prosessiResource = new HttpResource(root + "/dokumenttiprosessi/");
    */

    @Before
    public void startServer() {
        //ValintaKoosteTomcat.startShared();
    }

    public void kaikkiKutsutaanHakijanValinta() {

    }

}
