package fi.vm.sade.valinta.kooste.erillishaku.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.integrationtest.tomcat.SharedTomcat;
import fi.vm.sade.valinta.kooste.ValintaKoosteTomcat;
import static fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi.*;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuJson;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.*;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ImportedErillisHakuExcel;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.mocks.MockData;
import fi.vm.sade.valinta.kooste.mocks.MockDokumenttiResource;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Jussi Jartamo
 */
public class ErillishakuResourceKayttajaPalauteTest {
    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final static Logger LOG = LoggerFactory.getLogger(ErillishakuResourceKayttajaPalauteTest.class);
    final String root = "http://localhost:" + SharedTomcat.port + "/valintalaskentakoostepalvelu/resources";
    final String url = root + "/erillishaku/tuonti/json";
    String hakuOid = "1.2.246.562.5.2013080813081926341928";
    String hakukohdeOid = "1.2.246.562.5.72607738902";
    String tarjoajaOid = "1.2.246.562.10.591352080610";
    String valintatapajonoOid = "14090336922663576781797489829886";
    final HttpResource erillishakuResource = new HttpResource(url);
    final HttpResource prosessiResource = new HttpResource(root + "/dokumenttiprosessi/");

    @Before
    public void startServer() {
        ValintaKoosteTomcat.startShared();
    }

    @Test
    public void tuontiVirheTyhjaTietoJoukkoPalaute() throws IOException {
        final ProsessiId prosessiId =
                erillishaunWebClient()
                              .post(Entity.json(new ErillishakuJson(emptyList()))

                                      //
                                      ,ProsessiId.class);

        Prosessi valmisProsessi = odotaPaluuarvoTaiEpaonnistuTimeouttiin(prosessiId);
        assertTrue(valmisProsessi.poikkeukset.contains(Poikkeus.koostepalvelupoikkeus(POIKKEUS_TYHJA_DATAJOUKKO)));
//        LOG.error("VALMIS {}", new GsonBuilder().setPrettyPrinting().create().toJson(valmisProsessi));
  }

    /*
    @Test
    public void tuontiVirheitaTuoduissaTiedoissaPalaute() {



    }
    @Test
    public void tuontiVirheHenkilopalveluKutsussaPalaute() {

    }*/

    private Prosessi odotaPaluuarvoTaiEpaonnistuTimeouttiin(final ProsessiId prosessiId) {
        return odotaPaluuarvoTaiEpaonnistuTimeouttiin(prosessiId, DEFAULT_POLL_TIMEOUT_MS);
    }
    private Prosessi odotaPaluuarvoTaiEpaonnistuTimeouttiin(final ProsessiId prosessiId, long timeout) {
        if(timeout < 0) {
            throw new RuntimeException("Aika loppui yksikkötestin vastausta odotellessa!");
        }
        long t0 = System.currentTimeMillis();
        final Prosessi dokumenttiProsessi = prosessiResource.getWebClient().path(prosessiId.getId())
                .accept(MediaType.APPLICATION_JSON).get(Prosessi.class);
        if (dokumenttiProsessi.dokumenttiId != null) {
            try {
                LOG.error("Polling service!");
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return odotaPaluuarvoTaiEpaonnistuTimeouttiin(prosessiId, timeout - (System.currentTimeMillis() - t0));
        } else {
            return dokumenttiProsessi;
        }
    }
    private WebClient erillishaunWebClient() {
        return erillishakuResource.getWebClient()
                .query("hakutyyppi", KORKEAKOULU.toString())
                .query("hakuOid", hakuOid)
                .query("hakikohdeOid", hakukohdeOid)
                .query("tarjoajaOid", tarjoajaOid)
                .query("valintatapajonoOid", valintatapajonoOid)
                .query("valintatapajononNimi", "varsinainen jono")
                .accept(MediaType.APPLICATION_JSON);
    }
}
