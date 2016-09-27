package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.*;
import fi.vm.sade.valinta.kooste.server.MockServer;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonWithParams;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class PistesyottoKoosteE2ETest extends PistesyotonTuontiTestBase {

    @Before
    public void startServer() throws Throwable{
        startShared();
    }

    @Test
    public void testKoostaaPistetiedotHakemuksille() throws Exception {

        HttpResource http = new HttpResource(resourcesAddress + "/pistesyotto/koostaPistetiedotHakemuksille/haku/testihaku/hakukohde/testihakukohde");

        List<ApplicationAdditionalDataDTO> pistetiedot = readAdditionalData();
        List<String> hakemusOids = pistetiedot.stream().map(p -> p.getOid()).collect(Collectors.toList());

        assertFalse(pistetiedot.stream().anyMatch(p -> p.getAdditionalData().containsKey("kielikoe_fi")));

        mockHakuAppKutsu(pistetiedot);
        mockSureKutsu(createOppijat());

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
    public void testTallentaaKoostetutPistetiedot() throws Exception {
        HttpResource http = new HttpResource(resourcesAddress + "/pistesyotto/tallennaKoostetutPistetiedot/haku/testihaku/hakukohde/testihakukohde");
        List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("List_ApplicationAdditionalDataDTO.json");

        mockTarjontaHakukohdeCall();

        int totalCount = pistetiedot.stream().mapToInt(p -> p.getAdditionalData().size()).sum();
        int kielikoeCount = pistetiedot.stream().mapToInt(p -> p.getAdditionalData().keySet().stream().filter(k -> "kielikoe_fi".equals(k)).collect(Collectors.toList()).size()).sum();
        System.out.println(totalCount);
        System.out.println(kielikoeCount);
        System.out.println(totalCount - kielikoeCount);

        final Semaphore suoritusCounter = new Semaphore(0);
        final Semaphore arvosanaCounter = new Semaphore(0);
        mockSuoritusrekisteri(suoritusCounter, arvosanaCounter);

        final Semaphore lisatietoCounter = new Semaphore(0);
        mockHakuAppTallennus(lisatietoCounter);

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

    private void mockHakuAppTallennus(Semaphore counter) {
        MockServer fakeHakuApp = new MockServer();
        mockForward(PUT,
                fakeHakuApp.addHandler("/haku-app/applications/additionalData/testihaku/testihakukohde", exchange -> {
                    try {
                        List<ApplicationAdditionalDataDTO> additionalData = new Gson().fromJson(
                                IOUtils.toString(exchange.getRequestBody()), new TypeToken<List<ApplicationAdditionalDataDTO>>() {
                                }.getType()
                        );
                        Assert.assertEquals("209 hakijalle löytyy lisätiedot", 209, additionalData.size());
                        long count = additionalData.stream()
                                .flatMap(a -> a.getAdditionalData().entrySet().stream())
                                .count();

                        Assert.assertEquals("Editoimattomat lisätietokentät ja kielikoetulokset ohitetaan, eli viedään 1696-209=1487", 1487, count);
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
                new Result(hakukohdeDTO));
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
        pistetiedot.stream().forEach(p -> p.getAdditionalData().remove("kielikoe_fi"));
        return pistetiedot;
    }

    private List<Oppija> createOppijat() {
        return Arrays.asList(
               createOppija("1.2.246.562.24.77642460905", Arrays.asList(
                       createSuoritus("1.2.246.562.24.77642460905", "FI", Arrays.asList(
                               createArvosana("FI", "TRUE"))))),
               createOppija("1.2.246.562.24.52321744679", Arrays.asList(
                       createSuoritus("1.2.246.562.24.52321744679", "FI", Arrays.asList(
                               createArvosana("FI", "FALSE"))),
                       createSuoritus("1.2.246.562.24.52321744679", "SV", Arrays.asList(
                               createArvosana("SV", "TRUE"))))),
                createOppija("1.2.246.562.24.93793496064", Arrays.asList(
                        createSuoritus("1.2.246.562.24.93793496064", "FI", Arrays.asList(
                                createArvosana("FI", "FALSE"))),
                        createSuoritus("1.2.246.562.24.93793496064", "FI", Arrays.asList(
                                createArvosana("FI", "TRUE")))))
        );
    }

    private SuoritusJaArvosanat createSuoritus(String oppijanumero, String kieli, List<Arvosana> arvosanat) {
        String valmistuminen = new SimpleDateFormat("dd.MM.yyyy").format(new Date());
        Suoritus suoritus = new Suoritus();
        suoritus.setHenkiloOid(oppijanumero);
        suoritus.setTila("VALMIS");
        suoritus.setYksilollistaminen("Ei");
        suoritus.setVahvistettu(true);
        suoritus.setSuoritusKieli(kieli.toUpperCase());
        suoritus.setMyontaja("1.2.3.4444.5");
        suoritus.setKomo("ammatillisenKielikoe");
        suoritus.setValmistuminen(valmistuminen);

        SuoritusJaArvosanat suoritusJaArvosanat = new SuoritusJaArvosanat();
        suoritusJaArvosanat.setSuoritus(suoritus);
        suoritusJaArvosanat.setArvosanat(arvosanat);

        return suoritusJaArvosanat;
    }

    private Arvosana createArvosana(String kieli, String arvio) {
        Arvosana arvosana = new Arvosana();
        arvosana.setAine("KIELIKOE");
        arvosana.setLisatieto(kieli.toUpperCase());
        arvosana.setArvio(new Arvio(arvio.toUpperCase(), null, null));
        return arvosana;
    }

    private Oppija createOppija(String oppijanumero, List<SuoritusJaArvosanat> suoritusJaArvosanat) {
        Oppija oppija = new Oppija();
        oppija.setOppijanumero(oppijanumero);
        oppija.setSuoritukset(suoritusJaArvosanat);
        return oppija;
    }
}
