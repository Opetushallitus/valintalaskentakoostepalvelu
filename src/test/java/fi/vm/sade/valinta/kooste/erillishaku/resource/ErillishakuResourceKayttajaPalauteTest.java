package fi.vm.sade.valinta.kooste.erillishaku.resource;

import static fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi.KORKEAKOULU;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HAKEMUSPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HENKILOPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_TYHJA_DATAJOUKKO;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_VIALLINEN_DATAJOUKKO;
import static fi.vm.sade.valinta.kooste.erillishaku.util.ErillishakuRiviTestUtil.laillinenRivi;
import static fi.vm.sade.valinta.kooste.erillishaku.util.ErillishakuRiviTestUtil.viallinenJsonRiviPuuttuvillaTunnisteilla;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import com.google.gson.GsonBuilder;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuJson;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockHenkiloAsyncResource;
import fi.vm.sade.valinta.kooste.util.DokumenttiProsessiPoller;
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
import java.util.Collection;

/**
 * @author Jussi Jartamo
 */
public class ErillishakuResourceKayttajaPalauteTest {

    final static Logger LOG = LoggerFactory.getLogger(ErillishakuResourceKayttajaPalauteTest.class);
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    String hakuOid = "1.2.246.562.5.2013080813081926341928";
    String hakukohdeOid = "1.2.246.562.5.72607738902";
    String tarjoajaOid = "1.2.246.562.10.591352080610";
    String valintatapajonoOid = "14090336922663576781797489829886";
    final HttpResource jsonResource = new HttpResourceBuilder().address(root + "/erillishaku/tuonti/ui").build();
    final HttpResource excelResource = new HttpResourceBuilder().address(root + "/erillishaku/tuonti").build();
    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
        MockApplicationAsyncResource.serviceIsAvailable.set(true);
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
                        "Rivi 2: Henkilötunnus, syntymäaika + sukupuoli ja henkilö-oid olivat tyhjiä (vähintään yksi tunniste on syötettävä). Äidinkieli on pakollinen tieto, kun henkilötunnus ja henkilö OID puuttuvat. : Etunimi, Sukunimi, , HYVAKSYTTY, Ei, ***HENKILOTUNNUS***, , NAINEN, , EI_ILMOITTAUTUNUT, false, null, VASTAANOTTANUT_SITOVASTI, null, true, null, null, null, null, null, null, null, null, , null",
                        ErillishakuResource.RIVIN_TUNNISTE_KAYTTOLIITTYMAAN
                )
        )))));
    }

    @Test
    public void tuontiVirheitaPuuttuviaPakollisiaKKExcelTietoja() {
        final ProsessiId prosessiId =
                excelClient()
                        .post(Entity.entity(ExcelTestData.kkHakuPuuttuviaPakollisiaTietoja(), MediaType.APPLICATION_OCTET_STREAM), ProsessiId.class);

        Collection<Poikkeus> poikkeukset = odotaVirhettaTaiEpaonnistuTimeouttiin(prosessiId).poikkeukset;
        assertThat(poikkeukset.size(), equalTo(1));
        assertThat(poikkeukset.iterator().next().getTunnisteet().size(), equalTo(2));
    }


    @Test
    public void tuontiVirheHakemuspalveluKutsussaPalaute() {
        try {
            MockApplicationAsyncResource.serviceIsAvailable.set(false);
            final ProsessiId prosessiId = excelClient()
                    .post(Entity.entity(ExcelTestData.kkHakuToisenAsteenValintatuloksella(), MediaType.APPLICATION_OCTET_STREAM), ProsessiId.class);
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
        return DokumenttiProsessiPoller.pollDokumenttiProsessi(root, prosessiId, prosessiResponse -> !prosessiResponse.poikkeukset.isEmpty());
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
                .accept(MediaType.APPLICATION_JSON);
    }
}
