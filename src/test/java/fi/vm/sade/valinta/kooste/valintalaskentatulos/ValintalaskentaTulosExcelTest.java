package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.hakemusOsallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintakoe;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl.ApplicationAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTarjontaAsyncService;
import fi.vm.sade.valinta.kooste.mocks.MockValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import fi.vm.sade.valinta.kooste.spec.tarjonta.TarjontaSpec;
import fi.vm.sade.valinta.kooste.util.ExcelImportUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.sharedutils.http.HttpResource;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.OsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import io.reactivex.Observable;
import io.reactivex.internal.operators.observable.ObservableJust;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Jussi Jartamo
 */
public class ValintalaskentaTulosExcelTest {
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource hakemusResource = new ApplicationAsyncResourceImpl(null, null);
    final HttpResourceBuilder.WebClientExposingHttpResource valintakoekutsutResource = new HttpResourceBuilder(getClass().getName())
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
    final HakuV1RDTO haku = new TarjontaSpec.HakuBuilder(HAKU1, null).build();

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Ignore("This can be used for testing creation of production jsons")
    @Test
    public void testaaExcelinLuontiJsonLahteesta() throws Throwable {
        String listFull = IOUtils.toString(new FileInputStream("listfull.json"));
        String osallistumiset = IOUtils.toString(new FileInputStream("osallistumiset.json"));
        String valintakoe = IOUtils.toString(new FileInputStream("valintakoe.json"));
        List<Hakemus> hakemuses = hakemusResource.gson().fromJson(listFull, new TypeToken<List<Hakemus>>() {}.getType());
        List<HakemusWrapper> hakemusWrappers = hakemuses.stream().map(HakuappHakemusWrapper::new).collect(Collectors.toList());

        List<HakemusOsallistuminenDTO> osallistuminenDTOs = valintakoekutsutResource.gson().fromJson(osallistumiset, new TypeToken<List<HakemusOsallistuminenDTO>>() { }.getType());

        List<ValintakoeDTO> valintakoeDTOs = valintakoekutsutResource.gson().fromJson(valintakoe, new TypeToken<List<ValintakoeDTO>>() {}.getType());
        Mocks.reset();
        try {
            List<ValintakoeOsallistuminenDTO> osallistumistiedot = Collections.emptyList();
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
            MockValintalaskentaValintakoeAsyncResource.setHakemusOsallistuminenResult(osallistuminenDTOs);
            MockValintaperusteetAsyncResource.setValintakokeetResult(valintakoeDTOs);
            MockApplicationAsyncResource.setResult(hakemusWrappers);
            MockApplicationAsyncResource.setResultByOid(hakemusWrappers);
            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
            ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.anyLong(),
                    Mockito.anyList(),
                    Mockito.anyString(),
                    inputStreamArgumentCaptor.capture()
            )).thenReturn(new ObservableJust<Response>(null));


            DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot();
            lisatiedot.setValintakoeTunnisteet(Arrays.asList("7c0c20aa-c9a1-53eb-5e46-ca689b3625c0", "d579283e-ab61-e140-306c-7582a666fd85"));
            Response r = valintakoekutsutResource.getWebClient()
                            .query("hakuOid", "1.2.246.562.29.95390561488")
                            .query("hakukohdeOid", "1.2.246.562.20.40041089257")
                            .post(Entity.entity(lisatiedot, "application/json"));
            assertEquals(200, r.getStatus());
            byte[] excelBytes = IOUtils.toByteArray(inputStreamArgumentCaptor.getValue());
            IOUtils.copy(new ByteArrayInputStream(excelBytes), new FileOutputStream("e.xls"));
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
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
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
                                    .build()
                    )
            );
            MockValintaperusteetAsyncResource.setValintakokeetResult(
                    Arrays.asList(valintakoe()
                                    .setTunniste(TUNNISTE1)
                                    .setNimi(VALINTAKOENIMI1)
                                    .setSelvitettyTunniste(SELVITETTY_TUNNISTE1)
                                    .build(),
                            valintakoe()
                                    .setTunniste(TUNNISTE2)
                                    .setNimi(VALINTAKOENIMI2)
                                    .setKaikkiKutsutaan()
                                    .setSelvitettyTunniste(SELVITETTY_TUNNISTE2)
                                    .build())
            );

            MockApplicationAsyncResource.setResultByOid(Arrays.asList(
                    hakemus()
                            .setOid(HAKEMUS1)
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS2)
                            .build()
            ));
            MockApplicationAsyncResource.setResult(Arrays.asList(
                    hakemus()
                            .setOid(HAKEMUS2)
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS3)
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS4)
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS5)
                            .build()
            ));
            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

            ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.anyLong(),
                    Mockito.anyList(),
                    Mockito.anyString(),
                    inputStreamArgumentCaptor.capture()
            )).thenReturn(Observable.just(Response.ok("OK").build()));


            DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot();
            lisatiedot.setValintakoeTunnisteet(Arrays.asList(SELVITETTY_TUNNISTE1, SELVITETTY_TUNNISTE2));
            Response r =
                    valintakoekutsutResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid", HAKUKOHDE1)
                            .post(Entity.entity(lisatiedot, "application/json"));
            assertEquals(200, r.getStatus());
            byte[] excelBytes = IOUtils.toByteArray(inputStreamArgumentCaptor.getValue());
            InputStream excelData = new ByteArrayInputStream(excelBytes);
            assertTrue(excelData != null);
            Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(excelData);

            assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS1.equals(r0.toTeksti().getTeksti()))));
            assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS2.equals(r0.toTeksti().getTeksti()))));
            // Ei osallistujat ei tule mukaan
            assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS3.equals(r0.toTeksti().getTeksti()))));
            assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS4.equals(r0.toTeksti().getTeksti()))));
            assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS5.equals(r0.toTeksti().getTeksti()))));

        } finally {
            Mocks.reset();
            MockApplicationAsyncResource.clear();
        }
    }

    @Test
    public void testaaExcelinLuontiHakukohteenUlkopuolisillaHakijoilla() throws Throwable {
        Mocks.reset();
        try {
            List<ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
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
            Mockito.when(Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString())).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
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
                                    .build()
                    )
            );
            MockValintaperusteetAsyncResource.setValintakokeetResult(
                    Arrays.asList(valintakoe()
                                    .setTunniste(TUNNISTE1)
                                    .setNimi(VALINTAKOENIMI1)
                                    .setSelvitettyTunniste(SELVITETTY_TUNNISTE1)
                                    .build(),
                            valintakoe()
                                    .setTunniste(TUNNISTE2)
                                    .setNimi(VALINTAKOENIMI2)
                                    .setSelvitettyTunniste(SELVITETTY_TUNNISTE2)
                                    .build())
            );

            MockApplicationAsyncResource.setResultByOid(Arrays.asList(
                    hakemus()
                            .setOid(HAKEMUS1)
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS2)
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS3)
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS4)
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS5)
                            .build()
            ));

            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

            ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                    Mockito.anyString(),
                    Mockito.anyString(),
                    Mockito.anyLong(),
                    Mockito.anyList(),
                    Mockito.anyString(),
                    inputStreamArgumentCaptor.capture()
            )).thenReturn(Observable.just(Response.ok("OK").build()));


            DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot();
            lisatiedot.setValintakoeTunnisteet(Arrays.asList(SELVITETTY_TUNNISTE1));
            Response r = valintakoekutsutResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid", HAKUKOHDE1)
                            .post(Entity.entity(lisatiedot, "application/json"));
            assertEquals(200, r.getStatus());
            byte[] excelBytes = IOUtils.toByteArray(inputStreamArgumentCaptor.getValue());
            InputStream excelData = new ByteArrayInputStream(excelBytes);
            assertTrue(excelData != null);
            Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(excelData);
            assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS1.equals(r0.toTeksti().getTeksti()))));
            assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS2.equals(r0.toTeksti().getTeksti()))));
            // Ei osallistujat ei tule mukaan
            assertTrue(!rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS3.equals(r0.toTeksti().getTeksti()))));
            assertTrue(!rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS4.equals(r0.toTeksti().getTeksti()))));
            assertTrue(!rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS5.equals(r0.toTeksti().getTeksti()))));

        } finally {
            Mocks.reset();
        }
    }
}
