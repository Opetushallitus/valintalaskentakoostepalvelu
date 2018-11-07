package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.gson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToNotFound;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonWithParams;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnString;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.MockOpintopolkuCasAuthenticationFilter;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusOid;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvio;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Arvosana;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanat;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultTulos;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemuksenKoetulosYhteenveto;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakukohteenOsallistumistiedotDTO.KokeenOsallistumistietoDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HenkiloValilehtiDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.Osallistumistieto;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.PistesyottoValilehtiDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.OsallistuminenTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PistesyottoKoosteE2ETest extends PistesyotonTuontiTestBase {
    private static final ValintaperusteDTO kielikoeFi = new ValintaperusteDTO();

    static {
        kielikoeFi.setTunniste("kielikoe_fi");
        kielikoeFi.setOsallistuminenTunniste("kielikoe_fi-OSALLISTUMINEN");
        kielikoeFi.setVaatiiOsallistumisen(true);
    }

    private final MockServer fakeValintaPisteService = new MockServer();

    @Before
    public void startServer() {
        startShared();
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_UPDATE_1.2.246.562.10.00000000001");
    }

    @Test
    public void testKoostaaTyhjatPisteteidotJosParametriEiLoydy() throws Exception { //P

        HttpResourceBuilder.WebClientExposingHttpResource http = createClient(resourcesAddress + "/pistesyotto/koostetutPistetiedot/haku/testihaku/hakukohde/testihakukohde");

        List<ApplicationAdditionalDataDTO> applicationAdditionalDatas = readAdditionalData();
        List<String> hakemusOids = applicationAdditionalDatas.stream().map(ApplicationAdditionalDataDTO::getOid).collect(Collectors.toList());

        assertFalse(applicationAdditionalDatas.stream().anyMatch(p -> p.getAdditionalData().containsKey("kielikoe_fi")));

        mockHakuAppKutsu(applicationAdditionalDatas);
        mockValintaPisteServiceKutsu(applicationAdditionalDatas);

        mockSureKutsu(createOppijat());
        mockToReturnJson(GET, "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
                Collections.singletonList(kielikoeFi));
        mockToReturnJson(GET, "/valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/valintakoe/hakutoive/testihakukohde",
                Collections.<ValintakoeOsallistuminenDTO>emptyList());
        mockToNotFound(GET, "/ohjausparametrit-service/api/v1/rest/parametri/testihaku");

        Response r = http.getWebClient()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .get();

        assertEquals(200, r.getStatus());
        PistesyottoValilehtiDTO tulokset = gson().fromJson(new InputStreamReader((InputStream) r.getEntity()), PistesyottoValilehtiDTO.class);
        assertEquals(210, tulokset.getValintapisteet().size());
    }

    @Test
    public void testKoostaaPistetiedotHakemuksille() throws Exception {

        HttpResourceBuilder.WebClientExposingHttpResource http = createClient(resourcesAddress + "/pistesyotto/koostetutPistetiedot/haku/testihaku/hakukohde/testihakukohde");

        List<ApplicationAdditionalDataDTO> applicationAdditionalDatas = readAdditionalData();
        List<String> hakemusOids = applicationAdditionalDatas.stream().map(ApplicationAdditionalDataDTO::getOid).collect(Collectors.toList());

        assertFalse(applicationAdditionalDatas.stream().anyMatch(p -> p.getAdditionalData().containsKey("kielikoe_fi")));

        mockHakuAppKutsu(applicationAdditionalDatas);
        mockValintaPisteServiceKutsu(applicationAdditionalDatas);

        mockSureKutsu(createOppijat());
        mockToReturnJson(GET, "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
                Collections.singletonList(kielikoeFi));
        mockToReturnJson(GET, "/valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/valintakoe/hakutoive/testihakukohde",
                Collections.<ValintakoeOsallistuminenDTO>emptyList());
        mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/testihaku", new ParametritDTO());

        Response r = http.getWebClient()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .get();
        assertEquals(200, r.getStatus());

        PistesyottoValilehtiDTO tulokset = gson().fromJson(new InputStreamReader((InputStream) r.getEntity()), PistesyottoValilehtiDTO.class);
        BiFunction<String, String, String> readPistetieto = (personOid, key) ->
            tulokset.getValintapisteet().stream()
                .filter(p -> personOid.equals(p.applicationAdditionalDataDTO.getPersonOid()))
                .findFirst().get().applicationAdditionalDataDTO.getAdditionalData().get(key);

        assertEquals("true", readPistetieto.apply("1.2.246.562.24.77642460905", "kielikoe_fi"));
        assertEquals("", readPistetieto.apply("1.2.246.562.24.52321744679", "kielikoe_fi"));
        assertEquals("true", readPistetieto.apply("1.2.246.562.24.52321744679", "kielikoe_sv"));
        assertEquals("true", readPistetieto.apply("1.2.246.562.24.93793496064", "kielikoe_fi"));

        Function<List<ApplicationAdditionalDataDTO>, Integer> countAdditionalData = (pistetietoList) ->
            pistetietoList.stream().mapToInt(p -> p.getAdditionalData().values().size()).sum();

        int keysAddedByKielikoeFi = hakemusOids.size() * 2;
        int keysAddedByKielikoeSvSuoritus = 2;
        assertEquals(
                (keysAddedByKielikoeFi + keysAddedByKielikoeSvSuoritus),
                countAdditionalData.apply(tulokset.getValintapisteet().stream().map(p -> p.applicationAdditionalDataDTO).collect(Collectors.toList())).intValue()
        );
    }

    @Test
    public void testTallentaaKoostetutPistetiedotHakukohteelle() throws Exception {
        HttpResourceBuilder.WebClientExposingHttpResource http = createClient(resourcesAddress + "/pistesyotto/koostetutPistetiedot/haku/testihaku/hakukohde/testihakukohde");
        List<ApplicationAdditionalDataDTO> applicationAdditionalDataDtos = luePistetiedot("List_ApplicationAdditionalDataDTO.json");

        mockToReturnJson(GET, "/valintapiste-service/api/haku/testihaku/hakukohde/testihakukohde",
            applicationAdditionalDataDtos.stream().map(APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET).collect(Collectors.toList())
        );
        mockToReturnJson(POST, "/lomake-editori/api/external/valintalaskenta",
                new ArrayList<AtaruHakemus>());
        mockToReturnJson(POST, "/haku-app/applications/listfull",
            applicationAdditionalDataDtos.stream().map(p -> new HakemusOid(p.getOid())).collect(Collectors.toList())
        );
        mockOrganisaatioKutsu();
        mockTarjontaHakukohdeCall();
        mockSureKutsu(createOppijat());

        final Semaphore suoritusCounter = new Semaphore(0);
        final Semaphore arvosanaCounter = new Semaphore(0);
        final Semaphore deleteCounter = new Semaphore(0);
        mockSuoritusrekisteri(suoritusCounter, arvosanaCounter);
        mockSuoritusrekisteriDelete(deleteCounter);

        final Semaphore pisteCounter = new Semaphore(0);
        mockPistetietoTallennus(pisteCounter, 209);

        Response r = http.getWebClient()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .put(new Gson().toJson(applicationAdditionalDataDtos));
        assertEquals(204, r.getStatus());

        try {
            Assert.assertTrue(suoritusCounter.tryAcquire(2, 10, TimeUnit.SECONDS));
            Assert.assertTrue(arvosanaCounter.tryAcquire(2, 10, TimeUnit.SECONDS));
            Assert.assertTrue(pisteCounter.tryAcquire(1, 10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    @Test
    public void testTallentaaKoostetutPistetiedotJosEiKielikokeita() throws Exception {
        ApplicationAdditionalDataDTO applicationAdditionaData = luePistetiedot("List_ApplicationAdditionalDataDTO.json").get(0);
        String hakijaOid = applicationAdditionaData.getPersonOid();

        HttpResourceBuilder.WebClientExposingHttpResource http = createClient(resourcesAddress + "/pistesyotto/koostetutPistetiedot/hakemus/" + applicationAdditionaData.getOid());
        applicationAdditionaData.getAdditionalData().remove("kielikoe_fi");

        mockOrganisaatioKutsu();
        mockTarjontaHakukohdeCall();
        mockValintakoe();
        mockTarjontaHakukohdeRyhmaCall();

        Hakemus hakemusHakuAppista = new Hakemus();
        hakemusHakuAppista.setAdditionalInfo(applicationAdditionaData.getAdditionalData());
        hakemusHakuAppista.setPersonOid(hakijaOid);
        hakemusHakuAppista.setOid(applicationAdditionaData.getOid());
        hakemusHakuAppista.setApplicationSystemId("testihaku");
        Answers answers = new Answers();
        answers.setHenkilotiedot(new HashMap<>());
        answers.getHenkilotiedot().put("Etunimet", "Frank");
        answers.getHenkilotiedot().put("Sukunimi", "Tester");
        answers.setHakutoiveet(new HashMap<>());
        answers.getHakutoiveet().put("preference1-Koulutus-id", "testihakukohde");
        hakemusHakuAppista.setAnswers(answers);
        mockToReturnJson(POST,
                "/lomake-editori/api/external/valintalaskenta",
                new ArrayList<AtaruHakemus>());
        mockToReturnJson(GET,
                "/haku-app/applications/" + applicationAdditionaData.getOid(),
                hakemusHakuAppista
        );
        mockToReturnJson(POST,
            "/valintapiste-service/api/pisteet-with-hakemusoids",
            Collections.singletonList(APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET.apply(applicationAdditionaData)));
        final Semaphore suoritusCounter = new Semaphore(0);
        final Semaphore arvosanaCounter = new Semaphore(0);
        final Semaphore deleteCounter = new Semaphore(0);
        mockSuoritusrekisteri(suoritusCounter, arvosanaCounter);
        mockSuoritusrekisteriDelete(deleteCounter);

        final Semaphore pisteCounter = new Semaphore(0);
        mockPistetietoNormiTallennus(pisteCounter);

        Response r = http.getWebClient()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .put(new Gson().toJson(applicationAdditionaData));
        assertEquals(204, r.getStatus());

        try {
            Assert.assertTrue(suoritusCounter.tryAcquire(0, 10, TimeUnit.SECONDS));
            Assert.assertTrue(arvosanaCounter.tryAcquire(0, 10, TimeUnit.SECONDS));
            Assert.assertTrue(pisteCounter.tryAcquire(1, 10, TimeUnit.SECONDS));

            mockToReturnJson(GET, "/suoritusrekisteri/rest/v1/oppijat/" + hakijaOid, createOppijat().get(1));
            mockToReturnJson(GET, "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
                Collections.singletonList(kielikoeFi));
            mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/testihaku", new ParametritDTO());

            Response singleHakemusResponse = http.getWebClient().accept(MediaType.APPLICATION_JSON).get();
            assertEquals(200, singleHakemusResponse.getStatus());
            HenkiloValilehtiDTO tulokset = gson().fromJson(new InputStreamReader((InputStream) singleHakemusResponse.getEntity()), HenkiloValilehtiDTO.class);
            assertNull(tulokset.getLastmodified());
            assertThat(tulokset.getHakukohteittain(), Matchers.hasKey("testihakukohde"));
            assertThat(tulokset.getHakukohteittain().keySet(), Matchers.hasSize(1));
            HakemuksenKoetulosYhteenveto readPistetieto = tulokset.getHakukohteittain().get("testihakukohde");
            KokeenOsallistumistietoDTO osallistumistietoDTO = readPistetieto.osallistumistieto("testihakukohde", "kielikoe_fi");
            assertEquals(Osallistumistieto.OSALLISTUI, osallistumistietoDTO.osallistumistieto);

            assertEquals(applicationAdditionaData.getOid(), readPistetieto.applicationAdditionalDataDTO.getOid());
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    @Test
    public void testHakemuksenPistetietojenLukuOnnistuuJosOikeudetHakukohteeseen() throws IOException {
        ApplicationAdditionalDataDTO applicationAdditionalDataDto = luePistetiedot("List_ApplicationAdditionalDataDTO.json").get(0);
        String hakijaOid = applicationAdditionalDataDto.getPersonOid();

        HttpResourceBuilder.WebClientExposingHttpResource http = createClient(resourcesAddress + "/pistesyotto/koostetutPistetiedot/hakemus/" + applicationAdditionalDataDto.getOid());
        applicationAdditionalDataDto.getAdditionalData().remove("kielikoe_fi");

        String kayttajanOrganisaatioOid = "1.2.246.562.10.666";
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_" + kayttajanOrganisaatioOid);
        mockTarjontaOrganisaatioHakuCall(kayttajanOrganisaatioOid, "testihakukohde");

        mockOrganisaatioKutsu();
        mockTarjontaHakukohdeCall();
        mockValintakoe();
        mockTarjontaHakukohdeRyhmaCall();

        Hakemus hakemusHakuAppista = new Hakemus();
        hakemusHakuAppista.setAdditionalInfo(applicationAdditionalDataDto.getAdditionalData());
        hakemusHakuAppista.setPersonOid(hakijaOid);
        hakemusHakuAppista.setOid(applicationAdditionalDataDto.getOid());
        hakemusHakuAppista.setApplicationSystemId("testihaku");
        Answers answers = new Answers();
        answers.setHenkilotiedot(new HashMap<>());
        answers.getHenkilotiedot().put("Etunimet", "Frank");
        answers.getHenkilotiedot().put("Sukunimi", "Tester");
        answers.setHakutoiveet(new HashMap<>());
        answers.getHakutoiveet().put("preference1-Koulutus-id", "testihakukohde");
        hakemusHakuAppista.setAnswers(answers);
        mockToReturnJson(POST,
                "/lomake-editori/api/external/valintalaskenta",
                new ArrayList<AtaruHakemus>());
        mockToReturnJson(GET,
                "/haku-app/applications/" + applicationAdditionalDataDto.getOid(),
                hakemusHakuAppista
        );
        mockToReturnJson(POST, "/valintapiste-service/api/pisteet-with-hakemusoids", Collections.singletonList(APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET.apply(applicationAdditionalDataDto)));
        mockToReturnJson(GET, "/suoritusrekisteri/rest/v1/oppijat/" + hakijaOid, createOppijat().get(1));
        mockToReturnJson(GET, "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
                Collections.singletonList(kielikoeFi));
        mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/testihaku", new ParametritDTO());

        Response singleHakemusResponse = http.getWebClient().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(200, singleHakemusResponse.getStatus());
    }


    @Test
    public void testHakemuksenPistetietojenLukuEpaonnistuuJosEiOikeuksia() throws IOException {
        ApplicationAdditionalDataDTO applicationAdditionalDataDto = luePistetiedot("List_ApplicationAdditionalDataDTO.json").get(0);
        String hakijaOid = applicationAdditionalDataDto.getPersonOid();

        String url = resourcesAddress + "/pistesyotto/koostetutPistetiedot/hakemus/" + applicationAdditionalDataDto.getOid();
        HttpResourceBuilder.WebClientExposingHttpResource http = createClient(url);
        applicationAdditionalDataDto.getAdditionalData().remove("kielikoe_fi");

        String kayttajanOrganisaatioOid = "1.2.246.562.10.666";
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_" + kayttajanOrganisaatioOid);
        mockTarjontaOrganisaatioHakuCall(kayttajanOrganisaatioOid, "jokumuuhakukohde");

        mockOrganisaatioKutsu();
        mockTarjontaHakukohdeCall();
        mockValintakoe();
        mockTarjontaHakukohdeRyhmaCall();

        Hakemus hakemusHakuAppista = new Hakemus();
        hakemusHakuAppista.setAdditionalInfo(applicationAdditionalDataDto.getAdditionalData());
        hakemusHakuAppista.setPersonOid(hakijaOid);
        hakemusHakuAppista.setOid(applicationAdditionalDataDto.getOid());
        hakemusHakuAppista.setApplicationSystemId("testihaku");
        Answers answers = new Answers();
        answers.setHenkilotiedot(new HashMap<>());
        answers.getHenkilotiedot().put("Etunimet", "Frank");
        answers.getHenkilotiedot().put("Sukunimi", "Tester");
        answers.setHakutoiveet(new HashMap<>());
        answers.getHakutoiveet().put("preference1-Koulutus-id", "testihakukohde");
        hakemusHakuAppista.setAnswers(answers);
        mockToReturnJson(POST,
                "/lomake-editori/api/external/valintalaskenta",
                new ArrayList<AtaruHakemus>());
        mockToReturnJson(GET,
                "/haku-app/applications/" + applicationAdditionalDataDto.getOid(),
                hakemusHakuAppista
        );
        mockToReturnJson(POST, "/valintapiste-service/api/pisteet-with-hakemusoids", Collections.singletonList(APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET.apply(applicationAdditionalDataDto)));
        mockToReturnJson(GET, "/suoritusrekisteri/rest/v1/oppijat/" + hakijaOid, createOppijat().get(1));
        mockToReturnJson(GET, "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
                Collections.singletonList(kielikoeFi));
        mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/testihaku", new ParametritDTO());

        Response singleHakemusResponse = http.getWebClient().accept(MediaType.APPLICATION_JSON).get();
        assertEquals(403, singleHakemusResponse.getStatus());
    }

    private HttpResourceBuilder.WebClientExposingHttpResource createClient(String url) {
        return new HttpResourceBuilder()
                .address(url)
                .buildExposingWebClientDangerously();
    }

    private void mockValintakoe() {
        OsallistuminenTulosDTO osallistuminen = new OsallistuminenTulosDTO();
        osallistuminen.setOsallistuminen(Osallistuminen.OSALLISTUU);
        ValintakoeDTO koe = new ValintakoeDTO();
        koe.setValintakoeTunniste("ei_kielikoe");
        koe.setOsallistuminenTulos(osallistuminen);
        ValintakoeValinnanvaiheDTO vaihe = new ValintakoeValinnanvaiheDTO();
        vaihe.getValintakokeet().add(koe);
        HakutoiveDTO hakutoive = new HakutoiveDTO();
        hakutoive.getValinnanVaiheet().add(vaihe);
        hakutoive.setHakukohdeOid("testihakukohde");
        ValintakoeOsallistuminenDTO result = new ValintakoeOsallistuminenDTO();
        result.getHakutoiveet().add(hakutoive);
        result.setHakuOid("testihaku");
        result.setHakemusOid("1.2.246.562.11.00000060710");
        mockToReturnJson(GET, "/valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/valintakoe/hakemus/1.2.246.562.11.00000060710",
                result);
    }

    private void mockPistetietoNormiTallennus(Semaphore counter) {
        mockForward(PUT,
            fakeValintaPisteService.addHandler("/valintapiste-service/api/pisteet-with-hakemusoids", exchange -> {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write(gson().toJson(Collections.emptySet()).getBytes());
                exchange.getResponseBody().close();
                exchange.close();
                counter.release();
            }));
    }

    private void mockPistetietoTallennus(Semaphore counter, int n) {
        mockForward(PUT,
            fakeValintaPisteService.addHandler("/valintapiste-service/api/pisteet-with-hakemusoids", exchange -> {
                List<Valintapisteet> valintapisteetList = new Gson().fromJson(
                    IOUtils.toString(exchange.getRequestBody(), "UTF-8"), new TypeToken<List<Valintapisteet>>() {}.getType());
                assertEquals(n + " hakijalle löytyy pistetiedot", n, valintapisteetList.size());
                long count = valintapisteetList.stream().mapToLong(a -> a.getPisteet().size()).sum();

                assertEquals("Paljon pisteitä viedään", 1057 - n, count);
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write(gson().toJson(Collections.emptySet()).getBytes());
                exchange.getResponseBody().close();
                exchange.close();
                counter.release();
            }));
    }

    private void mockTarjontaHakukohdeCall() {
        HakukohdeV1RDTO hakukohdeDTO = new HakukohdeV1RDTO();
        hakukohdeDTO.setHakuOid("testihaku");
        hakukohdeDTO.setOid("testihakukohde");
        hakukohdeDTO.setTarjoajaOids(ImmutableSet.of("1.2.3.44444.5"));
        mockToReturnJson(GET,
                "/tarjonta-service/rest/v1/hakukohde/testihakukohde",
                new Result<>(hakukohdeDTO));
    }

    private void mockTarjontaHakukohdeRyhmaCall() {
        String s = "{\"result\": {\"tulokset\": [{\"tulokset\": [{\"oid\": \"1.2.246.562.20.50849071738\",\"ryhmaliitokset\": [{\"ryhmaOid\": \"1.2.246.562.28.77463971187\"},{\"ryhmaOid\": \"1.2.246.562.28.10942030083\"},{\"ryhmaOid\": \"1.2.246.562.28.92529355477\"}]},{\"oid\": \"1.2.246.562.20.52702700353\",\"ryhmaliitokset\": []}]}],\"tuloksia\": 1}}";
        mockToReturnString(GET,
                "/tarjonta-service/rest/v1/hakukohde/search",
                s);
    }

    private void mockOrganisaatioKutsu() {

        OrganisaatioTyyppiHierarkia hierarkia = new OrganisaatioTyyppiHierarkia(1, singletonList(
            new OrganisaatioTyyppi(
                "1.2.246.562.10.45042175963",
                ImmutableMap.of("fi", "Itä-Savon koulutuskuntayhtymä"),
                singletonList(
                    new OrganisaatioTyyppi(
                        "1.2.246.562.10.45698499378",
                        ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto"),
                        singletonList(
                            new OrganisaatioTyyppi(
                                "1.2.3.44444.5",
                                ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto, SAMI, kulttuuriala"),
                                emptyList(),
                                null,
                                singletonList("TOIMIPISTE")
                            )
                        ),
                        "oppilaitostyyppi_21#1",
                        singletonList("OPPILAITOS")
                    )
                ),
                null,
                singletonList("KOULUTUSTOIMIJA")
            )
        ));
        mockToReturnJsonWithParams(GET,
                "/organisaatio-service/rest/organisaatio/v2/hierarkia/hae/tyyppi.*",
                hierarkia,
                ImmutableMap.of("oid", "1.2.3.44444.5", "aktiiviset", "true", "suunnitellut", "false", "lakkautetut", "true"));
    }

    private void mockHakuAppKutsu(List<ApplicationAdditionalDataDTO> applicationAdditionalDataDtos) {
        mockToReturnJson(GET,
                "/haku-app/applications/additionalData/testihaku/testihakukohde",
                applicationAdditionalDataDtos
        );
    }

    private void mockValintaPisteServiceKutsu(List<ApplicationAdditionalDataDTO> applicationAdditionalDataDtos) {
        mockToReturnJson(GET,
                "/valintapiste-service/api/haku/testihaku/hakukohde/testihakukohde",
                applicationAdditionalDataDtos.stream().map(APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET).collect(Collectors.toList())
        );
    }

    private void mockSureKutsu(List<Oppija> oppijat) {
        mockToReturnJsonWithParams(GET,
                "/suoritusrekisteri/rest/v1/oppijat",
                oppijat,
                ImmutableMap.of("haku", "testihaku", "hakukohde", "testihakukohde")
        );
    }

    private List<ApplicationAdditionalDataDTO> readAdditionalData() throws Exception {
        List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("List_ApplicationAdditionalDataDTO.json");
        pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_fi"));
        pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_fi-OSALLISTUMINEN"));
        pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_sv"));
        pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_sv-OSALLISTUMINEN"));
        return pistetiedot;
    }

    private List<Oppija> createOppijat() {
        return Arrays.asList(
               createOppija("1.2.246.562.24.77642460905", singletonList(
                   createSuoritus("suoritus1", "1.2.246.562.24.77642460905", "FI", singletonList(
                       createArvosana("FI", hyvaksytty))))),
               createOppija("1.2.246.562.24.52321744679", Arrays.asList(
                       createSuoritus("suoritus2", "1.2.246.562.24.52321744679", "FI", singletonList(
                           createArvosana("FI", hylatty))),
                       createSuoritus("suoritus3", "1.2.246.562.24.52321744679", "SV", singletonList(
                           createArvosana("SV", hyvaksytty))))),
                createOppija("1.2.246.562.24.93793496064", Arrays.asList(
                        createSuoritus("suoritus4", "1.2.246.562.24.93793496064", "FI", singletonList(
                            createArvosana("FI", hylatty))),
                        createSuoritus("suoritus5", "1.2.246.562.24.93793496064", "FI", singletonList(
                            createArvosana("FI", hyvaksytty)), "1.2.246.562.10.45698499379")))
        );
    }

    private SuoritusJaArvosanat createSuoritus(String id, String oppijanumero, String kieli, List<Arvosana> arvosanat) {
        return createSuoritus(id, oppijanumero, kieli, arvosanat, "1.2.246.562.10.45698499378");
    }

    private SuoritusJaArvosanat createSuoritus(String id, String oppijanumero, String kieli, List<Arvosana> arvosanat, String myontaja) {
        String valmistuminen = new SimpleDateFormat(SuoritusJaArvosanatWrapper.SUORITUS_PVM_FORMAT).format(new Date());
        Suoritus suoritus = new Suoritus();
        suoritus.setId(id);
        suoritus.setHenkiloOid(oppijanumero);
        suoritus.setTila(AbstractPistesyottoKoosteService.KIELIKOE_SUORITUS_TILA);
        suoritus.setYksilollistaminen(AbstractPistesyottoKoosteService.KIELIKOE_SUORITUS_YKSILOLLISTAMINEN);
        suoritus.setVahvistettu(true);
        suoritus.setSuoritusKieli(kieli.toUpperCase());
        suoritus.setMyontaja(myontaja);
        suoritus.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        suoritus.setValmistuminen(valmistuminen);

        SuoritusJaArvosanat suoritusJaArvosanat = new SuoritusJaArvosanat();
        suoritusJaArvosanat.setSuoritus(suoritus);
        suoritusJaArvosanat.setArvosanat(arvosanat);

        return suoritusJaArvosanat;
    }

    private Arvosana createArvosana(String kieli, AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana arvio) {
        Arvosana arvosana = new Arvosana();
        arvosana.setAine(AbstractPistesyottoKoosteService.KIELIKOE_ARVOSANA_AINE);
        arvosana.setLisatieto(kieli.toUpperCase());
        arvosana.setArvio(new Arvio(arvio.name(), "HYVAKSYTTY", null));
        return arvosana;
    }

    private Oppija createOppija(String oppijanumero, List<SuoritusJaArvosanat> suoritusJaArvosanat) {
        Oppija oppija = new Oppija();
        oppija.setOppijanumero(oppijanumero);
        oppija.setSuoritukset(suoritusJaArvosanat);
        return oppija;
    }

    private void mockSuoritusrekisteriDelete(final Semaphore counter) {
        MockServer fakeSure = new MockServer();
        HttpHandler handler = (exchange) -> {
            try {
                String path = exchange.getRequestURI().getRawPath();
                String suoritusId = path.substring(39);
                System.out.println(path);
                System.out.println(suoritusId);
                Suoritus suoritus = new Suoritus();
                suoritus.setId(suoritusId);
                exchange.sendResponseHeaders(200, 0);
                counter.release();
                OutputStream responseBody = exchange.getResponseBody();
                IOUtils.write(new Gson().toJson(suoritus), responseBody);
                responseBody.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }

        };
        mockForward(DELETE,
                fakeSure.addHandler("/suoritusrekisteri/rest/v1/suoritukset/suoritus2", handler )
                        .addHandler("/suoritusrekisteri/rest/v1/suoritukset/suoritus1", handler )
                        .addHandler("/suoritusrekisteri/rest/v1/suoritukset/suoritus4", handler ));
    }

    private void mockTarjontaOrganisaatioHakuCall(String kayttajanOrganisaatioOid, String... hakukohdeOidsToReturn) {
        ResultSearch tarjontaHakukohdeSearchResult = new ResultSearch(new ResultTulos(Collections.singletonList(
                new ResultOrganization(kayttajanOrganisaatioOid, Stream.of(hakukohdeOidsToReturn).map(ResultHakukohde::new).collect(Collectors.toList())))));
        mockToReturnJsonWithParams(GET, "/tarjonta-service/rest/v1/hakukohde/search", tarjontaHakukohdeSearchResult,
                ImmutableMap.of("organisationOid", kayttajanOrganisaatioOid));
    }
}
