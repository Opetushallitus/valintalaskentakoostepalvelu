package fi.vm.sade.valinta.kooste.erillishaku.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import fi.vm.sade.authentication.model.HenkiloTyyppi;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
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
import fi.vm.sade.valinta.kooste.util.DokumenttiProsessiPoller;
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
import java.util.List;

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

    private void testVientiExcelTiedostoon(Hakutyyppi hakutyyppi) throws IOException {
        MockApplicationAsyncResource.setResult(createVientiHakemus(hakutyyppi));
        final String url = root + "/erillishaku/vienti";
        final ProsessiId prosessiId = createClient(url)
                .query("hakutyyppi", hakutyyppi)
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
        verifyCreatedExcelDocument(hakutyyppi, storedDocument);
    }

    @Test
    public void vientiExcelTiedostoonKK() throws IOException {
        testVientiExcelTiedostoon(Hakutyyppi.KORKEAKOULU);
    }

    @Test
    public void vientiExcelTiedostoonToinenAste() throws IOException {
        testVientiExcelTiedostoon(Hakutyyppi.TOISEN_ASTEEN_OPPILAITOS);
    }

    private List<Hakemus> createVientiHakemus(Hakutyyppi hakutyyppi) {
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
        if(hakutyyppi == Hakutyyppi.KORKEAKOULU) {
            answers.getKoulutustausta().put(HakemusWrapper.TOISEN_ASTEEN_SUORITUS, "true");
            answers.getKoulutustausta().put(HakemusWrapper.TOISEN_ASTEEN_SUORITUSMAA, "FIN");
        }
        hakemus.setAnswers(answers);
        return Collections.singletonList(hakemus);
    }

    @Test
    public void tuontiExcelTiedostostaKK() {
        final String url = root + "/erillishaku/tuonti";

        final ProsessiId prosessiId = createClient(url)
            .query("hakutyyppi", "KORKEAKOULU")
            .query("hakuOid", hakuOid)
            .query("hakukohdeOid", hakukohdeOid)
            .query("tarjoajaOid", tarjoajaOid)
            .query("valintatapajonoOid", valintatapajonoOid)
            .query("valintatapajononNimi", "varsinainen jono")
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.entity(ExcelTestData.kkHakuToisenAsteenValintatuloksella(), MediaType.APPLICATION_OCTET_STREAM), ProsessiId.class);

        odotaProsessiaPalautaDokumenttiId(prosessiId);
    }

    @Test
    public void tuontiExcelTiedostostaToinenAste() {
        final String url = root + "/erillishaku/tuonti";

        final ProsessiId prosessiId = createClient(url)
                .query("hakutyyppi", "TOISEN_ASTEEN_OPPILAITOS")
                .query("hakuOid", hakuOid)
                .query("hakukohdeOid", hakukohdeOid)
                .query("tarjoajaOid", tarjoajaOid)
                .query("valintatapajonoOid", valintatapajonoOid)
                .query("valintatapajononNimi", "varsinainen jono")
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(ExcelTestData.toisenAsteenErillisHaku(), MediaType.APPLICATION_OCTET_STREAM), ProsessiId.class);

        odotaProsessiaPalautaDokumenttiId(prosessiId);
    }

    private void verifyCreatedExcelDocument(Hakutyyppi hakutyyppi, final InputStream storedDocument) throws IOException {
        final ImportedErillisHakuExcel tulos = new ImportedErillisHakuExcel(hakutyyppi, storedDocument);
        assertEquals(1, tulos.rivit.size());
        final HenkiloCreateDTO expectedHenkilo = new HenkiloCreateDTO(
                "SV",
                "MIES",
                MockData.etunimi,
                MockData.sukunimi,
                MockData.hetu,
                ErillishakuRivi.SYNTYMAAIKAFORMAT.parseDateTime("1.1.1901").toDate(),
                henkiloOid,
                HenkiloTyyppi.OPPIJA,
                "SV",
                null);
        assertEquals(expectedHenkilo, tulos.rivit.get(0).toHenkiloCreateDTO(null));
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
                hakutyyppi == Hakutyyppi.KORKEAKOULU ? Boolean.TRUE : null,
                hakutyyppi == Hakutyyppi.KORKEAKOULU ? "FIN" : "",
                "TARKISTAMATTA");
        assertEquals(expectedRivi.toString(), tulos.rivit.get(0).toString());
    }

    private String odotaProsessiaPalautaDokumenttiId(final ProsessiId prosessiId) {
        Prosessi valmisProsessi = DokumenttiProsessiPoller.pollDokumenttiProsessi(root, prosessiId, prosessi -> {
            if (prosessi.poikkeuksia()) {
                throw new RuntimeException(prosessi.poikkeukset.toString());
            }
            return prosessi.valmis();
        });
        return valmisProsessi.dokumenttiId;
    }

    private WebClient createClient(String url) {
        return new HttpResource(url).getWebClient();
    }

}
