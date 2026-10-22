package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.gson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonWithParams;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnString;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.tarjonta.shared.types.TarjontaTila;
import fi.vm.sade.valinta.kooste.MockOpintopolkuCasAuthenticationFilter;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusOid;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultOrganization;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultTulos;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Piste;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemuksenKoetulosYhteenveto;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakukohteenOsallistumistiedotDTO.KokeenOsallistumistietoDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HenkiloValilehtiDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.Osallistumistieto;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.PistesyottoValilehtiDTO;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valinta.kooste.testapp.MockServicesApp;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.OsallistuminenTulosDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.valintakoe.Osallistuminen;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PistesyottoKoosteE2ETest extends PistesyotonTuontiTestBase {
  private static final ValintaperusteDTO kielikoeFi = new ValintaperusteDTO();

  static {
    kielikoeFi.setTunniste("kielikoe_fi");
    kielikoeFi.setOsallistuminenTunniste("kielikoe_fi-OSALLISTUMINEN");
    kielikoeFi.setVaatiiOsallistumisen(true);
  }

  private final MockServer fakeValintaPisteService = new MockServer();

  @BeforeEach
  public void startServer() {
    MockServicesApp.start();
    MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication(
        "ROLE_APP_HAKEMUS_READ_UPDATE_1.2.246.562.10.00000000001");
  }

  @Test
  public void testKoostaaPistetiedotHakemuksille() throws Exception {

    HttpResourceBuilder.WebClientExposingHttpResource http =
        createClient(
            MockServicesApp.resourcesAddress
                + "/pistesyotto/koostetutPistetiedot/haku/testihaku/hakukohde/testihakukohde");

    List<ApplicationAdditionalDataDTO> applicationAdditionalDatas = readAdditionalData();
    List<String> hakemusOids =
        applicationAdditionalDatas.stream()
            .map(ApplicationAdditionalDataDTO::getOid)
            .collect(Collectors.toList());

    assertFalse(
        applicationAdditionalDatas.stream()
            .anyMatch(p -> p.getAdditionalData().containsKey("kielikoe_fi")));

    mockHakuAppKutsu(applicationAdditionalDatas);
    Map<String, Piste> kielikoetulokset =
        Map.of(
            "1.2.246.562.24.77642460905", kielikoePiste("kielikoe_fi", "true"),
            "1.2.246.562.24.52321744679", kielikoePiste("kielikoe_sv", "true"),
            "1.2.246.562.24.93793496064", kielikoePiste("kielikoe_fi", "true"));
    mockToReturnJson(
        GET,
        "/valintalaskenta-laskenta-service/resources/valintapisteet/haku/testihaku/hakukohde/testihakukohde",
        applicationAdditionalDatas.stream()
            .map(APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET)
            .map(
                v ->
                    v.getOppijaOID() != null && kielikoetulokset.containsKey(v.getOppijaOID())
                        ? new Valintapisteet(
                            v.getHakemusOID(),
                            v.getOppijaOID(),
                            v.getEtunimet(),
                            v.getSukunimi(),
                            singletonList(kielikoetulokset.get(v.getOppijaOID())))
                        : v)
            .collect(Collectors.toList()));

    mockToReturnJson(
        GET,
        "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
        Collections.singletonList(kielikoeFi));
    mockToReturnJson(
        GET,
        "/valintalaskenta-laskenta-service/resources/valintakoe/hakutoive/testihakukohde",
        Collections.<ValintakoeOsallistuminenDTO>emptyList());

    Response r =
        http.getWebClient()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .get();
    assertEquals(200, r.getStatus());

    PistesyottoValilehtiDTO tulokset =
        gson()
            .fromJson(
                new InputStreamReader((InputStream) r.getEntity()), PistesyottoValilehtiDTO.class);
    BiFunction<String, String, String> readPistetieto =
        (personOid, key) ->
            tulokset.getValintapisteet().stream()
                .filter(p -> personOid.equals(p.applicationAdditionalDataDTO.getPersonOid()))
                .findFirst()
                .get()
                .applicationAdditionalDataDTO
                .getAdditionalData()
                .get(key);

    assertEquals("true", readPistetieto.apply("1.2.246.562.24.77642460905", "kielikoe_fi"));
    assertEquals("", readPistetieto.apply("1.2.246.562.24.52321744679", "kielikoe_fi"));
    assertEquals("true", readPistetieto.apply("1.2.246.562.24.52321744679", "kielikoe_sv"));
    assertEquals("true", readPistetieto.apply("1.2.246.562.24.93793496064", "kielikoe_fi"));

    Function<List<ApplicationAdditionalDataDTO>, Integer> countAdditionalData =
        (pistetietoList) ->
            pistetietoList.stream().mapToInt(p -> p.getAdditionalData().values().size()).sum();

    int keysAddedByKielikoeFi = hakemusOids.size() * 2;
    int keysAddedByKielikoeSvSuoritus = 2;
    assertEquals(
        (keysAddedByKielikoeFi + keysAddedByKielikoeSvSuoritus),
        countAdditionalData
            .apply(
                tulokset.getValintapisteet().stream()
                    .map(p -> p.applicationAdditionalDataDTO)
                    .collect(Collectors.toList()))
            .intValue());
  }

  @Test
  public void testTallentaaKoostetutPistetiedotHakukohteelle() throws Exception {
    HttpResourceBuilder.WebClientExposingHttpResource http =
        createClient(
            MockServicesApp.resourcesAddress
                + "/pistesyotto/koostetutPistetiedot/haku/testihaku/hakukohde/testihakukohde");
    List<ApplicationAdditionalDataDTO> applicationAdditionalDataDtos =
        luePistetiedot("List_ApplicationAdditionalDataDTO.json");

    mockToReturnJson(
        GET,
        "/valintalaskenta-laskenta-service/resources/valintapisteet/haku/testihaku/hakukohde/testihakukohde",
        applicationAdditionalDataDtos.stream()
            .map(APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET)
            .collect(Collectors.toList()));
    mockToReturnJson(
        POST, "/lomake-editori/api/external/valintalaskenta", new ArrayList<AtaruHakemus>());
    mockToReturnJson(
        POST,
        "/haku-app/applications/listfull",
        applicationAdditionalDataDtos.stream()
            .map(p -> new HakemusOid(p.getOid()))
            .collect(Collectors.toList()));
    mockTarjontaHakukohdeCall();

    final Semaphore pisteCounter = new Semaphore(0);
    mockPistetietoTallennus(pisteCounter, 209);

    Response r =
        http.getWebClient()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .put(new Gson().toJson(applicationAdditionalDataDtos));
    assertEquals(204, r.getStatus());

    try {
      Assertions.assertTrue(pisteCounter.tryAcquire(1, 10, TimeUnit.SECONDS));
    } catch (InterruptedException e) {
      Assertions.fail();
    }
  }

  @Test
  public void testTallentaaKoostetutPistetiedotJosEiKielikokeita() throws Exception {
    ApplicationAdditionalDataDTO applicationAdditionaData =
        luePistetiedot("List_ApplicationAdditionalDataDTO.json").get(0);
    String hakijaOid = applicationAdditionaData.getPersonOid();

    HttpResourceBuilder.WebClientExposingHttpResource http =
        createClient(
            MockServicesApp.resourcesAddress
                + "/pistesyotto/koostetutPistetiedot/hakemus/"
                + applicationAdditionaData.getOid());
    applicationAdditionaData.getAdditionalData().remove("kielikoe_fi");

    mockTarjontaHakukohdeCall();
    mockValintakoe();
    mockTarjontaHakukohdeRyhmaCall();
    mockKoutaHakukohdeSearchCall();

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
    mockToReturnJson(
        POST, "/lomake-editori/api/external/valintalaskenta", new ArrayList<AtaruHakemus>());
    mockToReturnJson(
        GET, "/haku-app/applications/" + applicationAdditionaData.getOid(), hakemusHakuAppista);
    mockToReturnJson(
        POST,
        "/valintalaskenta-laskenta-service/resources/valintapisteet/pisteet-with-hakemusoids",
        Collections.singletonList(
            APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET.apply(applicationAdditionaData)));
    final Semaphore pisteCounter = new Semaphore(0);
    mockPistetietoNormiTallennus(pisteCounter);

    Response r =
        http.getWebClient()
            .header("Content-Type", MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .put(new Gson().toJson(applicationAdditionaData));
    assertEquals(204, r.getStatus());

    try {
      Assertions.assertTrue(pisteCounter.tryAcquire(1, 10, TimeUnit.SECONDS));

      mockToReturnJson(
          GET,
          "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
          Collections.singletonList(kielikoeFi));

      Response singleHakemusResponse = http.getWebClient().accept(MediaType.APPLICATION_JSON).get();
      assertEquals(200, singleHakemusResponse.getStatus());
      HenkiloValilehtiDTO tulokset =
          gson()
              .fromJson(
                  new InputStreamReader((InputStream) singleHakemusResponse.getEntity()),
                  HenkiloValilehtiDTO.class);
      assertNull(tulokset.getLastmodified());
      assertThat(tulokset.getHakukohteittain(), Matchers.hasKey("testihakukohde"));
      assertThat(tulokset.getHakukohteittain().keySet(), Matchers.hasSize(1));
      HakemuksenKoetulosYhteenveto readPistetieto =
          tulokset.getHakukohteittain().get("testihakukohde");
      KokeenOsallistumistietoDTO osallistumistietoDTO =
          readPistetieto.osallistumistieto("testihakukohde", "kielikoe_fi");
      assertEquals(Osallistumistieto.OSALLISTUI, osallistumistietoDTO.osallistumistieto);

      assertEquals(
          applicationAdditionaData.getOid(), readPistetieto.applicationAdditionalDataDTO.getOid());
    } catch (InterruptedException e) {
      Assertions.fail();
    }
  }

  @Test
  public void testHakemuksenPistetietojenLukuOnnistuuJosOikeudetHakukohteeseen()
      throws IOException {
    ApplicationAdditionalDataDTO applicationAdditionalDataDto =
        luePistetiedot("List_ApplicationAdditionalDataDTO.json").get(0);
    String hakijaOid = applicationAdditionalDataDto.getPersonOid();

    HttpResourceBuilder.WebClientExposingHttpResource http =
        createClient(
            MockServicesApp.resourcesAddress
                + "/pistesyotto/koostetutPistetiedot/hakemus/"
                + applicationAdditionalDataDto.getOid());
    applicationAdditionalDataDto.getAdditionalData().remove("kielikoe_fi");

    String kayttajanOrganisaatioOid = "1.2.246.562.10.666";
    MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication(
        "ROLE_APP_HAKEMUS_READ_" + kayttajanOrganisaatioOid);
    mockTarjontaOrganisaatioHakuCall(kayttajanOrganisaatioOid, "testihakukohde");

    mockTarjontaHakukohdeCall();
    mockValintakoe();
    mockTarjontaHakukohdeRyhmaCall();
    mockKoutaHakukohdeSearchCall();

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
    mockToReturnJson(
        POST, "/lomake-editori/api/external/valintalaskenta", new ArrayList<AtaruHakemus>());
    mockToReturnJson(
        GET, "/haku-app/applications/" + applicationAdditionalDataDto.getOid(), hakemusHakuAppista);
    mockToReturnJson(
        POST,
        "/valintalaskenta-laskenta-service/resources/valintapisteet/pisteet-with-hakemusoids",
        Collections.singletonList(
            APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET.apply(applicationAdditionalDataDto)));
    mockToReturnJson(
        GET,
        "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
        Collections.singletonList(kielikoeFi));

    Response singleHakemusResponse = http.getWebClient().accept(MediaType.APPLICATION_JSON).get();
    assertEquals(200, singleHakemusResponse.getStatus());
  }

  @Test
  public void testHakemuksenPistetietojenLukuEpaonnistuuJosEiOikeuksia() throws IOException {
    ApplicationAdditionalDataDTO applicationAdditionalDataDto =
        luePistetiedot("List_ApplicationAdditionalDataDTO.json").get(0);
    String hakijaOid = applicationAdditionalDataDto.getPersonOid();

    String url =
        MockServicesApp.resourcesAddress
            + "/pistesyotto/koostetutPistetiedot/hakemus/"
            + applicationAdditionalDataDto.getOid();
    HttpResourceBuilder.WebClientExposingHttpResource http = createClient(url);
    applicationAdditionalDataDto.getAdditionalData().remove("kielikoe_fi");

    String kayttajanOrganisaatioOid = "1.2.246.562.10.666";
    MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication(
        "ROLE_APP_HAKEMUS_READ_" + kayttajanOrganisaatioOid);
    mockTarjontaOrganisaatioHakuCall(kayttajanOrganisaatioOid, "jokumuuhakukohde");

    mockTarjontaHakukohdeCall();
    mockValintakoe();
    mockTarjontaHakukohdeRyhmaCall();
    mockKoutaHakukohdeSearchCall();

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
    mockToReturnJson(
        POST, "/lomake-editori/api/external/valintalaskenta", new ArrayList<AtaruHakemus>());
    mockToReturnJson(
        GET, "/haku-app/applications/" + applicationAdditionalDataDto.getOid(), hakemusHakuAppista);
    mockToReturnJson(
        POST,
        "/valintalaskenta-laskenta-service/resources/valintapisteet/pisteet-with-hakemusoids",
        Collections.singletonList(
            APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET.apply(applicationAdditionalDataDto)));
    mockToReturnJson(
        GET,
        "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/testihakukohde",
        Collections.singletonList(kielikoeFi));

    Response singleHakemusResponse = http.getWebClient().accept(MediaType.APPLICATION_JSON).get();
    assertEquals(403, singleHakemusResponse.getStatus());
  }

  private HttpResourceBuilder.WebClientExposingHttpResource createClient(String url) {
    return new HttpResourceBuilder(getClass().getName())
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
    mockToReturnJson(
        GET,
        "/valintalaskenta-laskenta-service/resources/valintakoe/hakemus/1.2.246.562.11.00000060710",
        result);
  }

  private void mockPistetietoNormiTallennus(Semaphore counter) {
    mockForward(
        PUT,
        fakeValintaPisteService.addHandler(
            "/valintalaskenta-laskenta-service/resources/valintapisteet/pisteet-with-hakemusoids",
            exchange -> {
              exchange.getResponseHeaders().add("Content-Type", "application/json");
              exchange.sendResponseHeaders(200, 0);
              exchange.getResponseBody().write(gson().toJson(Collections.emptySet()).getBytes());
              exchange.getResponseBody().flush();
              exchange.close();
              counter.release();
            }));
  }

  private void mockPistetietoTallennus(Semaphore counter, int n) {
    mockForward(
        PUT,
        fakeValintaPisteService.addHandler(
            "/valintalaskenta-laskenta-service/resources/valintapisteet/pisteet-with-hakemusoids",
            exchange -> {
              List<Valintapisteet> valintapisteetList =
                  new Gson()
                      .fromJson(
                          IOUtils.toString(exchange.getRequestBody(), "UTF-8"),
                          new TypeToken<List<Valintapisteet>>() {}.getType());
              assertEquals(n, valintapisteetList.size(), n + " hakijalle löytyy pistetiedot");
              long count = valintapisteetList.stream().mapToLong(a -> a.getPisteet().size()).sum();

              assertEquals(1057 - n, count, "Paljon pisteitä viedään");
              long hyvaksytytKielikokeet =
                  valintapisteetList.stream()
                      .flatMap(a -> a.getPisteet().stream())
                      .filter(
                          piste ->
                              "kielikoe_fi".equals(piste.getTunniste())
                                  && "true".equals(piste.getArvo()))
                      .count();
              assertEquals(
                  2, hyvaksytytKielikokeet, "Kielikokeiden arvot tallennetaan valintapisteisiin");
              exchange.getResponseHeaders().add("Content-Type", "application/json");
              exchange.sendResponseHeaders(200, 0);
              exchange.getResponseBody().write(gson().toJson(Collections.emptySet()).getBytes());
              exchange.getResponseBody().flush();
              exchange.close();
              counter.release();
            }));
  }

  private void mockTarjontaHakukohdeCall() {
    HakukohdeV1RDTO hakukohdeDTO = new HakukohdeV1RDTO();
    hakukohdeDTO.setHakuOid("testihaku");
    hakukohdeDTO.setOid("testihakukohde");
    hakukohdeDTO.setTila(TarjontaTila.JULKAISTU);
    hakukohdeDTO.setTarjoajaOids(ImmutableSet.of("1.2.3.44444.5"));
    hakukohdeDTO.setHakukohdeKoulutusOids(Collections.emptyList());
    hakukohdeDTO.setPohjakoulutusvaatimus("");
    hakukohdeDTO.setValintakokeet(Collections.emptyList());
    mockToReturnJson(
        GET, "/tarjonta-service/rest/v1/hakukohde/testihakukohde", new Result<>(hakukohdeDTO));
  }

  private void mockTarjontaHakukohdeRyhmaCall() {
    String s =
        "{\"result\": {\"tulokset\": [{\"tulokset\": [{\"oid\": \"1.2.246.562.20.50849071738\",\"ryhmaliitokset\": [{\"ryhmaOid\": \"1.2.246.562.28.77463971187\"},{\"ryhmaOid\": \"1.2.246.562.28.10942030083\"},{\"ryhmaOid\": \"1.2.246.562.28.92529355477\"}]},{\"oid\": \"1.2.246.562.20.52702700353\",\"ryhmaliitokset\": []}]}],\"tuloksia\": 1}}";
    mockToReturnString(GET, "/tarjonta-service/rest/v1/hakukohde/search", s);
  }

  private void mockKoutaHakukohdeSearchCall() {
    mockToReturnString(GET, "/kouta-internal/hakukohde/search", "[]");
  }

  private void mockHakuAppKutsu(List<ApplicationAdditionalDataDTO> applicationAdditionalDataDtos) {
    mockToReturnJson(
        GET,
        "/haku-app/applications/additionalData/testihaku/testihakukohde",
        applicationAdditionalDataDtos);
  }

  private List<ApplicationAdditionalDataDTO> readAdditionalData() throws Exception {
    List<ApplicationAdditionalDataDTO> pistetiedot =
        luePistetiedot("List_ApplicationAdditionalDataDTO.json");
    pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_fi"));
    pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_fi-OSALLISTUMINEN"));
    pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_sv"));
    pistetiedot.forEach(p -> p.getAdditionalData().remove("kielikoe_sv-OSALLISTUMINEN"));
    return pistetiedot;
  }

  private static Piste kielikoePiste(String tunniste, String arvo) {
    return new Piste(
        tunniste,
        arvo,
        fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Osallistuminen.OSALLISTUI,
        "tallettaja");
  }

  private void mockTarjontaOrganisaatioHakuCall(
      String kayttajanOrganisaatioOid, String... hakukohdeOidsToReturn) {
    ResultSearch tarjontaHakukohdeSearchResult =
        new ResultSearch(
            new ResultTulos(
                Collections.singletonList(
                    new ResultOrganization(
                        kayttajanOrganisaatioOid,
                        Stream.of(hakukohdeOidsToReturn)
                            .map(ResultHakukohde::new)
                            .collect(Collectors.toList())))));
    mockToReturnJsonWithParams(
        GET,
        "/tarjonta-service/rest/v1/hakukohde/search",
        tarjontaHakukohdeSearchResult,
        ImmutableMap.of("organisationOid", kayttajanOrganisaatioOid));
  }
}
