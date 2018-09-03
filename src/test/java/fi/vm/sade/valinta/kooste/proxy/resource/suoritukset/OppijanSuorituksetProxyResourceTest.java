package fi.vm.sade.valinta.kooste.proxy.resource.suoritukset;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusHakija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockSuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTarjontaAsyncService;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import rx.Observable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class OppijanSuorituksetProxyResourceTest {
    private static final Logger LOG = LoggerFactory.getLogger(OppijanSuorituksetProxyResourceTest.class);
    private static final String URL = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    private static final String opiskelijaOid = "1.2.246.562.24.71943835646";
    private static final String hakemusOid = "1.2.246.562.11.00000000615";
    private static final String hakuOid = "1.2.246.562.29.90697286251";
    final HttpResourceBuilder.WebClientExposingHttpResource proxyResource = new HttpResourceBuilder()
            .address(URL + "/proxy/suoritukset/suorituksetByOpiskelijaOid/hakuOid/" + hakuOid + "/opiskeljaOid/" + opiskelijaOid + "/hakemusOid/" + hakemusOid)
            .buildExposingWebClientDangerously();



    private static String classpathResourceAsString(String path) throws Exception {
        return IOUtils.toString(new ClassPathResource(path).getInputStream());
    }

    final Gson GSON = DateDeserializer.GSON;

    @BeforeClass
    public static void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Before
    @After
    public void resetMocks() {
        Mocks.reset();
    }



    @Test
    public void peruskoulunSuoritusProxyResourceTest() throws Exception {

        initMocks("/proxy/suoritukset/peruskoulun-oppija.json", "/proxy/hakemus/peruskoulun-hakemus.json", "/proxy/tarjonta/tarjonta.json");

        Response response = proxyResource.getWebClient().get();
        assertEquals(200, response.getStatus());

        Map<String, String> oppijanSuoritukset = GSON.
                fromJson(getJsonFromResponse(response), new TypeToken<Map<String, String>>() {
                }.getType());
        assertEquals("9", oppijanSuoritukset.get("PK_GE"));
        assertEquals("10", oppijanSuoritukset.get("PK_AI"));
        assertEquals("10", oppijanSuoritukset.get("PK_A1"));
        assertEquals("FI", oppijanSuoritukset.get("PK_AI_OPPIAINE"));
        assertEquals("EN", oppijanSuoritukset.get("PK_A1_OPPIAINE"));
        assertEquals("EL", oppijanSuoritukset.get("PK_B1_OPPIAINE"));
        assertEquals("S", oppijanSuoritukset.get("PK_LI"));
        assertFalse(oppijanSuoritukset.containsKey("PK_LI_SUORITETTU"));
        assertEquals("1", oppijanSuoritukset.get("POHJAKOULUTUS"));
        assertEquals("2015", oppijanSuoritukset.get("PK_PAATTOTODISTUSVUOSI"));
        assertEquals("false", oppijanSuoritukset.get("YO_TILA"));

        MockSuoritusrekisteriAsyncResource.clear();

    }

    @Test
    public void lukionSuoritusProxyResourceTest() throws Exception {

        initMocks("/proxy/suoritukset/lukion-oppija.json", "/proxy/hakemus/lukion-hakemus.json", "/proxy/tarjonta/tarjonta.json");

        Response response = proxyResource.getWebClient().get();
        assertEquals(200, response.getStatus());

        Map<String, String> oppijanSuoritukset = GSON.
                fromJson(getJsonFromResponse(response), new TypeToken<Map<String, String>>() {
                }.getType());
        assertEquals("8", oppijanSuoritukset.get("LK_MA"));
        assertEquals("8", oppijanSuoritukset.get("LK_PS"));
        assertEquals("S", oppijanSuoritukset.get("LK_LI"));
        assertEquals("EN", oppijanSuoritukset.get("LK_A1_OPPIAINE"));
        assertEquals("FI", oppijanSuoritukset.get("LK_AI_OPPIAINE"));
        assertEquals("ZH", oppijanSuoritukset.get("LK_B1_OPPIAINE"));
        assertEquals("9", oppijanSuoritukset.get("POHJAKOULUTUS"));
        assertEquals(null, oppijanSuoritukset.get("PK_PAATTOTODISTUSVUOSI"));
        assertEquals("false", oppijanSuoritukset.get("YO_TILA"));
        assertEquals("true", oppijanSuoritukset.get("LK_TILA"));

        MockSuoritusrekisteriAsyncResource.clear();

    }

    @Test
    public void lukionSuoritusBatchProxyResourceTest() throws Exception {
        initMocksList(
                "/proxy/suoritukset/lukion-oppijoita.json",
                "/proxy/hakemus/lukion-hakemuksia.json",
                "/proxy/tarjonta/tarjonta.json");

        List<HakemusHakija> allHakemus = new ArrayList<>();

        String[] personoids = new String[]{
                "1.2.246.562.24.17552525783",
                "1.2.246.562.24.17552525784",
                "1.2.246.562.24.17552525785"};

        String[] applicationoids = new String[]{
                "1.2.246.562.11.00000000576",
                "1.2.246.562.11.00000000577",
                "1.2.246.562.11.00000000578"};

        for(int i = 0; i < personoids.length; i++) {
            String personoid = personoids[i];
            String applicationOid = applicationoids[i];

            HakemusHakija hakemusHakija = new HakemusHakija();
            hakemusHakija.setOpiskelijaOid(personoid);

            Hakemus hakemus = new Hakemus();
            hakemus.setOid(applicationOid);

            Answers ans = new Answers();
            Map<String, String> koulutustausta = new HashMap<>();
            koulutustausta.put("POHJAKOULUTUS", "5");
            ans.setKoulutustausta(koulutustausta);

            hakemus.setAnswers(ans);

            hakemus.setApplicationSystemId("1.2.246.562.29.90697286251");
            hakemus.setPersonOid(hakemusHakija.getOpiskelijaOid());
            hakemusHakija.setHakemus(hakemus);
            allHakemus.add(hakemusHakija);
        }


        final HttpResourceBuilder.WebClientExposingHttpResource proxyBatchResource = new HttpResourceBuilder()
                .address(URL + "/proxy/suoritukset/suorituksetByOpiskelijaOid/hakuOid/" + hakuOid)
                .timeoutMillis(1000*100)
                .buildExposingWebClientDangerously();

        Response response = proxyBatchResource.getWebClient().type(MediaType.APPLICATION_JSON_TYPE).post(allHakemus);
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        String json = getJsonFromResponse(response);

        Map<String, Map<String, String>> oppijanSuoritukset = GSON.
                fromJson(json, new TypeToken<Map<String, Map<String, String>>>() {
                }.getType());
        oppijanSuoritukset.entrySet().forEach(entry -> LOG.info(entry.toString()));
        assertTrue(!oppijanSuoritukset.isEmpty());

        List<String> poids = Arrays.asList(personoids);
        assertTrue(oppijanSuoritukset.keySet().stream().allMatch(poids::contains));

        assertTrue("At least some of the response answers should contain 9 as POHJAKOULUTUS",
                oppijanSuoritukset.values().stream().anyMatch(stringStringMap -> "9".equals(stringStringMap.get("POHJAKOULUTUS"))));

        MockSuoritusrekisteriAsyncResource.clear();

    }

    private String getJsonFromResponse(Response response) throws IOException {
        String s = IOUtils.toString((InputStream) response.getEntity());
        LOG.trace("{}", s);
        return s;
    }


    public void initMocks(String oppilajanSuorituksetFile, String oppijanHakemusFile, String tarjontaFile) throws Exception {
        Mocks.reset();

        Oppija expectedOppijanSuoritukset = GSON.
                fromJson(classpathResourceAsString(oppilajanSuorituksetFile), new TypeToken<Oppija>() {
                }.getType());
        Hakemus expectedHakemus = GSON
                .fromJson(classpathResourceAsString(oppijanHakemusFile), new TypeToken<Hakemus>() {
                }.getType());

        HakuV1RDTO expectedHaku = GSON
                .fromJson(classpathResourceAsString(tarjontaFile), new TypeToken<HakuV1RDTO>() {
                }.getType());

        HakemusWrapper expectedWrapper = new HakuappHakemusWrapper(expectedHakemus);

        Valintapisteet v = new Valintapisteet(expectedWrapper.getOid(), expectedWrapper.getPersonOid(),"","",Collections.emptyList());

        Mockito.when(
                Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.any(), Mockito.any())).thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(),
                singletonList(v))));


        MockTarjontaAsyncService.setMockHaku(expectedHaku);
        MockApplicationAsyncResource.setResultByOid(Collections.singletonList(expectedWrapper));
        MockSuoritusrekisteriAsyncResource.setResult(expectedOppijanSuoritukset);
    }

    public void initMocksList(String oppilajanSuorituksetFile, String oppijanHakemusFile, String tarjontaFile) throws Exception {
        Mocks.reset();
        List<Oppija> expectedOppijanSuoritukset = GSON.
                fromJson(classpathResourceAsString(oppilajanSuorituksetFile), new TypeToken<List<Oppija>>() {
                }.getType());
        List<Hakemus> expectedHakemukset = GSON
                .fromJson(classpathResourceAsString(oppijanHakemusFile), new TypeToken<List<Hakemus>>() {
                }.getType());

        List<HakemusWrapper> expectedWrappers = expectedHakemukset.stream().map(HakuappHakemusWrapper::new).collect(Collectors.toList());

        HakuV1RDTO expectedHaku = GSON
                .fromJson(classpathResourceAsString(tarjontaFile), new TypeToken<HakuV1RDTO>() {
                }.getType());
        List<Valintapisteet> v = expectedWrappers.stream().map(h -> new Valintapisteet(h.getOid(), h.getPersonOid(),"","",Collections.emptyList())).collect(Collectors.toList());

        Mockito.when(
                Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.any(), Mockito.any())).thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(),
                v)));

        MockTarjontaAsyncService.setMockHaku(expectedHaku);
        MockApplicationAsyncResource.setResultByOid(expectedWrappers);
        MockSuoritusrekisteriAsyncResource.setResults(expectedOppijanSuoritukset);
    }


}
