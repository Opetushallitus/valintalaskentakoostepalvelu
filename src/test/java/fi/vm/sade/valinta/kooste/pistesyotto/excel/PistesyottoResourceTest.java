package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.gson.GsonBuilder;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.excel.DataRivi;
import fi.vm.sade.valinta.kooste.excel.Excel;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.arvo.TekstiArvo;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.util.ExcelImportUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.*;

import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.*;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.*;
/**
 * @author Jussi Jartamo
 */
public class PistesyottoResourceTest {
    final static Logger LOG = LoggerFactory.getLogger(PistesyottoResourceTest.class);
    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource pistesyottoTuontiResource = new HttpResource(root + "/pistesyotto/tuonti");
    final HttpResource pistesyottoVientiResource = new HttpResource(root + "/pistesyotto/vienti");
    final String HAKU1 = "HAKU1";
    final String HAKUKOHDE1 = "HAKUKOHDE1";
    final String TARJOAJA1 = "TARJOAJA1";
    final String VALINTAKOE1 = "VALINTAKOE1";
    final String HAKEMUS1 = "HAKEMUS1";
    final String HAKEMUS2 = "HAKEMUS2";
    final String HAKEMUS3 = "HAKEMUS3";
    final String TUNNISTE1 = "TUNNISTE1";
    final String OSALLISTUMISENTUNNISTE1 = TUNNISTE1 + "-OSALLISTUMINEN";

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void pistesyottoVientiTest() throws Throwable {
        List< ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
                osallistuminen()
                        .setHakemusOid(HAKEMUS1)
                        .hakutoive()
                        .valinnanvaihe()
                        .valintakoe()
                        .setValintakoeOid(VALINTAKOE1)
                        .setTunniste(TUNNISTE1)
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
                        .setValintakoeOid(VALINTAKOE1)
                        .setTunniste(TUNNISTE1)
                        .setOsallistuu()
                        .build()
                        .build()
                        .build()
                        .build());
        List<ValintaperusteDTO> valintaperusteet = Arrays.asList(
                valintaperusteet()
                        .setKuvaus(TUNNISTE1)
                        .setTunniste(TUNNISTE1)
                        .setOsallistumisenTunniste(TUNNISTE1)
                        .setLukuarvofunktio()
                        .setArvot("1", "2", "3")
                        .build()
        );

