package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.hakemusOsallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintakoe;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.testapp.MockResourcesApp;
import fi.vm.sade.valinta.kooste.util.DokumenttiProsessiPoller;
import fi.vm.sade.valinta.kooste.util.ExcelImportUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.OsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Jussi Jartamo
 */
public class ValintalaskentaTulosExcelTest {
  final String root =
      "http://localhost:" + MockResourcesApp.port + "/valintalaskentakoostepalvelu/resources";

  final HttpResourceBuilder.WebClientExposingHttpResource valintakoekutsutResource =
      new HttpResourceBuilder(getClass().getName())
          .address(root + "/valintalaskentaexcel/valintakoekutsut/aktivoi")
          .buildExposingWebClientDangerously();
  final String HAKU1 = "HAKU1";
  final String HAKUKOHDE1 = "HAKUKOHDE1";
  final String VALINTAKOENIMI1 = "VALINTAKOENIMI1";
  final String VALINTAKOENIMI2 = "VALINTAKOENIMI2";
  final String HAKEMUS1 = "HAKEMUS1";
  final String HAKEMUS2 = "HAKEMUS2";
  final String HAKEMUS3 = "HAKEMUS3";
  final String HAKEMUS4 = "HAKEMUS4";
  final String HAKEMUS5 = "HAKEMUS5";
  final String TUNNISTE1 = "TUNNISTE1";
  final String SELVITETTY_TUNNISTE1 = "SELVITETTY_TUNNISTE1";
  final String TUNNISTE2 = "TUNNISTE2";
  final String SELVITETTY_TUNNISTE2 = "SELVITETTY_TUNNISTE2";
  final Haku haku =
      new Haku(HAKU1, new HashMap<>(), new HashSet<>(), null, null, null, null, null, null);

  @BeforeEach
  public void startServer() {
    MockResourcesApp.start();
  }

  @Disabled("This can be used for testing creation of production jsons")
  @Test
  public void testaaExcelinLuontiJsonLahteesta() throws Throwable {
    String listFull = IOUtils.toString(new FileInputStream("listfull.json"));
    String osallistumiset = IOUtils.toString(new FileInputStream("osallistumiset.json"));
    String valintakoe = IOUtils.toString(new FileInputStream("valintakoe.json"));
    List<Hakemus> hakemuses =
        DateDeserializer.gsonBuilder()
            .create()
            .fromJson(listFull, new TypeToken<List<Hakemus>>() {}.getType());
    List<HakemusWrapper> hakemusWrappers =
        hakemuses.stream().map(HakuappHakemusWrapper::new).collect(Collectors.toList());

    List<HakemusOsallistuminenDTO> osallistuminenDTOs =
        valintakoekutsutResource
            .gson()
            .fromJson(osallistumiset, new TypeToken<List<HakemusOsallistuminenDTO>>() {}.getType());

    List<ValintakoeDTO> valintakoeDTOs =
        valintakoekutsutResource
            .gson()
            .fromJson(valintakoe, new TypeToken<List<ValintakoeDTO>>() {}.getType());
    Mocks.reset();
    try {
      List<ValintakoeOsallistuminenDTO> osallistumistiedot = Collections.emptyList();
      Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString()))
          .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
      MockValintalaskentaValintakoeAsyncResource.setHakemusOsallistuminenResult(osallistuminenDTOs);
      MockValintaperusteetAsyncResource.setValintakokeetResult(valintakoeDTOs);
      MockApplicationAsyncResource.setResult(hakemusWrappers);
      MockApplicationAsyncResource.setResultByOid(hakemusWrappers);
      MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

      DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot();
      lisatiedot.setValintakoeTunnisteet(
          Arrays.asList(
              "7c0c20aa-c9a1-53eb-5e46-ca689b3625c0", "d579283e-ab61-e140-306c-7582a666fd85"));
      Response r =
          valintakoekutsutResource
              .getWebClient()
              .query("hakuOid", "1.2.246.562.29.95390561488")
              .query("hakukohdeOid", "1.2.246.562.20.40041089257")
              .post(Entity.entity(lisatiedot, "application/json"));
      assertEquals(200, r.getStatus());

