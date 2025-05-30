package fi.vm.sade.valinta.kooste.erillishaku.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRiviBuilder;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ExcelTestData;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Maksuvelvollisuus;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Sukupuoli;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ImportedErillisHakuExcel;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Maksuntila;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.testapp.MockResourcesApp;
import fi.vm.sade.valinta.kooste.util.DokumenttiProsessiPoller;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ErillishakuResourceTest {
  private String hakuOid = "1.2.246.562.5.2013080813081926341928";
  private String hakukohdeOid = "1.2.246.562.5.72607738902";
  private String tarjoajaOid = "1.2.246.562.10.591352080610";
  private String valintatapajonoOid = "14090336922663576781797489829886";
  private String henkiloOid = "hakija1";
  private final String root =
      "http://localhost:" + MockResourcesApp.port + "/valintalaskentakoostepalvelu/resources";
  final MockKoodistoCachedAsyncResource mockKoodistoCachedAsyncResource =
      new MockKoodistoCachedAsyncResource(mock(KoodistoAsyncResource.class));

  @BeforeEach
  public void startServer() {
    MockResourcesApp.start();
  }

  private void testVientiExcelTiedostoon(Hakutyyppi hakutyyppi) {
    MockApplicationAsyncResource.setResult(createVientiHakemus(hakutyyppi));

    final String url = root + "/erillishaku/vienti";
    final ProsessiId prosessiId =
        createClient(url)
            .query("hakutyyppi", hakutyyppi)
            .query("hakuOid", hakuOid)
            .query("hakukohdeOid", hakukohdeOid)
            .query("tarjoajaOid", tarjoajaOid)
            .query("valintatapajonoOid", valintatapajonoOid)
            .query("valintatapajononNimi", "varsinainen jono")
            .type(MediaType.APPLICATION_JSON_TYPE)
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(
                Entity.entity(Collections.emptyList(), MediaType.APPLICATION_JSON),
                ProsessiId.class);

    String documentId =
        DokumenttiProsessiPoller.odotaProsessiaPalautaDokumenttiId(root, prosessiId);
    final InputStream storedDocument = MockDokumenttiAsyncResource.getStoredDocument(documentId);
    assertNotNull(storedDocument);
    verifyCreatedExcelDocument(hakutyyppi, storedDocument);
  }

  @Test
  public void vientiExcelTiedostoonKK() {
    testVientiExcelTiedostoon(Hakutyyppi.KORKEAKOULU);
  }

  @Test
  public void vientiExcelTiedostoonToinenAste() {
    testVientiExcelTiedostoon(Hakutyyppi.TOISEN_ASTEEN_OPPILAITOS);
  }

  private List<HakemusWrapper> createVientiHakemus(Hakutyyppi hakutyyppi) {
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
    if (hakutyyppi == Hakutyyppi.KORKEAKOULU) {
      answers.getKoulutustausta().put(HakuappHakemusWrapper.TOISEN_ASTEEN_SUORITUS, "true");
      answers.getKoulutustausta().put(HakuappHakemusWrapper.TOISEN_ASTEEN_SUORITUSMAA, "FIN");
    }
    hakemus.setAnswers(answers);
    return Collections.singletonList(new HakuappHakemusWrapper(hakemus));
  }

  @Test
  public void tuontiExcelTiedostostaKK() {
    final String url = root + "/erillishaku/tuonti";

    final ProsessiId prosessiId =
        createClient(url)
            .query("hakutyyppi", "KORKEAKOULU")
            .query("hakuOid", hakuOid)
            .query("hakukohdeOid", hakukohdeOid)
            .query("tarjoajaOid", tarjoajaOid)
            .query("valintatapajonoOid", valintatapajonoOid)
            .query("valintatapajononNimi", "varsinainen jono")
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(
                Entity.entity(
                    ExcelTestData.kkHakuToisenAsteenValintatuloksella(),
                    MediaType.APPLICATION_OCTET_STREAM),
                ProsessiId.class);

    DokumenttiProsessiPoller.odotaProsessiaPalautaDokumenttiId(root, prosessiId);
  }

  @Test
  public void tuontiExcelTiedostostaToinenAste() {
    final String url = root + "/erillishaku/tuonti";

    final ProsessiId prosessiId =
        createClient(url)
            .query("hakutyyppi", "TOISEN_ASTEEN_OPPILAITOS")
            .query("hakuOid", hakuOid)
            .query("hakukohdeOid", hakukohdeOid)
            .query("tarjoajaOid", tarjoajaOid)
            .query("valintatapajonoOid", valintatapajonoOid)
            .query("valintatapajononNimi", "varsinainen jono")
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(
                Entity.entity(
                    ExcelTestData.toisenAsteenErillisHaku(), MediaType.APPLICATION_OCTET_STREAM),
                ProsessiId.class);

    DokumenttiProsessiPoller.odotaProsessiaPalautaDokumenttiId(root, prosessiId);
  }

  private void verifyCreatedExcelDocument(Hakutyyppi hakutyyppi, final InputStream storedDocument) {
    final ImportedErillisHakuExcel tulos =
        new ImportedErillisHakuExcel(hakutyyppi, storedDocument, mockKoodistoCachedAsyncResource);
    assertEquals(1, tulos.rivit.size());
    final HenkiloCreateDTO expectedHenkilo =
        new HenkiloCreateDTO(
            "SV",
            "1",
            MockData.etunimi,
            MockData.sukunimi,
            MockData.hetu,
            "1901-01-01",
            henkiloOid,
            HenkiloTyyppi.OPPIJA,
            "SV",
            null);
    assertEquals(expectedHenkilo, tulos.rivit.get(0).toHenkiloCreateDTO(null));

    final ErillishakuRivi expectedRivi =
        new ErillishakuRiviBuilder()
            .hakemusOid(MockData.hakemusOid)
            .sukunimi(MockData.sukunimi)
            .etunimi(MockData.etunimi)
            .kutsumanimi(MockData.etunimi)
            .henkilotunnus(MockData.hetu)
            .sahkoposti("testi@testi.fi")
            .syntymaAika(MockData.syntymaAika)
            .sukupuoli(Sukupuoli.MIES)
            .personOid("")
            .aidinkieli("SV")
            .hakemuksenTila("KESKEN")
            .ehdollisestiHyvaksyttavissa(false)
            .hyvaksymiskirjeLahetetty(null)
            .vastaanottoTila("")
            .ilmoittautumisTila("")
            .julkaistaankoTiedot(false)
            .poistetaankoRivi(false)
            .asiointikieli("SV")
            .puhelinnumero("040123")
            .osoite("Testitie 2")
            .postinumero("00100")
            .postitoimipaikka("HELSINKI")
            .asuinmaa("FIN")
            .kansalaisuus("FIN")
            .kotikunta("Helsinki")
            .toisenAsteenSuoritus(hakutyyppi == Hakutyyppi.KORKEAKOULU ? Boolean.TRUE : null)
            .toisenAsteenSuoritusmaa(hakutyyppi == Hakutyyppi.KORKEAKOULU ? "FIN" : "")
            .maksuvelvollisuus(Maksuvelvollisuus.NOT_CHECKED)
            .maksuntila(hakutyyppi == Hakutyyppi.KORKEAKOULU ? Maksuntila.MAKSAMATTA : null)
            .syntymapaikka("")
            .passinNumero("")
            .idTunnus("")
            .kaupunkiJaMaa("")
            .build();
    assertEquals(expectedRivi.toString(), tulos.rivit.get(0).toString());
  }

  private WebClient createClient(String url) {
    return new HttpResourceBuilder(getClass().getName())
        .address(url)
        .buildExposingWebClientDangerously()
        .getWebClient();
  }
}