        MockValintaperusteetAsyncResource.setValintaperusteetResultReference(
                valintaperusteet
        );
        MockValintaperusteetAsyncResource.setHakukohdeResult(
                Arrays.asList(
                        hakukohdeJaValintakoe()
                                .addValintakoe(VALINTAKOE1)
                                .build()
                )
        );
        MockApplicationAsyncResource.setResult(Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .build()
        ));
        MockApplicationAsyncResource.setResultByOid(Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS2)
                        .build()
        ));
        MockApplicationAsyncResource.setAdditionalDataResult(Arrays.asList(
                lisatiedot()
                    .setOid(HAKEMUS1)
                        .build()));
        MockApplicationAsyncResource.setAdditionalDataResultByOid(Arrays.asList(
                lisatiedot()
                        .setOid(HAKEMUS2)
                        .build()));

        MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

        ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
        Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(), Mockito.anyString(),
                inputStreamArgumentCaptor.capture(), Mockito.any(Consumer.class), Mockito.any(Consumer.class))).thenReturn(new PeruutettavaImpl(Futures.immediateFuture(null)));

        Response r =
                pistesyottoVientiResource.getWebClient()
                        .query("hakuOid", HAKU1)
                        .query("hakukohdeOid",HAKUKOHDE1)
                        .post(Entity.entity("",
                                "application/json"));
        Assert.assertEquals(200, r.getStatus());
        InputStream excelData = inputStreamArgumentCaptor.getValue();
        Assert.assertTrue(excelData != null);
        Collection<Rivi> rivit = ExcelImportUtil.importExcel(excelData);
        Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS1.equals(r0.toTeksti().getTeksti()))));
        Assert.assertTrue(rivit.stream().anyMatch(rivi -> rivi.getSolut().stream().anyMatch(r0 -> HAKEMUS2.equals(r0.toTeksti().getTeksti()))));
    }

    @Test
    public void pistesyottoTest() {
        List< ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
                osallistuminen()
                        .setHakemusOid(HAKEMUS1)
                        .hakutoive()
                        .valinnanvaihe()
                        .valintakoe()
                        .setValintakoeOid(VALINTAKOE1)
                        .setTunniste(TUNNISTE1)
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
                        .setValintakoeOid(VALINTAKOE1)
                        .setTunniste(TUNNISTE1)
                        .setOsallistuu()
                        .build()
                        .build()
                        .build()
                        .build(),
                osallistuminen()
                        .setHakemusOid(HAKEMUS3)
                        .hakutoive()
                        .valinnanvaihe()
                        .valintakoe()
                        .setValintakoeOid(VALINTAKOE1)
                        .setTunniste(TUNNISTE1)
                        .setEiOsallistu()
                        .build()
                        .build()
                        .build()
                        .build()
        );
        List<ValintaperusteDTO> valintaperusteet = Arrays.asList(
                valintaperusteet()
                        .setKuvaus(TUNNISTE1)
                        .setTunniste(TUNNISTE1)
                        .setOsallistumisenTunniste(OSALLISTUMISENTUNNISTE1)
                        .setLukuarvofunktio()
                        .setArvot("1", "2", "3")
                        .build()
        );

        MockValintaperusteetAsyncResource.setValintaperusteetResultReference(valintaperusteet);
        MockApplicationAsyncResource.setAdditionalDataResult(Arrays.asList(
                lisatiedot()
                    .setOid(HAKEMUS1).build(),
                lisatiedot()
                        .setOid(HAKEMUS3).build()));
        MockApplicationAsyncResource.setAdditionalDataResultByOid(
                Arrays.asList(
                        lisatiedot()
                                .setOid(HAKEMUS2)
                                .build(),
                        lisatiedot()
                                .setOid(HAKEMUS3).build()
                )
        );
        MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
        PistesyottoExcel excel = new PistesyottoExcel(HAKU1, HAKUKOHDE1,
                TARJOAJA1, "", "", "",
                Arrays.asList(
                        hakemus()
                                .setOid(HAKEMUS1)
                                .build(),
                        hakemus()
                                .setOid(HAKEMUS2)
                                .build(),
                        hakemus()
                                .setOid(HAKEMUS3)
                                .build()
                ),
                Sets.newHashSet(Arrays.asList(VALINTAKOE1)), // KAIKKI KUTSUTAAN TUNNISTEET
                Arrays.asList(VALINTAKOE1), // TUNNISTEET
                osallistumistiedot,
                valintaperusteet,
                Arrays.asList(
                        lisatiedot()
                                .setOid(HAKEMUS1)
                                .addLisatieto(TUNNISTE1, "3")
                                .build(),
                        lisatiedot()
                                .setOid(HAKEMUS2)
                                .addLisatieto(TUNNISTE1, "2")
                                .build(),
                        lisatiedot()
                                .setOid(HAKEMUS3)
                                .addLisatieto(TUNNISTE1, "")
                                .build()
                ));

        Response r =
                pistesyottoTuontiResource.getWebClient()
                        .query("hakuOid", HAKU1)
                        .query("hakukohdeOid",HAKUKOHDE1)
                        .post(Entity.entity(excel.getExcel().vieXlsx(),
                                MediaType.APPLICATION_OCTET_STREAM));
        Assert.assertEquals(200, r.getStatus());
        List<ApplicationAdditionalDataDTO> tuodutLisatiedot =
        MockApplicationAsyncResource.
        getAdditionalDataInput();
        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(tuodutLisatiedot));
        Assert.assertEquals("Oletettiin että hakukohteen hakemukselle että ulkopuoliselle hakemukselle tuotiin lisätiedot!",2, tuodutLisatiedot.size());
    }
}
