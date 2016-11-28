package fi.vm.sade.valinta.kooste.pistesyotto.excel;

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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
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
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author Jussi Jartamo
 */
public class PistesyottoE2ETest extends PistesyotonTuontiTestBase {

    @Before
    public void startServer() throws Throwable{
        startShared();
    }
    @Test
    public void tuonnissaEiYlikirjoitetaEditoimattomiaKenttiaHakemuspalveluun() throws Throwable {
        mockToReturnString(GET, "/valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/valintakoe/hakutoive/1.2.246.562.5.85532589612",
                IOUtils.toString(new ClassPathResource("pistesyotto/List_ValintakoeOsallistuminenDTO.json").getInputStream())
        );
        mockToReturnString(GET,
                "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/1.2.246.562.5.85532589612/",
                IOUtils.toString(new ClassPathResource("pistesyotto/List_ValintaperusteDTO.json").getInputStream())
        );

        List<ApplicationAdditionalDataDTO> pistetiedot = luePistetiedot("List_ApplicationAdditionalDataDTO.json");

        pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_fi"));

        mockToReturnJsonWithParams(GET,
                "/suoritusrekisteri/rest/v1/oppijat",
                Arrays.asList(createOppija()),
                ImmutableMap.of("haku", "testioidi1", "hakukohde", "1.2.246.562.5.85532589612")
        );

        mockToReturnJsonWithParams(GET,
                "/haku-app/applications/listfull",
                Collections.emptyList(),
                ImmutableMap.of("asId", "testioidi1", "aoOid", "1.2.246.562.5.85532589612"));

        mockToReturnJson(POST,
                "/haku-app/applications/list",
                Collections.emptyList());

        mockToReturnString(GET,
                "/haku-app/applications/additionalData/testioidi1/1.2.246.562.5.85532589612",
                new Gson().toJson(pistetiedot)
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

        HttpResource http = new HttpResource(resourcesAddress + "/pistesyotto/tuonti");

        final Semaphore suoritusCounter = new Semaphore(0);
        final Semaphore arvosanaCounter = new Semaphore(0);
        mockSuoritusrekisteri(suoritusCounter, arvosanaCounter);

        MockServer fakeHakuApp = new MockServer();
        final Semaphore counter = new Semaphore(0);
        mockForward(PUT,
                fakeHakuApp.addHandler("/haku-app/applications/additionalData/testioidi1/1.2.246.562.5.85532589612", exchange -> {
                    try {
                        List<ApplicationAdditionalDataDTO> additionalData = new Gson().fromJson(
                                IOUtils.toString(exchange.getRequestBody()), new TypeToken<List<ApplicationAdditionalDataDTO>>() {
                                }.getType()
                        );
                        Assert.assertEquals("210 hakijalle löytyy lisätiedot", 210, additionalData.size());
                        long count = additionalData.stream()
                                .flatMap(a -> a.getAdditionalData().entrySet().stream())
                                .count();

                        Assert.assertEquals("Editoimattomat lisätietokentät ja kielikoetulokset ohitetaan, eli viedään vain 630/1260.", 630, count);
                        exchange.sendResponseHeaders(200, 0);
                        counter.release();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }));
        Response r = http.getWebClient()
                .query("hakuOid", "testioidi1")
                .query("hakukohdeOid", "1.2.246.562.5.85532589612")
                .header("Content-Type", "application/octet-stream")
                .accept(MediaType.APPLICATION_JSON)
                .post(new ClassPathResource("pistesyotto/pistesyotto.xlsx").getInputStream());
        Assert.assertEquals(200, r.getStatus());
        try {
            Assert.assertTrue(suoritusCounter.tryAcquire(5, 25, TimeUnit.SECONDS));
            Assert.assertTrue(arvosanaCounter.tryAcquire(5, 25, TimeUnit.SECONDS));
            Assert.assertTrue(counter.tryAcquire(1, 25, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    private void mockTarjontaHakuCall() {
        HakuV1RDTO haku = new HakuV1RDTO();
        haku.setOid("testioidi1");
        haku.setHakukohdeOids(singletonList("1.2.246.562.5.85532589612"));
        mockToReturnJson(GET,
                "/tarjonta-service/rest/v1/haku/testioidi1/",
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
                "/tarjonta-service/rest/v1/hakukohde/1.2.246.562.5.85532589612/",
                new Result(hakukohdeDTO));
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
