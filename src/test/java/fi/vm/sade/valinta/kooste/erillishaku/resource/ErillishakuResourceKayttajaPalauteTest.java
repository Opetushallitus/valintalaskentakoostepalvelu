package fi.vm.sade.valinta.kooste.erillishaku.resource;

import com.google.gson.GsonBuilder;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import static fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi.*;
import static org.hamcrest.CoreMatchers.*;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuJson;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.*;

import static fi.vm.sade.valinta.kooste.erillishaku.util.ErillishakuRiviTestUtil.*;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockHenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import static java.util.Arrays.*;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.*;
import static org.junit.Assert.*;

/**
 * @author Jussi Jartamo
 */
public class ErillishakuResourceKayttajaPalauteTest {

    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final static Logger LOG = LoggerFactory.getLogger(ErillishakuResourceKayttajaPalauteTest.class);
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    String hakuOid = "1.2.246.562.5.2013080813081926341928";
    String hakukohdeOid = "1.2.246.562.5.72607738902";
    String tarjoajaOid = "1.2.246.562.10.591352080610";
    String valintatapajonoOid = "14090336922663576781797489829886";
    final HttpResource jsonResource = new HttpResource(root + "/erillishaku/tuonti/json");
    final HttpResource excelResource = new HttpResource(root + "/erillishaku/tuonti");
    final HttpResource prosessiResource = new HttpResource(root + "/dokumenttiprosessi/");

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void tuontiVirheTyhjaTietoJoukkoPalaute() throws IOException {
        final ProsessiId prosessiId =
                jsonClient()
                        .post(Entity.json(new ErillishakuJson(emptyList()))
                                //
                                , ProsessiId.class);

        assertTrue(odotaVirhettaTaiEpaonnistuTimeouttiin(prosessiId)
                // Odotetaan tyhjää datajoukko palautetta!
                .poikkeukset.contains(Poikkeus.koostepalvelupoikkeus(POIKKEUS_TYHJA_DATAJOUKKO)));
    }
    @Test
    public void tuontiVirheitaTuoduissaTiedoissaPalaute() {
        LOG.error("\n{}", new GsonBuilder().create().toJson(viallinenJsonRiviPuuttuvillaTunnisteilla()));
        final ProsessiId prosessiId =
                jsonClient()
                        .post(Entity.json(viallinenJsonRiviPuuttuvillaTunnisteilla()
                        )
                                //
                                , ProsessiId.class);

        assertThat(odotaVirhettaTaiEpaonnistuTimeouttiin(prosessiId)
                // Odotetaan tyhjää datajoukko palautetta!
                .poikkeukset, equalTo(asList(Poikkeus.koostepalvelupoikkeus(POIKKEUS_VIALLINEN_DATAJOUKKO, asList(
                new Tunniste(
                        "Rivi 2: Henkilötunnus, syntymäaika + sukupuoli ja henkilö-oid oli tyhjiä. Vähintään yksi tunniste on syötettävä. Etunimi, Sukunimi, , HYVAKSYTTY, Ei, ***HENKILOTUNNUS***, , NAINEN, , EI_ILMOITTAUTUNUT, false, null, VASTAANOTTANUT_SITOVASTI, true, null, null, null, null, null, null, null, null, null",
                        ErillishakuResource.RIVIN_TUNNISTE_KAYTTOLIITTYMAAN
                )
        )))));
    }

    @Test
    public void tuontiVirheHakemuspalveluKutsussaPalaute() {
        try {
            MockApplicationAsyncResource.serviceIsAvailable.set(false);
            final ProsessiId prosessiId =
                    jsonClient()
                            .post(Entity.json(new ErillishakuJson(asList(laillinenRivi())))
                                    //
                                    , ProsessiId.class);

            assertThat(odotaVirhettaTaiEpaonnistuTimeouttiin(prosessiId)
                    // Odotetaan hakemuspalvelun epäonnistumisesta johtuvaa palautetta!
                    .poikkeukset, equalTo(asList(Poikkeus.hakemuspalvelupoikkeus(POIKKEUS_HAKEMUSPALVELUN_VIRHE))));
        } finally {
            MockApplicationAsyncResource.serviceIsAvailable.set(true);
        }
    }
    @Test
    public void tuontiVirheHenkilopalveluKutsussaPalaute() {
        try {
            MockHenkiloAsyncResource.serviceIsAvailable.set(false);
            final ProsessiId prosessiId =
                    jsonClient()
                            .post(Entity.json(new ErillishakuJson(asList(laillinenRivi())))
                                    //
                                    , ProsessiId.class);

            assertThat(odotaVirhettaTaiEpaonnistuTimeouttiin(prosessiId)
                    // Odotetaan hakemuspalvelun epäonnistumisesta johtuvaa palautetta!
                    .poikkeukset, equalTo(asList(Poikkeus.henkilopalvelupoikkeus(POIKKEUS_HENKILOPALVELUN_VIRHE))));
        } finally {
            MockHenkiloAsyncResource.serviceIsAvailable.set(true);
        }
    }
    private Prosessi odotaVirhettaTaiEpaonnistuTimeouttiin(final ProsessiId prosessiId) {
        return odotaPaluuarvoTaiEpaonnistuTimeouttiin(prosessiId, DEFAULT_POLL_TIMEOUT_MS);
    }
    private Prosessi odotaPaluuarvoTaiEpaonnistuTimeouttiin(final ProsessiId prosessiId, long timeout) {
        if(timeout < 0) {
            throw new RuntimeException("Aika loppui yksikkötestin vastausta odotellessa!");
        }
        long t0 = System.currentTimeMillis();
        final Prosessi dokumenttiProsessi = prosessiResource.getWebClient().path(prosessiId.getId())
                .accept(MediaType.APPLICATION_JSON).get(Prosessi.class);
        if (dokumenttiProsessi.poikkeukset.isEmpty()) {
            try {
                Thread.sleep(0L);
            } catch (InterruptedException e) {
                //ignore
            }
            return odotaPaluuarvoTaiEpaonnistuTimeouttiin(prosessiId, timeout - (System.currentTimeMillis() - t0));
        } else {
            return dokumenttiProsessi;
        }
    }
    private WebClient jsonClient() {
        return jsonResource.getWebClient()
                .query("hakutyyppi", KORKEAKOULU.toString())
                .query("hakuOid", hakuOid)
                .query("hakikohdeOid", hakukohdeOid)
                .query("tarjoajaOid", tarjoajaOid)
                .query("valintatapajonoOid", valintatapajonoOid)
                .query("valintatapajononNimi", "varsinainen jono")
                .accept(MediaType.APPLICATION_JSON);
    }
    private WebClient excelClient() {
        return excelResource.getWebClient()
                .query("hakutyyppi", KORKEAKOULU.toString())
                .query("hakuOid", hakuOid)
                .query("hakikohdeOid", hakukohdeOid)
                .query("tarjoajaOid", tarjoajaOid)
                .query("valintatapajonoOid", valintatapajonoOid)
                .query("valintatapajononNimi", "varsinainen jono")
                .accept(MediaType.APPLICATION_OCTET_STREAM);
    }
}