      JSONObject dokumenttiJSON = new JSONObject(r.readEntity(String.class));
      String storedDocumentId =
          DokumenttiProsessiPoller.odotaProsessiaPalautaDokumenttiId(
              root, new ProsessiId(dokumenttiJSON.get("id").toString()));
      final InputStream excelData = MockDokumenttiAsyncResource.getStoredDocument(storedDocumentId);

      IOUtils.copy(excelData, new FileOutputStream("e.xls"));
    } finally {
      Mocks.reset();
      MockApplicationAsyncResource.clear();
    }
  }

  @Test
  public void testaaExcelinLuontiKaikkiOsallistuu() throws Throwable {
    Mocks.reset();
    try {
      List<ValintakoeOsallistuminenDTO> osallistumistiedot = Collections.emptyList();
      Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString()))
          .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
      MockTarjontaAsyncService.setMockHaku(haku);
      MockValintalaskentaValintakoeAsyncResource.setHakemusOsallistuminenResult(
          Arrays.asList(
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS1)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS2)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS3)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1, OsallistuminenDTO.EI_OSALLISTU)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS4)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1, OsallistuminenDTO.EI_VAADITA)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS5)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1, OsallistuminenDTO.VIRHE)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS1)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE2)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS2)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE2)
                  .build()));
      MockValintaperusteetAsyncResource.setValintakokeetResult(
          Arrays.asList(
              valintakoe()
                  .setTunniste(TUNNISTE1)
                  .setNimi(VALINTAKOENIMI1)
                  .setSelvitettyTunniste(SELVITETTY_TUNNISTE1)
                  .build(),
              valintakoe()
                  .setTunniste(TUNNISTE2)
                  .setNimi(VALINTAKOENIMI2)
                  .setKaikkiKutsutaan()
                  .setSelvitettyTunniste(SELVITETTY_TUNNISTE2)
                  .build()));

      MockApplicationAsyncResource.setResultByOid(
          Arrays.asList(hakemus().setOid(HAKEMUS1).build(), hakemus().setOid(HAKEMUS2).build()));
      MockApplicationAsyncResource.setResult(
          Arrays.asList(
              hakemus().setOid(HAKEMUS2).build(),
              hakemus().setOid(HAKEMUS3).build(),
              hakemus().setOid(HAKEMUS4).build(),
              hakemus().setOid(HAKEMUS5).build()));
      MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

      DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot();
      lisatiedot.setValintakoeTunnisteet(Arrays.asList(SELVITETTY_TUNNISTE1, SELVITETTY_TUNNISTE2));
      Response r =
          valintakoekutsutResource
              .getWebClient()
              .query("hakuOid", HAKU1)
              .query("hakukohdeOid", HAKUKOHDE1)
              .post(Entity.entity(lisatiedot, "application/json"));
      assertEquals(200, r.getStatus());

      JSONObject dokumenttiJSON = new JSONObject(r.readEntity(String.class));
      String storedDocumentId =
          DokumenttiProsessiPoller.odotaProsessiaPalautaDokumenttiId(
              root, new ProsessiId(dokumenttiJSON.get("id").toString()));
      final InputStream excelData = MockDokumenttiAsyncResource.getStoredDocument(storedDocumentId);

      assertTrue(excelData != null);
      Collection<Rivi> rivit = ExcelImportUtil.importExcel(excelData);

      assertTrue(
          rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS1.equals(r0.toTeksti().getTeksti()))));
      assertTrue(
          rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS2.equals(r0.toTeksti().getTeksti()))));
      // Ei osallistujat ei tule mukaan
      assertTrue(
          rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS3.equals(r0.toTeksti().getTeksti()))));
      assertTrue(
          rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS4.equals(r0.toTeksti().getTeksti()))));
      assertTrue(
          rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS5.equals(r0.toTeksti().getTeksti()))));

    } finally {
      Mocks.reset();
      MockApplicationAsyncResource.clear();
    }
  }

  @Test
  public void testaaExcelinLuontiHakukohteenUlkopuolisillaHakijoilla() throws Throwable {
    Mocks.reset();
    try {
      List<ValintakoeOsallistuminenDTO> osallistumistiedot =
          Arrays.asList(
              osallistuminen()
                  .setHakemusOid(HAKEMUS1)
                  .hakutoive()
                  .valinnanvaihe()
                  .valintakoe()
                  .setTunniste(SELVITETTY_TUNNISTE1)
                  .setOsallistuu()
                  .build()
                  .build()
                  .build()
                  .build(),
              osallistuminen()
                  .setHakemusOid(HAKEMUS2)
                  .hakutoive()
                  .valinnanvaihe()
                  .valintakoe()
                  .setTunniste(SELVITETTY_TUNNISTE1)
                  .setOsallistuu()
                  .build()
                  .build()
                  .build()
                  .build());
      Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString()))
          .thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
      MockValintalaskentaValintakoeAsyncResource.setHakemusOsallistuminenResult(
          Arrays.asList(
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS1)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS2)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS3)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1, OsallistuminenDTO.EI_OSALLISTU)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS4)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1, OsallistuminenDTO.EI_VAADITA)
                  .build(),
              hakemusOsallistuminen()
                  .setHakemusOid(HAKEMUS5)
                  .setHakutoive(HAKUKOHDE1)
                  .addOsallistuminen(SELVITETTY_TUNNISTE1, OsallistuminenDTO.VIRHE)
                  .build()));
      MockValintaperusteetAsyncResource.setValintakokeetResult(
          Arrays.asList(
              valintakoe()
                  .setTunniste(TUNNISTE1)
                  .setNimi(VALINTAKOENIMI1)
                  .setSelvitettyTunniste(SELVITETTY_TUNNISTE1)
                  .build(),
              valintakoe()
                  .setTunniste(TUNNISTE2)
                  .setNimi(VALINTAKOENIMI2)
                  .setSelvitettyTunniste(SELVITETTY_TUNNISTE2)
                  .build()));

      MockApplicationAsyncResource.setResultByOid(
          Arrays.asList(
              hakemus().setOid(HAKEMUS1).build(),
              hakemus().setOid(HAKEMUS2).build(),
              hakemus().setOid(HAKEMUS3).build(),
              hakemus().setOid(HAKEMUS4).build(),
              hakemus().setOid(HAKEMUS5).build()));

      MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

      DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot();
      lisatiedot.setValintakoeTunnisteet(Arrays.asList(SELVITETTY_TUNNISTE1));
      Response r =
          valintakoekutsutResource
              .getWebClient()
              .query("hakuOid", HAKU1)
              .query("hakukohdeOid", HAKUKOHDE1)
              .post(Entity.entity(lisatiedot, "application/json"));
      assertEquals(200, r.getStatus());

      JSONObject dokumenttiJSON = new JSONObject(r.readEntity(String.class));
      String storedDocumentId =
          DokumenttiProsessiPoller.odotaProsessiaPalautaDokumenttiId(
              root, new ProsessiId(dokumenttiJSON.get("id").toString()));
      final InputStream excelData = MockDokumenttiAsyncResource.getStoredDocument(storedDocumentId);
      assertTrue(excelData != null);
      Collection<Rivi> rivit = ExcelImportUtil.importExcel(excelData);
      assertTrue(
          rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS1.equals(r0.toTeksti().getTeksti()))));
      assertTrue(
          rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS2.equals(r0.toTeksti().getTeksti()))));
      // Ei osallistujat ei tule mukaan
      assertTrue(
          !rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS3.equals(r0.toTeksti().getTeksti()))));
      assertTrue(
          !rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS4.equals(r0.toTeksti().getTeksti()))));
      assertTrue(
          !rivit.stream()
              .anyMatch(
                  rivi ->
                      rivi.getSolut().stream()
                          .anyMatch(r0 -> HAKEMUS5.equals(r0.toTeksti().getTeksti()))));

    } finally {
      Mocks.reset();
    }
  }
}
