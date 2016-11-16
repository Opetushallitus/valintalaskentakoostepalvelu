package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonWithParams;
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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpHandler;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PistesyottoKoosteE2ETest extends PistesyotonTuontiTestBase {

    @Before
    public void startServer() throws Throwable{
        startShared();
    }

    @Test
    public void testKoostaaPistetiedotHakemuksille() throws Exception {

        HttpResource http = new HttpResource(resourcesAddress + "/pistesyotto/koostaPistetiedotHakemuksille/haku/testihaku/hakukohde/testihakukohde");

        List<ApplicationAdditionalDataDTO> pistetiedot = readAdditionalData();
        List<String> hakemusOids = pistetiedot.stream().map(ApplicationAdditionalDataDTO::getOid).collect(Collectors.toList());

        assertFalse(pistetiedot.stream().anyMatch(p -> p.getAdditionalData().containsKey("kielikoe_fi")));

        mockHakuAppKutsu(pistetiedot);
        mockSureKutsu(createOppijat());
        mockToReturnJson(GET, "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde/",
                Collections.<ValintaperusteDTO>emptyList()); // TODO add correct avaimet
        mockToReturnJson(GET, "/valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/valintakoe/hakutoive/testihakukohde",
                Collections.<ValintakoeOsallistuminenDTO>emptyList()); // TODO add correct osallistuminen
        mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/testihaku", new ParametritDTO());

        Response r = http.getWebClient()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(new Gson().toJson(hakemusOids));
        assertEquals(200, r.getStatus());

        List<ApplicationAdditionalDataDTO> uudetPistetiedot = new Gson().fromJson(
                new InputStreamReader((InputStream)r.getEntity()),
                new TypeToken<List<ApplicationAdditionalDataDTO>>(){}.getType());

        BiFunction<String, String, String> readPistetieto = (personOid, key) ->
            uudetPistetiedot.stream().filter(p -> personOid.equals(p.getPersonOid())).findFirst().get().getAdditionalData().get(key);

        assertEquals("true", readPistetieto.apply("1.2.246.562.24.77642460905", "kielikoe_fi"));
        assertEquals("false", readPistetieto.apply("1.2.246.562.24.52321744679", "kielikoe_fi"));
        assertEquals("true", readPistetieto.apply("1.2.246.562.24.52321744679", "kielikoe_sv"));
        assertEquals("true", readPistetieto.apply("1.2.246.562.24.93793496064", "kielikoe_fi"));

        Function<List<ApplicationAdditionalDataDTO>, Integer> countAdditionalData = (pistetietoList) ->
            pistetietoList.stream().mapToInt(p -> p.getAdditionalData().values().size()).sum();

        assertEquals((countAdditionalData.apply(pistetiedot) + 4), (int)countAdditionalData.apply(uudetPistetiedot));
    }

    @Test
    public void testTallentaaKoostetutPistetiedotHakukohteelle() throws Exception {
        HttpResource http = new HttpResource(resourcesAddress + "/pistesyotto/tallennaKoostetutPistetiedot/haku/testihaku/hakukohde/testihakukohde");
        List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("List_ApplicationAdditionalDataDTO.json");

        mockOrganisaatioKutsu();
        mockTarjontaHakukohdeCall();
        mockSureKutsu(createOppijat());

        final Semaphore suoritusCounter = new Semaphore(0);
        final Semaphore arvosanaCounter = new Semaphore(0);
        final Semaphore deleteCounter = new Semaphore(0);
        mockSuoritusrekisteri(suoritusCounter, arvosanaCounter);
        mockSuoritusrekisteriDelete(deleteCounter);

        final Semaphore lisatietoCounter = new Semaphore(0);
        mockHakuAppTallennus(lisatietoCounter, 209);

        Response r = http.getWebClient()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .put(new Gson().toJson(pistetiedot));
        assertEquals(200, r.getStatus());

        try {
            Assert.assertTrue(suoritusCounter.tryAcquire(2, 10, TimeUnit.SECONDS));
            Assert.assertTrue(arvosanaCounter.tryAcquire(2, 10, TimeUnit.SECONDS));
            Assert.assertTrue(lisatietoCounter.tryAcquire(1, 10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    @Test
    public void testTallentaaKoostetutPistetiedotJosEiKielikokeita() throws Exception {
        HttpResource http = new HttpResource(resourcesAddress + "/pistesyotto/tallennaKoostetutPistetiedot");
        ApplicationAdditionalDataDTO pistetieto = luePistetiedot("List_ApplicationAdditionalDataDTO.json").get(0);
        pistetieto.getAdditionalData().remove("kielikoe_fi");

        mockOrganisaatioKutsu();
        mockTarjontaHakukohdeCall();
        mockSureKutsu(createOppijat());
        mockValintakoe();

        final Semaphore suoritusCounter = new Semaphore(0);
        final Semaphore arvosanaCounter = new Semaphore(0);
        final Semaphore deleteCounter = new Semaphore(0);
        mockSuoritusrekisteri(suoritusCounter, arvosanaCounter);
        mockSuoritusrekisteriDelete(deleteCounter);

        final Semaphore lisatietoCounter = new Semaphore(0);
        mockHakuAppNormiTallennus(lisatietoCounter);

        Response r = http.getWebClient()
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .put(new Gson().toJson(pistetieto));
        assertEquals(200, r.getStatus());

        try {
            Assert.assertTrue(suoritusCounter.tryAcquire(0, 10, TimeUnit.SECONDS));
            Assert.assertTrue(arvosanaCounter.tryAcquire(0, 10, TimeUnit.SECONDS));
            Assert.assertTrue(lisatietoCounter.tryAcquire(1, 10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
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

    private void mockHakuAppNormiTallennus(Semaphore counter) {
        MockServer fakeHakuApp = new MockServer();
        mockForward(PUT,
                fakeHakuApp.addHandler("/haku-app/applications/additionalData/testihaku", exchange -> {
                    try {
                        new Gson().fromJson(
                            IOUtils.toString(exchange.getRequestBody()), new TypeToken<List<ApplicationAdditionalDataDTO>>() {
                            }.getType()
                        );
                        exchange.sendResponseHeaders(200, 0);
                        exchange.getResponseBody().close();
                        counter.release();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }));
    }

    private void mockHakuAppTallennus(Semaphore counter, int n) {
        MockServer fakeHakuApp = new MockServer();
        mockForward(PUT,
                fakeHakuApp.addHandler("/haku-app/applications/additionalData/testihaku/testihakukohde", exchange -> {
                    try {
                        List<ApplicationAdditionalDataDTO> additionalData = new Gson().fromJson(
                                IOUtils.toString(exchange.getRequestBody()), new TypeToken<List<ApplicationAdditionalDataDTO>>() {
                                }.getType()
                        );
                        Assert.assertEquals(n + " hakijalle löytyy lisätiedot", n, additionalData.size());
                        long count = additionalData.stream()
                                .flatMap(a -> a.getAdditionalData().entrySet().stream())
                                .count();

                        Assert.assertEquals("Editoimattomat lisätietokentät ja kielikoetulokset ohitetaan, eli viedään 1696-" + n + "=1487", 1696 - n, count);
                        exchange.sendResponseHeaders(200, 0);
                        exchange.getResponseBody().close();
                        counter.release();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }));
    }

    private void mockTarjontaHakukohdeCall() {
        HakukohdeV1RDTO hakukohdeDTO = new HakukohdeV1RDTO();
        hakukohdeDTO.setHakuOid("testihaku");
        hakukohdeDTO.setOid("testihakukohde");
        hakukohdeDTO.setTarjoajaOids(ImmutableSet.of("1.2.3.44444.5"));
        mockToReturnJson(GET,
                "/tarjonta-service/rest/v1/hakukohde/testihakukohde/",
                new Result<>(hakukohdeDTO));
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

    private void mockHakuAppKutsu(List<ApplicationAdditionalDataDTO> pistetiedot) {
        mockToReturnJson(POST,
                "/haku-app/applications/additionalData",
                pistetiedot
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
}
