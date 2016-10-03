package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.lisatiedot;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.hakukohdeJaValintakoe;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintaperuste;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.Solu;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec;
import fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec;
import fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec;
import fi.vm.sade.valinta.kooste.util.DokumenttiProsessiPoller;
import fi.vm.sade.valinta.kooste.util.ExcelImportUtil;
import fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
public class PistesyottoResourceTest {
    final static Logger LOG = LoggerFactory.getLogger(PistesyottoResourceTest.class);
    public static final long DEFAULT_POLL_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5L); //5sec
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource pistesyottoTuontiResource = new HttpResource(root + "/pistesyotto/tuonti");
    final HttpResource pistesyottoVientiResource = new HttpResource(root + "/pistesyotto/vienti");
    final HttpResource pistesyottoUlkoinenTuontiResource = new HttpResource(root + "/pistesyotto/ulkoinen");
    final String HAKU1 = "HAKU1";
    final String HAKUKOHDE1 = "HAKUKOHDE1";
    final String TARJOAJA1 = "TARJOAJA1";
    final String VALINTAKOE1 = "VALINTAKOE1";
    final String KIELIKOE = "KIELIKOE";
    final String HAKEMUS1 = "HAKEMUS1";
    final String HAKEMUS2 = "HAKEMUS2";
    final String HAKEMUS3 = "HAKEMUS3";
    final String TUNNISTE1 = "TUNNISTE1";
    final String TUNNISTE2 = "TUNNISTE2";
    final String PERSONOID1 = "1.2.3.4.111";
    final String PERSONOID2 = "1.2.3.4.222";
    final String PERSONOID3 = "1.2.3.4.333";
    final String KIELIKOE_TUNNISTE = "kielikoe_fi";
    final String AMMATILLINEN_KIELIKOE_TYYPPI = SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE;
    final String OSALLISTUMISENTUNNISTE1 = TUNNISTE1 + "-OSALLISTUMINEN";
    final String OSALLISTUMISENTUNNISTE2 = TUNNISTE2 + "-OSALLISTUMINEN";
    final String KIELIKOE_OSALLISTUMISENTUNNISTE = KIELIKOE_TUNNISTE + "-OSALLISTUMINEN";
    private final List<ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
            osallistuminen()
                    .setHakemusOid(HAKEMUS1)
                    .hakutoive()
                    .valinnanvaihe()
                    .valintakoe()
                    .setValintakoeOid(VALINTAKOE1)
                    .setTunniste(TUNNISTE1)
                    .setOsallistuu()
                    .build()
                    .valintakoe()
                    .setValintakoeOid(KIELIKOE)
                    .setTunniste(KIELIKOE_TUNNISTE)
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
                    .valintakoe()
                    .setValintakoeOid(KIELIKOE)
                    .setTunniste(KIELIKOE_TUNNISTE)
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
                    .valintakoe()
                    .setValintakoeOid(KIELIKOE)
                    .setTunniste(KIELIKOE_TUNNISTE)
                    .setEiOsallistu()
                    .build()
                    .build()
                    .build()
                    .build()
    );
    private final List<ValintaperusteDTO> valintaperusteet = Arrays.asList(
        valintaperuste()
            .setKuvaus(TUNNISTE1)
            .setTunniste(TUNNISTE1)
            .setOsallistumisenTunniste(OSALLISTUMISENTUNNISTE1)
            .setLukuarvofunktio()
            .setArvot("1", "2", "3")
            .build(),
        valintaperuste()
            .setKuvaus(TUNNISTE2)
            .setTunniste(TUNNISTE2)
            .setOsallistumisenTunniste(OSALLISTUMISENTUNNISTE2)
            .setTotuusarvofunktio()
            .build(),
        valintaperuste()
            .setKuvaus(KIELIKOE_TUNNISTE)
            .setTunniste(KIELIKOE_TUNNISTE)
            .setOsallistumisenTunniste(KIELIKOE_OSALLISTUMISENTUNNISTE)
            .setTotuusarvofunktio()
            .build()
    );
    private final ObjectMapper objectMapper = new ObjectMapper();
    private PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(HAKU1, HAKUKOHDE1,
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
                                            .setPersonOid(PERSONOID1)
                                            .addLisatieto(TUNNISTE1, "3")
                                            .addLisatieto(TUNNISTE2, "true")
                                            .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                                            //.addLisatieto(KIELIKOE_TUNNISTE, "true")
                                            .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                                            .build(),
                                    lisatiedot()
                                            .setOid(HAKEMUS2)
                                            .setPersonOid(PERSONOID2)
                                            .addLisatieto(TUNNISTE1, "2")
                                            .addLisatieto(TUNNISTE2, "true")
                                            .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                                            //.addLisatieto(KIELIKOE_TUNNISTE, "true")
                                            .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                                            .build(),
                                    lisatiedot()
                                            .setOid(HAKEMUS3)
                                            .setPersonOid(PERSONOID3)
                                            .addLisatieto(TUNNISTE1, "")
                                            .build()
                            ), ImmutableMap.of(
                                PERSONOID1,
                                Arrays.asList(new Arvosana(
                                        null, null, KIELIKOE, true, "", "", new HashMap<>(), new Arvio("true", null, null), "FI")),
                                PERSONOID2,
                                Arrays.asList(new Arvosana(
                                        null, null, KIELIKOE, true, "", "", new HashMap<>(), new Arvio("false", null, null), "FI"))
                            )
                    );

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void pistesyottoUlkoinenTuontiResource() throws Exception {
        final String tunniste1 = "1234";
        final String tunniste2 = "1235";
        final String hakemusOid1 = "12316.7.7.74";
        final String hakemusPersonOid1 = "1.2.4124.41214";
        final String hakemusOid2 = "12316.7.7.998";
        final String hakemusPersonOid2 = "1.2.4124.83219";
        final String hakemusOid3 = "12316.7.7.997";
        final String hakemusInvalidPersonOid3 = "1.2.4124.83215";
        final String hakukohdeOid1 = "1.2.3.4";
        final String hakukohdeOid2 = "1.2.3.5";
        final String hakemusOid4 = "12316.7.7.996";
        final String hakemusPersonOid4 = "1.2.4124.83142";
        final String hakemusOid5 = "12316.7.7.995";
        final String hakemusPersonOid5 = "1.2.4124.83141";

        cleanMocks();

        Hakemus hakemus1 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid1).addHakutoive(hakukohdeOid1).setPersonOid(hakemusPersonOid1).build();
        Hakemus hakemus2 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid2).addHakutoive(hakukohdeOid1).setPersonOid(hakemusPersonOid2).build();
        Hakemus hakemus3 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid3).addHakutoive(hakukohdeOid1).setPersonOid(hakemusInvalidPersonOid3).build();
        Hakemus hakemus4 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid4).addHakutoive(hakukohdeOid1).setPersonOid(hakemusPersonOid4).build();
        Hakemus hakemus5 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid5).addHakutoive(hakukohdeOid2).setPersonOid(hakemusPersonOid5).build();


        List<Hakemus> hakemuses = Arrays.asList(hakemus1, hakemus2, hakemus3, hakemus4, hakemus5);
        MockApplicationAsyncResource.setResult(hakemuses);

        HakukohdeJaValintaperusteDTO vp = new HakukohdeJaValintaperusteDTO(hakukohdeOid1,
                Arrays.asList(
                        new ValintaperusteetSpec.ValintaperusteBuilder().setTunniste(tunniste1).setLukuarvofunktio().setArvot("8.34", "5.00").build(),
                        new ValintaperusteetSpec.ValintaperusteBuilder().setTunniste(tunniste2).setLukuarvofunktio().setMax("9.00").setMin("5.00").build()));
        HakukohdeJaValintaperusteDTO vp2 = new HakukohdeJaValintaperusteDTO(hakukohdeOid2,
                Arrays.asList(
                        new ValintaperusteetSpec.ValintaperusteBuilder().setTunniste(tunniste1).setLukuarvofunktio().setArvot("8.34", "5.00").build(),
                        new ValintaperusteetSpec.ValintaperusteBuilder().setTunniste(tunniste2).setLukuarvofunktio().setMax("9.00").setMin("5.00").build()));
        MockValintaperusteetAsyncResource.setHakukohdeValintaperusteResult(Arrays.asList(vp, vp2));

        HakukohdeJaValintakoeDTO vk = new HakukohdeJaValintakoeDTO(hakukohdeOid1,
                Arrays.asList(
                        new ValintaperusteetSpec.ValintakoeDTOBuilder().setTunniste(tunniste1).setKaikkiKutsutaan().build(),
                        new ValintaperusteetSpec.ValintakoeDTOBuilder().setTunniste(tunniste2).setKaikkiKutsutaan().build()));
        MockValintaperusteetAsyncResource.setHakukohdeResult(Arrays.asList(vk));

        List<ValintakoeOsallistuminenDTO> osallistuminenDTOs =
                Arrays.asList(new ValintalaskentaSpec.ValintakoeOsallistuminenBuilder().hakutoive().setHakukohdeOid(hakukohdeOid1).build().build());
        MockValintalaskentaValintakoeAsyncResource.setResult(osallistuminenDTOs);

        String requestBody = IOUtils.toString(new ClassPathResource("pistesyotto/ulkoinen_tuonti.json").getInputStream());
        List<HakemusDTO> hakemusDTOs  = pistesyottoVientiResource.gson().fromJson(requestBody, new TypeToken<List<HakemusDTO>>() {}.getType());
        assertEquals(6, hakemusDTOs.size());
        Response response = pistesyottoUlkoinenTuontiResource.getWebClient()
                .query("hakuOid", "HAKUOID")
                .post(Entity.entity(requestBody, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());

        String responseAsString = response.readEntity(String.class);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(responseAsString);
        String prettyJsonString = gson.toJson(je);


        System.out.println(prettyJsonString);
    }

    @Test
    public void pistesyottoVientiTest() throws Throwable {
        cleanMocks();
        try {
            List<ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
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
                    valintaperuste()
                            .setKuvaus(TUNNISTE1)
                            .setTunniste(TUNNISTE1)
                            .setOsallistumisenTunniste(TUNNISTE1)
                            .setLukuarvofunktio()
                            .setArvot("1", "2", "3")
                            .build()
            );

            MockValintaperusteetAsyncResource.setValintaperusteetResult(
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
                            .setHenkilotunnus("123456-789x")
                            .build()
            ));
            MockApplicationAsyncResource.setResultByOid(Arrays.asList(
                    hakemus()
                            .setOid(HAKEMUS2)
                            .setSyntymaaika("1.1.1900")
                            .build()
            ));
            MockApplicationAsyncResource.setAdditionalDataResult(Arrays.asList(
                    lisatiedot()
                            .setOid(HAKEMUS1)
                            .setEtunimiJaSukunimi("Hilla", "Hiiri")
                            .build()));
            MockApplicationAsyncResource.setAdditionalDataResultByOid(Arrays.asList(
                    lisatiedot()
                            .setOid(HAKEMUS2)
                            .setEtunimiJaSukunimi("Hellevi", "Hiiri")
                            .build()));

            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

            ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(), Mockito.anyString(),
                    inputStreamArgumentCaptor.capture(), Mockito.any(Consumer.class), Mockito.any(Consumer.class))).thenReturn(new PeruutettavaImpl(Futures.immediateFuture(null)));

            Response r =
                    pistesyottoVientiResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid", HAKUKOHDE1)
                            .post(Entity.entity("",
                                    "application/json"));
            assertEquals(200, r.getStatus());
            InputStream excelData = inputStreamArgumentCaptor.getValue();
            assertTrue(excelData != null);
            List<Rivi> rivit = ExcelImportUtil.importExcel(excelData);

            Rivi hakemus1Rivi = rivit.stream().filter(rivi -> rivi.getSolut().stream().anyMatch(solu -> HAKEMUS1.equals(solu.toTeksti().getTeksti()))).findFirst().get();
            assertRivi(hakemus1Rivi, new String[]{HAKEMUS1, "Hiiri, Hilla", "123456-789x", null, null, "Merkitsemättä"});
            Rivi hakemus2Rivi = rivit.stream().filter(rivi -> rivi.getSolut().stream().anyMatch(solu -> HAKEMUS2.equals(solu.toTeksti().getTeksti()))).findFirst().get();
            assertRivi(hakemus2Rivi, new String[]{HAKEMUS2, "Hiiri, Hellevi", null, "1.1.1900", null, "Merkitsemättä"});

            } finally {
            cleanMocks();
        }
    }

    @Test
    public void pistesyottoVienti2Test() throws Throwable {
        cleanMocks();
        try {
            List<ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
                    osallistuminen()
                            .setHakemusOid(HAKEMUS1)
                            .hakutoive()
                            .valinnanvaihe()
                            .valintakoe()
                            .setValintakoeOid(VALINTAKOE1)
                            .setTunniste(TUNNISTE1)
                            .setOsallistuu()
                            .build()
                            .valintakoe()
                            .setValintakoeOid(KIELIKOE)
                            .setTunniste(KIELIKOE_TUNNISTE)
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
                            .valintakoe()
                            .setValintakoeOid(KIELIKOE)
                            .setTunniste(KIELIKOE_TUNNISTE)
                            .setEiOsallistu()
                            .build()
                            .build()
                            .build()
                            .build());
            List<ValintaperusteDTO> valintaperusteet = Arrays.asList(
                    valintaperuste()
                            .setKuvaus(TUNNISTE1)
                            .setTunniste(TUNNISTE1)
                            .setOsallistumisenTunniste(OSALLISTUMISENTUNNISTE1)
                            .setTotuusarvofunktio()
                            .build(),
                    valintaperuste()
                            .setKuvaus(KIELIKOE_TUNNISTE)
                            .setTunniste(KIELIKOE_TUNNISTE)
                            .setOsallistumisenTunniste(KIELIKOE_OSALLISTUMISENTUNNISTE)
                            .setTotuusarvofunktio()
                            .build()
            );

            MockValintaperusteetAsyncResource.setValintaperusteetResult(
                    valintaperusteet
            );
            MockValintaperusteetAsyncResource.setHakukohdeResult(
                    Arrays.asList(
                            hakukohdeJaValintakoe()
                                    .addValintakoe(VALINTAKOE1)
                                    .addValintakoe(KIELIKOE)
                                    .build()
                    )
            );
            MockApplicationAsyncResource.setResult(Arrays.asList(
                    hakemus()
                            .setOid(HAKEMUS1)
                            .setHenkilotunnus("123456-789x")
                            .build(),
                    hakemus()
                            .setOid(HAKEMUS2)
                            .setSyntymaaika("1.1.1900")
                            .build()
            ));
            MockSuoritusrekisteriAsyncResource.setResult(
                    new SuoritusrekisteriSpec.OppijaBuilder()
                        .setOppijanumero(PERSONOID1)
                        .suoritus()
                        .setHenkiloOid(PERSONOID1)
                        .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                        .arvosana()
                        .setAine(KIELIKOE)
                        .setLisatieto("FI")
                        .setArvosana("true")
                        .build()
                        .build()
                        .suoritus()
                        .setHenkiloOid(PERSONOID1)
                        .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                        .arvosana()
                        .setAine(KIELIKOE)
                        .setLisatieto("FI")
                        .setArvosana("false")
                        .build()
                        .build()
                        .suoritus()
                        .setHenkiloOid(PERSONOID1)
                        .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                        .arvosana()
                        .setAine(KIELIKOE)
                        .setLisatieto("SV")
                        .setArvosana("false")
                        .build()
                        .build()
                        .build());
            MockApplicationAsyncResource.setAdditionalDataResult(Arrays.asList(
                    lisatiedot()
                            .setOid(HAKEMUS1)
                            .setPersonOid(PERSONOID1)
                            .setEtunimiJaSukunimi("Hilla", "Hiiri")
                            .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                            .build(),
                    lisatiedot()
                            .setOid(HAKEMUS2)
                            .setPersonOid(PERSONOID2)
                            .setEtunimiJaSukunimi("Hellevi", "Hiiri")
                            .addLisatieto(TUNNISTE1, "true")
                            .addLisatieto(OSALLISTUMISENTUNNISTE1, "OSALLISTUI")
                            .build()));

            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

            ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(), Mockito.anyString(),
                    inputStreamArgumentCaptor.capture(), Mockito.any(Consumer.class), Mockito.any(Consumer.class))).thenReturn(new PeruutettavaImpl(Futures.immediateFuture(null)));

            Response r =
                    pistesyottoVientiResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid", HAKUKOHDE1)
                            .post(Entity.entity("",
                                    "application/json"));
            assertEquals(200, r.getStatus());
            InputStream excelData = inputStreamArgumentCaptor.getValue();
            assertTrue(excelData != null);
            Collection<Rivi> rivit = ExcelImportUtil.importExcel(excelData);

            rivit.stream().forEach(rivi -> {
                System.out.println("RIVI: " + rivi.toString());
            });
            Rivi hakemus1Rivi = rivit.stream().filter(rivi -> rivi.getSolut().stream().anyMatch(solu -> HAKEMUS1.equals(solu.toTeksti().getTeksti()))).findFirst().get();
            assertRivi(hakemus1Rivi, new String[]{HAKEMUS1, "Hiiri, Hilla", "123456-789x", null, "Tyhjä", "Merkitsemättä", "Hyväksytty", "Osallistui"});
            Rivi hakemus2Rivi = rivit.stream().filter(rivi -> rivi.getSolut().stream().anyMatch(solu -> HAKEMUS2.equals(solu.toTeksti().getTeksti()))).findFirst().get();
            assertRivi(hakemus2Rivi, new String[]{HAKEMUS2, "Hiiri, Hellevi", null, "1.1.1900", "Kyllä", "Osallistui", "Tyhjä", "Merkitsemättä"});

        } finally {
            cleanMocks();
        }
    }


    private void assertRivi(Rivi rivi, String[] expectedSolut) {
        Solu[] solut = rivi.getSolut().toArray(new Solu[rivi.getSolut().size()]);
        assertTrue(solut.length == expectedSolut.length);
        for(int i = 0; i < solut.length; i++) {
            if(isBlank(expectedSolut[i])) {
                assertTrue(isBlank(solut[i].toTeksti().getTeksti()));
            } else {
                assertTrue(expectedSolut[i].equals(solut[i].toTeksti().getTeksti()));
            }
        }
    }

    @Test
    public void pistesyottoTuontiTest() {
        cleanMocks();
        try {
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
                valintaperuste()
                        .setKuvaus(TUNNISTE1)
                        .setTunniste(TUNNISTE1)
                        .setOsallistumisenTunniste(OSALLISTUMISENTUNNISTE1)
                        .setLukuarvofunktio()
                        .setArvot("1", "2", "3")
                        .build(),
                valintaperuste()
                        .setKuvaus(TUNNISTE2)
                        .setTunniste(TUNNISTE2)
                        .setOsallistumisenTunniste(OSALLISTUMISENTUNNISTE2)
                        .setLukuarvofunktio()
                        .setArvot("1", "2", "3")
                        .build()
        );

        MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
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
        MockValintaperusteetAsyncResource.setHakukohdeResult(Collections.singletonList(
                hakukohdeJaValintakoe().addValintakoe(VALINTAKOE1).addValintakoe(KIELIKOE).build()));
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
                ), null);

        Response r =
                pistesyottoTuontiResource.getWebClient()
                        .query("hakuOid", HAKU1)
                        .query("hakukohdeOid",HAKUKOHDE1)
                        .post(Entity.entity(excel.getExcel().vieXlsx(),
                                MediaType.APPLICATION_OCTET_STREAM));
        assertEquals(200, r.getStatus());
        List<ApplicationAdditionalDataDTO> tuodutLisatiedot = MockApplicationAsyncResource.getAdditionalDataInput();
        LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(tuodutLisatiedot));
        assertEquals("Oletettiin että hakukohteen hakemukselle että ulkopuoliselle hakemukselle tuotiin lisätiedot!", 3, tuodutLisatiedot.size());
        } finally {
            cleanMocks();
        }
    }

    @Test
    public void pistesyottoTuonti2Test() {
        cleanMocks();
        try {
            MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
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
            MockSuoritusrekisteriAsyncResource.setResult(
                new SuoritusrekisteriSpec.OppijaBuilder()
                    .setOppijanumero(PERSONOID1)
                    .suoritus()
                    .setHenkiloOid(PERSONOID1)
                    .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                    .arvosana()
                    .setAine(KIELIKOE)
                    .setLisatieto("FI")
                    .setArvosana("true")
                    .build()
                    .build()
                    .suoritus()
                    .setHenkiloOid(PERSONOID1)
                    .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                    .arvosana()
                    .setAine(KIELIKOE)
                    .setLisatieto("SV")
                    .setArvosana("false")
                    .build()
                    .build()
                    .build());
            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
            MockOrganisaationAsyncResource.setOrganisaationTyyppiHierarkia(
                    new OrganisaatioTyyppiHierarkia(1, Arrays.asList(
                            new OrganisaatioTyyppi(
                                    "1.2.246.562.10.45042175963",
                                    ImmutableMap.of("fi", "Itä-Savon koulutuskuntayhtymä"),
                                    Arrays.asList(
                                            new OrganisaatioTyyppi(
                                                    "1.2.246.562.10.45698499378",
                                                    ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto"),
                                                    Arrays.asList(
                                                            new OrganisaatioTyyppi(
                                                                    "1.2.3.44444.5",
                                                                    ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto, SAMI, kulttuuriala"),
                                                                    Arrays.asList(),
                                                                    null,
                                                                    Arrays.asList("TOIMIPISTE")
                                                            ),
                                                            new OrganisaatioTyyppi(
                                                                    "1.2.3.44444.6",
                                                                    ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto, SAMI, sirkusala"),
                                                                    Arrays.asList(),
                                                                    null,
                                                                    Arrays.asList("TOIMIPISTE")
                                                            )
                                                    ),
                                                    "oppilaitostyyppi_21#1",
                                                    Arrays.asList("OPPILAITOS")
                                            ),
                                            new OrganisaatioTyyppi(
                                                    "1.2.246.562.10.45698499379",
                                                    ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto 2"),
                                                    Arrays.asList(
                                                            new OrganisaatioTyyppi(
                                                                    "1.2.3.44444.7",
                                                                    ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto, SAMI, kulttuuriala"),
                                                                    Arrays.asList(),
                                                                    null,
                                                                    Arrays.asList("TOIMIPISTE")
                                                            ),
                                                            new OrganisaatioTyyppi(
                                                                    "1.2.3.44444.8",
                                                                    ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto, SAMI, sirkusala"),
                                                                    Arrays.asList(),
                                                                    null,
                                                                    Arrays.asList("TOIMIPISTE")
                                                            )
                                                    ),
                                                    "oppilaitostyyppi_21#1",
                                                    Arrays.asList("OPPILAITOS")
                                            )
                                    ),
                                    null,
                                    Arrays.asList("OPPILAITOS")
                            )
                    ))
            );
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
                                    .setPersonOid(PERSONOID1)
                                    .addLisatieto(TUNNISTE1, "3")
                                    .addLisatieto(TUNNISTE2, "true")
                                    .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                                    //.addLisatieto(KIELIKOE_TUNNISTE, "true")
                                    .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                                    .build(),
                            lisatiedot()
                                    .setOid(HAKEMUS2)
                                    .setPersonOid(PERSONOID2)
                                    .addLisatieto(TUNNISTE1, "2")
                                    .addLisatieto(TUNNISTE2, "true")
                                    .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                                    //.addLisatieto(KIELIKOE_TUNNISTE, "true")
                                    .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                                    .build(),
                            lisatiedot()
                                    .setOid(HAKEMUS3)
                                    .setPersonOid(PERSONOID3)
                                    .addLisatieto(TUNNISTE1, "")
                                    .build()
                    ), ImmutableMap.of(
                        PERSONOID1,
                        Arrays.asList(new Arvosana(
                                null, null, KIELIKOE, true, "", "", new HashMap<>(), new Arvio("true", null, null), "FI")),
                        PERSONOID2,
                        Arrays.asList(new Arvosana(
                                null, null, KIELIKOE, true, "", "", new HashMap<>(), new Arvio("false", null, null), "FI"))
                    )
            );

            Response r =
                    pistesyottoTuontiResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid",HAKUKOHDE1)
                            .post(Entity.entity(excel.getExcel().vieXlsx(),
                                    MediaType.APPLICATION_OCTET_STREAM));
            assertEquals(200, r.getStatus());
            List<ApplicationAdditionalDataDTO> tuodutLisatiedot = MockApplicationAsyncResource.getAdditionalDataInput();
            LOG.error("{}", new GsonBuilder().setPrettyPrinting().create().toJson(tuodutLisatiedot));
            assertEquals("Oletettiin että hakukohteen hakemukselle että ulkopuoliselle hakemukselle tuotiin lisätiedot!", 3, tuodutLisatiedot.size());
            assertFalse("Kielikokeita ei saa löytyä hakemuksen lisätiedoista", tuodutLisatiedot.stream().anyMatch(a -> a.getAdditionalData().containsKey("kielikoe_fi")));
            MockSuoritusrekisteriAsyncResource.suorituksetRef.get().stream().forEach(s -> LOG.error(s.toString()));
            assertEquals("Suorituksilla on oikea myöntäjä", 2, MockSuoritusrekisteriAsyncResource.suorituksetRef.get().stream().filter(s -> s.getMyontaja().equals("1.2.246.562.10.45698499378")).count());
            MockSuoritusrekisteriAsyncResource.arvosanatRef.get().stream().forEach(a -> LOG.error(a.toString()));
            assertEquals("Kielikokeiden suoritukset löytyvät suresta", 2, MockSuoritusrekisteriAsyncResource.suorituksetRef.get().size());
            assertEquals("Kielikokeiden arvosanat löytyvät suresta", 2, MockSuoritusrekisteriAsyncResource.arvosanatRef.get().size());
        } finally {
            cleanMocks();
        }
    }

    public void cleanMocks() {
        Mocks.reset();
        MockApplicationAsyncResource.clear();
    }

    @Test
    public void pistesyottoTuontiVirheellisestiSortatuillaHakemuksillaTest() throws Throwable {
        cleanMocks();
        try {
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
                valintaperuste()
                        .setKuvaus(TUNNISTE1)
                        .setTunniste(TUNNISTE1)
                        .setOsallistumisenTunniste(OSALLISTUMISENTUNNISTE1)
                        .setLukuarvofunktio()
                        .setArvot("1", "2", "3")
                        .build(),
                valintaperuste()
                        .setKuvaus(TUNNISTE2)
                        .setTunniste(TUNNISTE2)
                        .setOsallistumisenTunniste(OSALLISTUMISENTUNNISTE2)
                        .setLukuarvofunktio()
                        .setArvot("1", "2", "3")
                        .build()
        );

        MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
        MockApplicationAsyncResource.setAdditionalDataResult(Arrays.asList(
                lisatiedot()
                        .setEtunimiJaSukunimi("Essi1", "Hakemus1")
                        .setOid(HAKEMUS1).build(),
                lisatiedot()
                        .setOid(HAKEMUS3).build()));
        MockApplicationAsyncResource.setAdditionalDataResultByOid(
                Arrays.asList(
                        lisatiedot()
                                .setEtunimiJaSukunimi("Esko2", "Hakemus2")
                                .setOid(HAKEMUS2)
                                .build(),
                        lisatiedot()
                                .setEtunimiJaSukunimi("Elli3", "Hakemus3")
                                .setOid(HAKEMUS3).build()
                )
        );
        MockValintaperusteetAsyncResource.setHakukohdeResult(Collections.singletonList(
                hakukohdeJaValintakoe().addValintakoe(VALINTAKOE1).addValintakoe(KIELIKOE).build()));
        MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
        Response r =
                pistesyottoTuontiResource.getWebClient()
                        .query("hakuOid", HAKU1)
                        .query("hakukohdeOid",HAKUKOHDE1)
                        .post(Entity.entity(
                                PistesyottoResourceTest.class.getResourceAsStream("/virheellisesti_sortattu_excel.xlsx"),
                                MediaType.APPLICATION_OCTET_STREAM));
        assertEquals(200, r.getStatus());
        List<ApplicationAdditionalDataDTO> tuodutLisatiedot = MockApplicationAsyncResource.getAdditionalDataInput();
        assertEquals("Oletettiin että hakukohteen hakemukselle että ulkopuoliselle hakemukselle tuotiin lisätiedot!",null, tuodutLisatiedot);
        } finally {
            cleanMocks();
        }
    }

    @Test
    public void pistesyottoTuontiSureVirheellaTest() throws IOException {
            cleanMocks();
            try {
                MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
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
                MockValintaperusteetAsyncResource.setHakukohdeResult(Collections.singletonList(
                    hakukohdeJaValintakoe().addValintakoe(VALINTAKOE1).addValintakoe(KIELIKOE).build()));
                MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
                MockSuoritusrekisteriAsyncResource.setResult(
                    new SuoritusrekisteriSpec.OppijaBuilder()
                        .setOppijanumero(PERSONOID1)
                        .suoritus()
                        .setHenkiloOid(PERSONOID1)
                        .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                        .arvosana()
                        .setAine(KIELIKOE)
                        .setLisatieto("FI")
                        .setArvosana("true")
                        .build()
                        .build()
                        .suoritus()
                        .setHenkiloOid(PERSONOID1)
                        .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                        .arvosana()
                        .setAine(KIELIKOE)
                        .setLisatieto("SV")
                        .setArvosana("false")
                        .build()
                        .build()
                        .build());
                MockOrganisaationAsyncResource.setOrganisaationTyyppiHierarkia(
                        new OrganisaatioTyyppiHierarkia(1, Arrays.asList(
                                new OrganisaatioTyyppi(
                                        "1.2.246.562.10.45042175963",
                                        ImmutableMap.of("fi", "Itä-Savon koulutuskuntayhtymä"),
                                        Arrays.asList(
                                                new OrganisaatioTyyppi(
                                                        "1.2.246.562.10.45698499378",
                                                        ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto"),
                                                        Arrays.asList(
                                                                new OrganisaatioTyyppi(
                                                                        "1.2.3.44444.5",
                                                                        ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto, SAMI, kulttuuriala"),
                                                                        Arrays.asList(),
                                                                        null,
                                                                        Arrays.asList("TOIMIPISTE")
                                                                )
                                                        ),
                                                        "oppilaitostyyppi_21#1",
                                                        Arrays.asList("OPPILAITOS")
                                                )
                                        ),
                                        null,
                                        Arrays.asList("KOULUTUSTOIMIJA")
                                )
                        ))
                );
                MockSuoritusrekisteriAsyncResource.setPostException(Optional.of(
                    new HttpExceptionWithResponse("Something terrible happened", Response.serverError().entity("Boom").build())));

                Response r =
                        pistesyottoTuontiResource.getWebClient()
                                .query("hakuOid", HAKU1)
                                .query("hakukohdeOid",HAKUKOHDE1)
                                .post(Entity.entity(pistesyottoExcel.getExcel().vieXlsx(),
                                        MediaType.APPLICATION_OCTET_STREAM));
                assertEquals(200, r.getStatus());
                ProsessiId prosessiId = r.readEntity(ProsessiId.class);

                DokumenttiProsessiPoller.pollDokumenttiProsessi(root, prosessiId, prosessiStatusResponse -> {
                    if (prosessiStatusResponse.valmis() || prosessiStatusResponse.poikkeuksia()) {
                        String exceptionMessage = prosessiStatusResponse.poikkeukset.iterator().next().getViesti();
                        assertEquals("Something terrible happened", exceptionMessage);
                        return true;
                    }
                    return false;
                });
            } finally {
                MockSuoritusrekisteriAsyncResource.clear();
                cleanMocks();
            }
        }

}
