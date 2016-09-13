package fi.vm.sade.valinta.kooste.erillishaku.resource;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuDataRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ImportedErillisHakuExcel;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockData;
import fi.vm.sade.valinta.kooste.mocks.MockDokumenttiResource;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ErillishakuResourceTest {
    private String hakuOid = "1.2.246.562.5.2013080813081926341928";
    private String hakukohdeOid = "1.2.246.562.5.72607738902";
    private String tarjoajaOid = "1.2.246.562.10.591352080610";
    private String valintatapajonoOid = "14090336922663576781797489829886";
    private String henkiloOid = "hakija1";
    private final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void vientiExcelTiedostoon() throws IOException {
        MockApplicationAsyncResource.setResult(null);
        final String url = root + "/erillishaku/vienti";
        final ProsessiId prosessiId = createClient(url)
            .query("hakutyyppi", "KORKEAKOULU")
            .query("hakuOid", hakuOid)
            .query("hakukohdeOid", hakukohdeOid)
            .query("tarjoajaOid", tarjoajaOid)
            .query("valintatapajonoOid", valintatapajonoOid)
            .query("valintatapajononNimi", "varsinainen jono")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(Arrays.asList(), MediaType.APPLICATION_JSON), ProsessiId.class);

        String documentId = odotaProsessiaPalautaDokumenttiId(prosessiId);
        final InputStream storedDocument = MockDokumenttiResource.getStoredDocument(documentId);
        assertNotNull(storedDocument);
        verifyCreatedExcelDocument(storedDocument);
    }

    @Test
    public void vientiExcelTiedostoon2() throws IOException {
        MockApplicationAsyncResource.setResult(createVientiHakemus());
        final String url = root + "/erillishaku/vienti";
        final ProsessiId prosessiId = createClient(url)
                .query("hakutyyppi", "KORKEAKOULU")
                .query("hakuOid", hakuOid)
                .query("hakukohdeOid", hakukohdeOid)
                .query("tarjoajaOid", tarjoajaOid)
                .query("valintatapajonoOid", valintatapajonoOid)
                .query("valintatapajononNimi", "varsinainen jono")
                .type(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(Collections.emptyList(), MediaType.APPLICATION_JSON), ProsessiId.class);

        String documentId = odotaProsessiaPalautaDokumenttiId(prosessiId);
        final InputStream storedDocument = MockDokumenttiResource.getStoredDocument(documentId);
        assertNotNull(storedDocument);
        verifyCreatedExcelDocument2(storedDocument);
    }

    private List<Hakemus> createVientiHakemus() {
        Hakemus hakemus = new Hakemus();
        hakemus.setOid(MockData.hakemusOid);
        hakemus.setPersonOid(MockData.hakijaOid);
        Answers answers = new Answers();
        answers.getHenkilotiedot().put("Henkilotunnus", MockData.hetu);
        answers.getHenkilotiedot().put("Etunimet", MockData.etunimi);
        answers.getHenkilotiedot().put("Kutsumanimi", MockData.etunimi);
        answers.getHenkilotiedot().put("Sukunimi", MockData.sukunimi);
        answers.getHenkilotiedot().put("syntymaaika", MockData.syntymaAika);
        answers.getHenkilotiedot().put("asuinmaa", "FIN");
        answers.getHenkilotiedot().put("kansalaisuus", "FIN");
        answers.getHenkilotiedot().put("Postinumero", "00100");
        answers.getHenkilotiedot().put("matkapuhelinnumero", "040123");
        answers.getHenkilotiedot().put("lahiosoite", "Testitie 2");
        answers.getHenkilotiedot().put("Sähköposti", "testi@testi.fi");
        answers.getHenkilotiedot().put("aidinkieli", "SV");
        answers.getHenkilotiedot().put("kotikunta", "091");
        answers.getLisatiedot().put("asiointikieli", "ruotsi");
        answers.getKoulutustausta().put(HakemusWrapper.POHJAKOULUTUSMAA_TOINEN_ASTE, "FIN");
        hakemus.setAnswers(answers);
        return Collections.singletonList(hakemus);
    }

    @Test
    public void tuontiExcelTiedostosta() {
        final String url = root + "/erillishaku/tuonti";

        final ProsessiId prosessiId = createClient(url)
            .query("hakutyyppi", "KORKEAKOULU")
            .query("hakuOid", hakuOid)
            .query("hakukohdeOid", hakukohdeOid)
            .query("tarjoajaOid", tarjoajaOid)
            .query("valintatapajonoOid", valintatapajonoOid)
            .query("valintatapajononNimi", "varsinainen jono")
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(ExcelTestData.erillisHakuUusillaKentilla(), MediaType.APPLICATION_OCTET_STREAM), ProsessiId.class);

        odotaProsessiaPalautaDokumenttiId(prosessiId);
    }

    @Test
    public void tuontiExcelTiedostosta2() {
        final String url = root + "/erillishaku/tuonti";

        final ProsessiId prosessiId = createClient(url)
                .query("hakutyyppi", "KORKEAKOULU")
                .query("hakuOid", hakuOid)
                .query("hakukohdeOid", hakukohdeOid)
                .query("tarjoajaOid", tarjoajaOid)
                .query("valintatapajonoOid", valintatapajonoOid)
                .query("valintatapajononNimi", "varsinainen jono")
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(ExcelTestData.erillisHakuUusillaKentilla(), MediaType.APPLICATION_OCTET_STREAM), ProsessiId.class);

        odotaProsessiaPalautaDokumenttiId(prosessiId);
    }

    private void verifyCreatedExcelDocument(final InputStream storedDocument) throws IOException {
        final ImportedErillisHakuExcel tulos = new ImportedErillisHakuExcel(Hakutyyppi.KORKEAKOULU, storedDocument);
        assertEquals(1, tulos.rivit.size());
        final HenkiloCreateDTO expectedHenkilo = new HenkiloCreateDTO(
                "",
                "MIES",
                "Tuomas",
                "Hakkarainen",
                MockData.hetu,
                ErillishakuRivi.SYNTYMAAIKAFORMAT.parseDateTime("1.1.1901").toDate(),
                henkiloOid,
                HenkiloTyyppi.OPPIJA,
                null,
                null);
        assertEquals(expectedHenkilo, tulos.rivit.get(0).toHenkiloCreateDTO(null));
    }

    private void verifyCreatedExcelDocument2(final InputStream storedDocument) throws IOException {
        final ImportedErillisHakuExcel tulos = new ImportedErillisHakuExcel(Hakutyyppi.KORKEAKOULU, storedDocument);
        assertEquals(1, tulos.rivit.size());
        final ErillishakuRivi expectedRivi = new ErillishakuRivi(
                MockData.hakemusOid,
                MockData.sukunimi,
                MockData.etunimi,
                MockData.hetu,
                "testi@testi.fi",
                MockData.syntymaAika,
                "MIES",
                "",
                "SV",
                "KESKEN",
                false,
                null,
                "",
                "",
                false,
                false,
                "SV",
                "040123",
                "Testitie 2",
                "00100",
                "HELSINKI",
                "FIN",
                "FIN",
                "Helsinki",
                "FIN");
        assertEquals(expectedRivi.toString(), tulos.rivit.get(0).toString());
    }

    private String odotaProsessiaPalautaDokumenttiId(final ProsessiId prosessiId) {
        final Prosessi dokumenttiProsessi = createClient(root + "/dokumenttiprosessi/" + prosessiId.getId())
            .accept(MediaType.APPLICATION_JSON).get(Prosessi.class);
        if(dokumenttiProsessi.poikkeuksia()) {
            throw new RuntimeException(dokumenttiProsessi.poikkeukset.toString());
        }
        if (!dokumenttiProsessi.valmis()) {
            try {
                Thread.sleep(100);
                //System.err.println("odotetaan koska saatiin \n"+ new GsonBuilder().setPrettyPrinting().create().toJson(dokumenttiProsessi));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return odotaProsessiaPalautaDokumenttiId(prosessiId);
        }
        return dokumenttiProsessi.dokumenttiId;
    }

    private WebClient createClient(String url) {
        return new HttpResource(url).getWebClient();
    }

}
