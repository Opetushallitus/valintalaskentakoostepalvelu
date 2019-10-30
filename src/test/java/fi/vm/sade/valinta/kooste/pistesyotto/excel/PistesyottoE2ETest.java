package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.gson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonWithParams;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnString;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static java.util.Collections.singletonList;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.MockOpintopolkuCasAuthenticationFilter;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
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
import fi.vm.sade.valinta.kooste.pistesyotto.service.AbstractPistesyottoKoosteService;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PistesyottoE2ETest extends PistesyotonTuontiTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoE2ETest.class);

    private List<Valintapisteet> pisteetFromValintaPisteService;
    private final MockServer fakeValintaPisteService = new MockServer();

    @Before
    public void init() throws Throwable {
        Type valintapisteetListType = new TypeToken<List<ValintakoeOsallistuminenDTO>>() {}.getType();
        String valintakoeOstallistuminenDtosJson = IOUtils.toString(new ClassPathResource("pistesyotto/List_ValintakoeOsallistuminenDTO.json").getInputStream(), "UTF-8");
        pisteetFromValintaPisteService = gson().<List<ValintakoeOsallistuminenDTO>>fromJson(valintakoeOstallistuminenDtosJson, valintapisteetListType).stream()
            .map(o -> new Valintapisteet(o.getHakemusOid(), o.getHakijaOid(), o.getEtunimi(), o.getSukunimi(), Collections.emptyList()))
            .collect(Collectors.toList());
        startShared();
        setUpMockCalls();
    }

    private void setUpMockCalls() throws IOException {
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_UPDATE_1.2.246.562.10.00000000001");

        mockToReturnString(GET, "/valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/valintakoe/hakutoive/1.2.246.562.5.85532589612",
                IOUtils.toString(new ClassPathResource("pistesyotto/List_ValintakoeOsallistuminenDTO.json").getInputStream())
        );
        mockToReturnString(GET,
                "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/1.2.246.562.5.85532589612",
                IOUtils.toString(new ClassPathResource("pistesyotto/List_ValintaperusteDTO.json").getInputStream())
        );

        List<ApplicationAdditionalDataDTO> applicationAdditionalDataDtos = luePistetiedot("List_ApplicationAdditionalDataDTO.json");

        applicationAdditionalDataDtos.forEach(p -> p.getAdditionalData().remove("kielikoe_fi"));

        mockToReturnJsonWithParams(GET,
                "/suoritusrekisteri/rest/v1/oppijat",
                Arrays.asList(createOppija()),
                ImmutableMap.of("haku", "testioidi1", "hakukohde", "1.2.246.562.5.85532589612")
        );

        mockToReturnJsonWithParams(GET,
                "/haku-app/applications/listfull",
                convertToHakemusSkeletons(applicationAdditionalDataDtos),
                ImmutableMap.of("asId", "testioidi1", "aoOid", "1.2.246.562.5.85532589612"));

        mockToReturnJson(POST,
                "/haku-app/applications/list",
                Collections.emptyList());

        mockToReturnString(GET,
                "/haku-app/applications/additionalData/testioidi1/1.2.246.562.5.85532589612",
                new Gson().toJson(applicationAdditionalDataDtos)
        );
        mockToReturnJson(POST,
                "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/valintakoe",
                Collections.emptyList()
        );
        mockToReturnJson(GET,
                "/ohjausparametrit-service/api/v1/rest/parametri/testioidi1",
                new ParametritDTO());

        mockTarjontaHakukohdeCall();
        mockTarjontaHakuCall();
        mockOrganisaatioKutsu();

        mockToReturnJson(PUT,
            "/dokumenttipalvelu-service/resources/dokumentit/tallenna",
            "Success of dokumenttipalvelu-service PUT in " + getClass().getSimpleName());

        mockToReturnJson(PUT,
            "/valintapiste-service/api/haku/testioidi1/hakukohde/1.2.246.562.5.85532589612",
            Collections.emptySet());

        mockToReturnJson(GET,
            "/valintapiste-service/api/haku/testioidi1/hakukohde/1.2.246.562.5.85532589612",
            pisteetFromValintaPisteService);
    }

    @Test
    public void tuonnissaKirjoitetaanSuoritusrekisteriinjaValintaPisteServiceen() throws Throwable {
        final Semaphore suoritusCounter = new Semaphore(0);
        final Semaphore arvosanaCounter = new Semaphore(0);
        mockSuoritusrekisteri(suoritusCounter, arvosanaCounter);

        String url = resourcesAddress + "/pistesyotto/tuonti";
        HttpResourceBuilder.WebClientExposingHttpResource http = createHttpResource(url);
        final Semaphore counter = new Semaphore(0);

        mockForward(PUT,
            fakeValintaPisteService.addHandler("/valintapiste-service/api/pisteet-with-hakemusoids", exchange -> {
                try {
                    List<Valintapisteet> valintapisteList = new Gson().fromJson(
                        IOUtils.toString(exchange.getRequestBody(), "UTF-8"), new TypeToken<List<Valintapisteet>>() {
                        }.getType()
                    );
                    Assert.assertEquals("209 hakijalle löytyy pistetiedot", 209, valintapisteList.size());
                    long count = valintapisteList.stream()
                        .mapToLong(a -> valintapisteList.size())
                        .sum();

                    Assert.assertEquals("Pisteitä tallennetaan ilmeisesti paljon.", 43681, count);
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().write(gson().toJson(Collections.emptySet()).getBytes());
                    exchange.getResponseBody().flush();
                    exchange.close();
                } finally {
                    counter.release();
                }
            }));
        postExcelAndExpectOkResponseWithRetry(http, "1.2.246.562.5.85532589612");
        try {
            Assert.assertTrue(suoritusCounter.tryAcquire(5, 25, TimeUnit.SECONDS));
            Assert.assertTrue(arvosanaCounter.tryAcquire(5, 25, TimeUnit.SECONDS));
            Assert.assertTrue(counter.tryAcquire(1, 25, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    @Test
    public void tuontiOnnistuuKayttajalleJollaOnOikeudetHakukohteeseen() throws Throwable {
        final Semaphore suoritusCounter = new Semaphore(0);
        final Semaphore arvosanaCounter = new Semaphore(0);
        mockSuoritusrekisteri(suoritusCounter, arvosanaCounter);

        String kayttajanOrganisaatioOid = "1.2.246.562.10.666";
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_UPDATE_" + kayttajanOrganisaatioOid);

        String hakukohdeOidFromUiRequest = "1.2.246.562.5.85532589612";
        mockTarjontaOrganisaatioHakuCall(kayttajanOrganisaatioOid, hakukohdeOidFromUiRequest);

        HttpResourceBuilder.WebClientExposingHttpResource http = createHttpResource(resourcesAddress + "/pistesyotto/tuonti");
        final Semaphore counter = new Semaphore(0);
        mockForward(PUT,
            fakeValintaPisteService.addHandler("/valintapiste-service/api/pisteet-with-hakemusoids", exchange -> {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write(gson().toJson(Collections.emptySet()).getBytes());
                exchange.getResponseBody().flush();
                exchange.close();
                counter.release();
            }));
        postExcelAndExpectOkResponseWithRetry(http, hakukohdeOidFromUiRequest);
        try {
            Assert.assertTrue(suoritusCounter.tryAcquire(5, 25, TimeUnit.SECONDS));
            Assert.assertTrue(arvosanaCounter.tryAcquire(5, 25, TimeUnit.SECONDS));
            Assert.assertTrue(counter.tryAcquire(1, 25, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    @Test
    public void tuontiEpaOnnistuuJosKayttajallaEiOleOikeuksiaHakukohteeseen() throws Throwable {
        String kayttajanOrganisaatioOid = "1.2.246.562.10.666";
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_UPDATE_" + kayttajanOrganisaatioOid);

        String hakukohdeOidFromUiRequest = "1.2.246.562.5.85532589612";
        mockTarjontaOrganisaatioHakuCall(kayttajanOrganisaatioOid, hakukohdeOidFromUiRequest + ".666");

        HttpResourceBuilder.WebClientExposingHttpResource http = createHttpResource(resourcesAddress + "/pistesyotto/tuonti");
        Response r = http.getWebClient()
                .query("hakuOid", "testioidi1")
                .query("hakukohdeOid", hakukohdeOidFromUiRequest)
                .header("Content-Type", "application/octet-stream")
                .accept(MediaType.APPLICATION_JSON)
                .post(new ClassPathResource("pistesyotto/pistesyotto.xlsx").getInputStream());
        Assert.assertThat(IOUtils.toString((InputStream) r.getEntity()), CoreMatchers.containsString("ei ole oikeuksia käsitellä hakukohteen 1.2.246.562.5.85532589612 pistetietoja"));
        Assert.assertEquals(FORBIDDEN.getStatusCode(), r.getStatus());
    }

    /**
     * OY-280 : It would be great to get rid of this hack.
     * But sometimes the processing just gets stuck, probably because of some race condition in the complex
     * RxJava streams processing in AbstractPistesyottoKoosteService. Some streams just don't provide values
     * for the final zip call.
     */
    private void postExcelAndExpectOkResponseWithRetry(HttpResourceBuilder.WebClientExposingHttpResource http, String hakukohdeOid) throws IOException {
        final int expectedHttpResponseStatus = NO_CONTENT.getStatusCode();
        final int maxRetries = 5;
        int retryCount = 1;

        Response response;
        String responseBody;
        do {
            LOG.info(String.format("Invoking endpoint with attempt number %d/%d ...", retryCount, maxRetries));
            retryCount = retryCount + 1;
            response = http.getWebClient()
                .query("hakuOid", "testioidi1")
                .query("hakukohdeOid", hakukohdeOid)
                .header("Content-Type", "application/octet-stream")
                .accept(MediaType.APPLICATION_JSON)
                .post(IOUtils.toByteArray(new ClassPathResource("pistesyotto/pistesyotto.xlsx").getInputStream()));
            responseBody = response.readEntity(String.class);
            LOG.info(String.format("Got response '%s'", responseBody));
        } while ((response.getStatus() != expectedHttpResponseStatus) && retryCount < maxRetries);
        Assert.assertEquals("", responseBody);
        Assert.assertEquals(expectedHttpResponseStatus, response.getStatus());
    }

    private void mockTarjontaOrganisaatioHakuCall(String kayttajanOrganisaatioOid, String... hakukohdeOidsToReturn) {
        ResultSearch tarjontaHakukohdeSearchResult = new ResultSearch(new ResultTulos(Collections.singletonList(
            new ResultOrganization(kayttajanOrganisaatioOid, Stream.of(hakukohdeOidsToReturn).map(ResultHakukohde::new).collect(Collectors.toList())))));
        mockToReturnJsonWithParams(GET, "/tarjonta-service/rest/v1/hakukohde/search", tarjontaHakukohdeSearchResult,
            ImmutableMap.of("organisationOid", kayttajanOrganisaatioOid));
    }

    private void mockTarjontaHakuCall() {
        HakuV1RDTO haku = new HakuV1RDTO();
        haku.setOid("testioidi1");
        haku.setHakukohdeOids(singletonList("1.2.246.562.5.85532589612"));
        mockToReturnJson(GET,
                "/tarjonta-service/rest/v1/haku/testioidi1",
                new Result(haku));
    }

    private void mockOrganisaatioKutsu() {

        OrganisaatioTyyppiHierarkia hierarkia = new OrganisaatioTyyppiHierarkia(1, Arrays.asList(
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
        ));
        mockToReturnJsonWithParams(GET,
                "/organisaatio-service/rest/organisaatio/v2/hierarkia/hae/tyyppi.*",
                hierarkia,
                ImmutableMap.of("oid", "1.2.3.44444.5", "aktiiviset", "true", "suunnitellut", "false", "lakkautetut", "true"));
    }

    private Oppija createOppija() {
        String valmistuminen = new SimpleDateFormat(SuoritusJaArvosanatWrapper.SUORITUS_PVM_FORMAT).format(new Date());
        Oppija oppija = new Oppija();
        oppija.setOppijanumero("1.2.246.562.24.77642460905");
        Suoritus suoritus = new Suoritus();
        suoritus.setHenkiloOid("1.2.246.562.24.77642460905");
        suoritus.setTila(AbstractPistesyottoKoosteService.KIELIKOE_SUORITUS_TILA);
        suoritus.setYksilollistaminen(AbstractPistesyottoKoosteService.KIELIKOE_SUORITUS_YKSILOLLISTAMINEN);
        suoritus.setVahvistettu(true);
        suoritus.setSuoritusKieli("FI");
        suoritus.setMyontaja("1.2.3.4444.5");
        suoritus.setKomo(SuoritusJaArvosanatWrapper.AMMATILLISEN_KIELIKOE);
        suoritus.setValmistuminen(valmistuminen);
        Arvosana arvosana = new Arvosana();
        arvosana.setAine(AbstractPistesyottoKoosteService.KIELIKOE_ARVOSANA_AINE);
        arvosana.setLisatieto("FI");
        arvosana.setArvio(new Arvio("TRUE", null, null));
        SuoritusJaArvosanat suoritusJaArvosanat = new SuoritusJaArvosanat();
        suoritusJaArvosanat.setSuoritus(suoritus);
        suoritusJaArvosanat.setArvosanat(Arrays.asList(arvosana));
        oppija.setSuoritukset(Arrays.asList(suoritusJaArvosanat));
        return oppija;
    }

    private void mockTarjontaHakukohdeCall() {
        HakukohdeV1RDTO hakukohdeDTO = new HakukohdeV1RDTO();
        hakukohdeDTO.setHakuOid("testioidi1");
        hakukohdeDTO.setOid("1.2.246.562.5.85532589612");
        hakukohdeDTO.setTarjoajaOids(ImmutableSet.of("1.2.3.44444.5"));
        mockToReturnJson(GET,
                "/tarjonta-service/rest/v1/hakukohde/1.2.246.562.5.85532589612",
                new Result(hakukohdeDTO));
    }

    private HttpResourceBuilder.WebClientExposingHttpResource createHttpResource(String url) {
        return new HttpResourceBuilder(getClass().getName()).address(url).timeoutMillis(TimeUnit.SECONDS.toMillis(240L)).buildExposingWebClientDangerously();
    }

    public static class Result<T> {
        private T result;

        public Result(T result) {
            this.result = result;
        }

        public T getResult() {
            return result;
        }
    }
}
