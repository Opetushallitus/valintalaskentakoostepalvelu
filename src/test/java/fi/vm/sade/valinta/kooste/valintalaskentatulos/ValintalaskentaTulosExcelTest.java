package fi.vm.sade.valinta.kooste.valintalaskentatulos;

import com.google.common.util.concurrent.Futures;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.util.ExcelImportUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valintalaskenta.domain.dto.OsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import junit.framework.Assert;
import org.apache.poi.util.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.lisatiedot;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.hakemusOsallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.hakukohdeJaValintakoe;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintakoe;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintaperusteet;

/**
 * @author Jussi Jartamo
 */
public class ValintalaskentaTulosExcelTest {

    final static Logger LOG = LoggerFactory.getLogger(ValintalaskentaTulosExcelTest.class);
    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource valintakoekutsutResource = new HttpResource(root + "/valintalaskentaexcel/valintakoekutsut/aktivoi");
    final String HAKU1 = "HAKU1";
    final String HAKUKOHDE1 = "HAKUKOHDE1";
    final String TARJOAJA1 = "TARJOAJA1";
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

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void testaaExcelinLuontiKaikkiOsallistuu() throws Throwable {
        Mocks.reset();
        try {
            List<ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList();
            Mockito.when(
                    Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString(), Mockito.any(), Mockito.any())).then(
                    answer -> {
                        Consumer<List<Koodi>> callback = (Consumer<List<Koodi>>) answer.getArguments()[1];
                        if (callback != null) {
                            callback.accept(Collections.emptyList());
                        }
                        return new PeruutettavaImpl(Futures.immediateFuture(null));
                    });
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
                            //
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
                    ) // Osallistumiset
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
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(), Mockito.anyString(),
                    inputStreamArgumentCaptor.capture(), Mockito.any(Consumer.class), Mockito.any(Consumer.class))).thenReturn(new PeruutettavaImpl(Futures.immediateFuture(null)));


            DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot();
            lisatiedot.setValintakoeTunnisteet(Arrays.asList(SELVITETTY_TUNNISTE1, SELVITETTY_TUNNISTE2));
            Response r =
                    valintakoekutsutResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid", HAKUKOHDE1)
                            .post(Entity.entity(lisatiedot,
                                    "application/json"));
            Assert.assertEquals(200, r.getStatus());
            byte[] excelBytes = IOUtils.toByteArray(inputStreamArgumentCaptor.getValue());
            //IOUtils.copy(new ByteArrayInputStream(excelBytes), new FileOutputStream("e.xls"));
            InputStream excelData = new ByteArrayInputStream(excelBytes);
            Assert.assertTrue(excelData != null);
            Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(excelData);

            Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS1.equals(r0.toTeksti().getTeksti()))));
            Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS2.equals(r0.toTeksti().getTeksti()))));
            // Ei osallistujat ei tule mukaan
            Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS3.equals(r0.toTeksti().getTeksti()))));
            Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS4.equals(r0.toTeksti().getTeksti()))));
            Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS5.equals(r0.toTeksti().getTeksti()))));

        } finally {
            Mocks.reset();
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
                                    //.setValintakoeOid(VALINTAKOE1)
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
                                    //.setValintakoeOid(VALINTAKOE1)
                            .setTunniste(SELVITETTY_TUNNISTE1)
                            .setOsallistuu()
                            .build()
                            .build()
                            .build()
                            .build());
            Mockito.when(
                    Mocks.getKoodistoAsyncResource().haeKoodisto(Mockito.anyString(), Mockito.any(), Mockito.any())).then(
                    answer -> {
                        Consumer<List<Koodi>> callback = (Consumer<List<Koodi>>) answer.getArguments()[1];
                        if (callback != null) {
                            callback.accept(Collections.emptyList());
                        }
                        return new PeruutettavaImpl(Futures.immediateFuture(null));
                    });
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
                    ) // Osallistumiset
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
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(), Mockito.anyString(),
                    inputStreamArgumentCaptor.capture(), Mockito.any(Consumer.class), Mockito.any(Consumer.class))).thenReturn(new PeruutettavaImpl(Futures.immediateFuture(null)));


            DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot();
            lisatiedot.setValintakoeTunnisteet(Arrays.asList(SELVITETTY_TUNNISTE1));
            Response r =
                    valintakoekutsutResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid", HAKUKOHDE1)
                            .post(Entity.entity(lisatiedot,
                                    "application/json"));
            Assert.assertEquals(200, r.getStatus());
            byte[] excelBytes = IOUtils.toByteArray(inputStreamArgumentCaptor.getValue());
            //IOUtils.copy(new ByteArrayInputStream(excelBytes), new FileOutputStream("e.xls"));//new File("")));
            InputStream excelData = new ByteArrayInputStream(excelBytes);
            Assert.assertTrue(excelData != null);
            Collection<Rivi> rivit = ExcelImportUtil.importHSSFExcel(excelData);
        /*
        rivit.forEach(r0 -> {
            System.err.println(r0);
        });
        */
            Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS1.equals(r0.toTeksti().getTeksti()))));
            Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS2.equals(r0.toTeksti().getTeksti()))));
            // Ei osallistujat ei tule mukaan
            Assert.assertTrue(!rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS3.equals(r0.toTeksti().getTeksti()))));
            Assert.assertTrue(!rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS4.equals(r0.toTeksti().getTeksti()))));
            Assert.assertTrue(!rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS5.equals(r0.toTeksti().getTeksti()))));

        } finally {
            Mocks.reset();
        }
    }
}
