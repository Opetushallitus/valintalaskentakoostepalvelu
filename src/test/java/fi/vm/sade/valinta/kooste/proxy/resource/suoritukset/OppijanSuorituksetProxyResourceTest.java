package fi.vm.sade.valinta.kooste.proxy.resource.suoritukset;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.DateDeserializer;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockSuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockTarjontaAsyncService;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import rx.Observable;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OppijanSuorituksetProxyResourceTest {
    private static final Logger LOG = LoggerFactory.getLogger(OppijanSuorituksetProxyResourceTest.class);
    private static final String URL = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    private static final String opiskelijaOid = "1.2.246.562.24.71943835646";
    private static final String hakemusOid = "1.2.246.562.11.00000000615";
    private static final String hakuOid = "1.2.246.562.29.90697286251";
    final HttpResource proxyResource = new HttpResource(URL + "/proxy/suoritukset/suorituksetByOpiskelijaOid/hakuOid/" + hakuOid + "/opiskeljaOid/" + opiskelijaOid + "/hakemusOid/" + hakemusOid);

    private static String classpathResourceAsString(String path) throws Exception {
        return IOUtils.toString(new ClassPathResource(path).getInputStream());
    }

    final Gson GSON = DateDeserializer.GSON;

    @BeforeClass
    public static void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void peruskoulunSuoritusProxyResourceTest() throws Exception {
        Mocks.reset();
        try {
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
        } finally {
            Mocks.reset();
        }
    }

    @Test
    public void lukionSuoritusProxyResourceTest() throws Exception {
        Mocks.reset();
        try {
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
        } finally {
            Mocks.reset();
        }
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


        MockTarjontaAsyncService.setMockHaku(expectedHaku);
        MockApplicationAsyncResource.setResultByOid(Arrays.asList(expectedHakemus));
        MockSuoritusrekisteriAsyncResource.setResult(expectedOppijanSuoritukset);
    }
}
