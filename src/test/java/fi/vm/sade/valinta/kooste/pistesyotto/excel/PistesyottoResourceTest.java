package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.Solu;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.dto.OrganisaatioTyyppiHierarkia;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Suoritus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.SuoritusJaArvosanatWrapper;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.mocks.*;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.ValintakoeDTO;
import fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec;
import fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec;
import fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec;
import fi.vm.sade.valinta.kooste.util.ExcelImportUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.spec.SuoritusrekisteriSpec;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import io.reactivex.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.lisatiedot;
import static fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.osallistuminen;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.hakukohdeJaValintakoe;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintaperuste;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hylatty;
import static fi.vm.sade.valinta.kooste.util.sure.AmmatillisenKielikoetuloksetSurestaConverter.SureHyvaksyttyArvosana.hyvaksytty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class PistesyottoResourceTest {
    final static Logger LOG = LoggerFactory.getLogger(PistesyottoResourceTest.class);
    private static final String KIELIKOE_KOULUTUSTOIMIJA_OID = "1.2.246.562.10.45042175963";
    private static final String KIELIKOE_OPPILAITOS_OID = "1.2.246.562.10.45698499378";
    private static final String KIELIKOE_TOIMIPISTE_OID = "1.2.3.44444.5";
    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResourceBuilder.WebClientExposingHttpResource pistesyottoTuontiResource = new HttpResourceBuilder()
            .address(root + "/pistesyotto/tuonti").buildExposingWebClientDangerously();
    final HttpResourceBuilder.WebClientExposingHttpResource pistesyottoVientiResource = new HttpResourceBuilder()
            .address(root + "/pistesyotto/vienti").buildExposingWebClientDangerously();
    final HttpResourceBuilder.WebClientExposingHttpResource pistesyottoUlkoinenTuontiResource = new HttpResourceBuilder()
            .address(root + "/pistesyotto/ulkoinen").buildExposingWebClientDangerously();
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
    private final List<HakemusWrapper> hakemukset = Arrays.asList(
            hakemus()
                    .setOid(HAKEMUS1)
                    .setHenkilotunnus("123456-789x")
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build(),
            hakemus()
                    .setOid(HAKEMUS2)
                    .setSyntymaaika("1.1.1900")
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build(),
            hakemus()
                    .setOid(HAKEMUS3)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build()
    );
    private final List<ValintakoeOsallistuminenDTO> osallistumistiedot = Arrays.asList(
            osallistuminen()
                    .setHakemusOid(HAKEMUS1)
                    .hakutoive()
                    .setHakukohdeOid(HAKUKOHDE1)
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
                    .setHakukohdeOid(HAKUKOHDE1)
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
                    .setHakukohdeOid(HAKUKOHDE1)
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
    private final OrganisaatioTyyppiHierarkia kielikokeitaJarjestavanOppilaitoksenHierarkia
        = new OrganisaatioTyyppiHierarkia(1, Collections.singletonList(
        new OrganisaatioTyyppi(KIELIKOE_KOULUTUSTOIMIJA_OID,
            ImmutableMap.of("fi", "Itä-Savon koulutuskuntayhtymä"),
            Collections.singletonList(
                new OrganisaatioTyyppi(KIELIKOE_OPPILAITOS_OID,
                    ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto"),
                    Collections.singletonList(
                        new OrganisaatioTyyppi(KIELIKOE_TOIMIPISTE_OID,
                            ImmutableMap.of("fi", "Savonlinnan ammatti- ja aikuisopisto, SAMI, kulttuuriala"),
                            Collections.emptyList(),
                            null,
                            Collections.singletonList("TOIMIPISTE")
                        )
                    ),
                    "oppilaitostyyppi_21#1",
                    Collections.singletonList("OPPILAITOS")
                )
            ),
            null,
            Collections.singletonList("KOULUTUSTOIMIJA")
        )
    ));
    private final Response okResponse = Response.ok("Success in " + getClass().getSimpleName()).build();
    private PistesyottoExcel pistesyottoExcel = new PistesyottoExcel(HAKU1, HAKUKOHDE1,
            KIELIKOE_TOIMIPISTE_OID, "", "", "",
            Optional.empty(),
            Arrays.asList(
                    hakemus().setOid(HAKEMUS1).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build(),
                    hakemus().setOid(HAKEMUS2).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build(),
                    hakemus().setOid(HAKEMUS3).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build()),
            Sets.newHashSet(Collections.singletonList(VALINTAKOE1)), // KAIKKI KUTSUTAAN TUNNISTEET
            Collections.singletonList(VALINTAKOE1), // TUNNISTEET
            osallistumistiedot,
            valintaperusteet,
            Arrays.asList(
                    lisatiedot().setOid(HAKEMUS1).setPersonOid(PERSONOID1).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                            .addLisatieto(TUNNISTE1, "3")
                            .addLisatieto(TUNNISTE2, "true")
                            .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                            .addLisatieto(KIELIKOE_TUNNISTE, "true")
                            .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                            .build(),
                    lisatiedot().setOid(HAKEMUS2).setPersonOid(PERSONOID2).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                            .addLisatieto(TUNNISTE1, "2")
                            .addLisatieto(TUNNISTE2, "true")
                            .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                            .addLisatieto(KIELIKOE_TUNNISTE, "false")
                            .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                            .build(),
                    lisatiedot().setOid(HAKEMUS3).setPersonOid(PERSONOID3).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                            .addLisatieto(TUNNISTE1, "")
                            .build()),
            Collections.emptyList());
    private List<Valintapisteet> tuodutPisteet;

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

        HakemusWrapper hakemus1 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid1).addHakutoive(hakukohdeOid1).setPersonOid(hakemusPersonOid1).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build();
        HakemusWrapper hakemus2 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid2).addHakutoive(hakukohdeOid1).setPersonOid(hakemusPersonOid2).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build();
        HakemusWrapper hakemus3 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid3).addHakutoive(hakukohdeOid1).setPersonOid(hakemusInvalidPersonOid3).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build();
        HakemusWrapper hakemus4 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid4).addHakutoive(hakukohdeOid1).setPersonOid(hakemusPersonOid4).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build();
        HakemusWrapper hakemus5 = new HakemusSpec.HakemusBuilder().setOid(hakemusOid5).addHakutoive(hakukohdeOid2).setPersonOid(hakemusPersonOid5).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build();


        List<HakemusWrapper> hakemuses = Arrays.asList(hakemus1, hakemus2, hakemus3, hakemus4, hakemus5);
        MockApplicationAsyncResource.setResult(hakemuses);
        MockApplicationAsyncResource.setResultByOid(hakemuses);

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

        Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.anyCollectionOf(String.class), Mockito.any(AuditSession.class)))
            .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), Collections.emptyList())));
        Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.eq(HAKU1), Mockito.eq(HAKUKOHDE1), Mockito.any(AuditSession.class)))
            .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), Collections.emptyList())));
        Mockito.when(Mocks.getValintapisteAsyncResource().putValintapisteet(Mockito.eq(Optional.empty()), Mockito.anyListOf(Valintapisteet.class), Mockito.any(AuditSession.class)))
            .thenReturn(Observable.just(Collections.emptySet()));

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
    public void pistesyottoUlkoinenTuontiResourceWithBadInput() {
        final String tunniste1 = "1234";
        final String hakemusOid1 = "12316.7.7.74";
        final String hakemusPersonOid1 = "1.2.4124.41214";
        final String hakukohdeOid1 = "1.2.3.4";
        final String hakukohdeOid2 = "1.2.3.5";

        cleanMocks();

        List<HakemusWrapper> hakemuses = Collections.singletonList(new HakemusSpec.HakemusBuilder().setOid(hakemusOid1).addHakutoive(hakukohdeOid1).setPersonOid(hakemusPersonOid1).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build());
        MockApplicationAsyncResource.setResult(hakemuses);
        MockApplicationAsyncResource.setResultByOid(hakemuses);

        HakukohdeJaValintaperusteDTO vp = new HakukohdeJaValintaperusteDTO(hakukohdeOid1,
            Collections.singletonList(
                new ValintaperusteetSpec.ValintaperusteBuilder().setTunniste(tunniste1).setLukuarvofunktio().setArvot("8.34", "5.00").build()));
        HakukohdeJaValintaperusteDTO vp2 = new HakukohdeJaValintaperusteDTO(hakukohdeOid2,
            Collections.singletonList(
                new ValintaperusteetSpec.ValintaperusteBuilder().setTunniste(tunniste1).setLukuarvofunktio().setArvot("8.34", "5.00").build()));
        MockValintaperusteetAsyncResource.setHakukohdeValintaperusteResult(Arrays.asList(vp, vp2));

        HakukohdeJaValintakoeDTO vk = new HakukohdeJaValintakoeDTO(hakukohdeOid1,
            Collections.singletonList(
                new ValintaperusteetSpec.ValintakoeDTOBuilder().setTunniste(tunniste1).setKaikkiKutsutaan().build()));
        MockValintaperusteetAsyncResource.setHakukohdeResult(Collections.singletonList(vk));

        List<ValintakoeOsallistuminenDTO> osallistuminenDTOs =
            Collections.singletonList(new ValintalaskentaSpec.ValintakoeOsallistuminenBuilder().hakutoive().setHakukohdeOid(hakukohdeOid1).build().build());
        MockValintalaskentaValintakoeAsyncResource.setResult(osallistuminenDTOs);

        ValintakoeDTO koeToInput = new ValintakoeDTO(tunniste1, ValintakoeDTO.Osallistuminen.OSALLISTUI, "5.00");
        List<HakemusDTO> hakemusesToInput = Collections.singletonList(new HakemusDTO(hakemusOid1, hakemusPersonOid1, Collections.singletonList(koeToInput)));

        Mockito.when(Mocks.getValintapisteAsyncResource().putValintapisteet(Mockito.eq(Optional.empty()), Mockito.anyListOf(Valintapisteet.class), Mockito.any(AuditSession.class)))
            .thenReturn(Observable.just(Collections.emptySet()));
        JsonObject goodResponseJson = postPisteet(hakemusesToInput);
        assertEquals(goodResponseJson.get("kasiteltyOk").getAsInt(), 1);
        assertThat(Lists.newArrayList(goodResponseJson.get("virheet").getAsJsonArray()), hasSize(0));

        koeToInput.setPisteet(null);

        JsonObject badResponseJson = postPisteet(hakemusesToInput);
        assertEquals(badResponseJson.get("kasiteltyOk").getAsInt(), 0);
        ArrayList<JsonElement> errors = Lists.newArrayList(badResponseJson.get("virheet").getAsJsonArray());
        assertThat(errors, hasSize(1));
        assertThat(errors.get(0).getAsJsonObject().get("virhe").getAsString(), equalTo(
            "Validointivirhe pisteiden (null) tuonnissa tunnisteelle 1234. " +
                "Pisteiden arvon 'null' muuntaminen numeroksi ei onnistunut"));
    }

    private JsonObject postPisteet(List<HakemusDTO> hakemusesToInput) {
        Response goodInputResponse = pistesyottoUlkoinenTuontiResource.getWebClient()
                .query("hakuOid", "HAKUOID").post(Entity.entity(hakemusesToInput, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, goodInputResponse.getStatus());
        return parseJson(goodInputResponse);
    }

    private JsonObject parseJson(Response goodInputResponse) {
        String goodResponseJson = goodInputResponse.readEntity(String.class);
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(goodResponseJson);
        return je.getAsJsonObject();
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
                            .setOsallistumisenTunniste(TUNNISTE1 + "-OSALLISTUMINEN")
                            .setLukuarvofunktio()
                            .setArvot("1", "2", "3")
                            .build()
            );

            MockValintaperusteetAsyncResource.setValintaperusteetResult(
                    valintaperusteet
            );
            mockValintakokeetHakukohteille();
            MockApplicationAsyncResource.setResult(Arrays.asList(
                    hakemus()
                            .setOid(HAKEMUS1)
                            .setHenkilotunnus("123456-789x")
                            .setEtunimiJaSukunimi("Hilla", "Hiiri")
                            .build()
            ));
            MockApplicationAsyncResource.setResultByOid(Arrays.asList(
                    hakemus()
                            .setOid(HAKEMUS2)
                            .setSyntymaaika("1.1.1900")
                            .setEtunimiJaSukunimi("Hellevi", "Hiiri")
                            .build()
            ));
            List<ApplicationAdditionalDataDTO> additionalDataResult = Arrays.asList(
                lisatiedot()
                    .setOid(HAKEMUS1)
                    .setEtunimiJaSukunimi("Hilla", "Hiiri")
                    .build());
            MockApplicationAsyncResource.setAdditionalDataResult(additionalDataResult);
            List<ApplicationAdditionalDataDTO> additionalDataResultByOid = Arrays.asList(
                lisatiedot()
                    .setOid(HAKEMUS2)
                    .setEtunimiJaSukunimi("Hellevi", "Hiiri")
                    .build());
            MockApplicationAsyncResource.setAdditionalDataResultByOid(additionalDataResultByOid);

            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

            List<Valintapisteet> kaikkiPisteet = new ArrayList<>();
            kaikkiPisteet.addAll(asValintapisteet(additionalDataResult));
            kaikkiPisteet.addAll(asValintapisteet(additionalDataResultByOid));

            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.anyCollectionOf(String.class), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResultByOid))));
            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.eq(HAKU1), Mockito.eq(HAKUKOHDE1), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), kaikkiPisteet)));

            ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(), Mockito.anyString(),
                    inputStreamArgumentCaptor.capture()))
                    .thenReturn(Observable.just(okResponse));

            Response r = pistesyottoVientiResource.getWebClient()
                    .query("hakuOid", HAKU1)
                    .query("hakukohdeOid", HAKUKOHDE1)
                    .post(Entity.entity("", "application/json"));
            assertEquals(200, r.getStatus());
            Thread.sleep(2000);
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
                            .setHakukohdeOid(HAKUKOHDE1)
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
                            .setHakukohdeOid(HAKUKOHDE1)
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
            mockValintakokeetHakukohteille();
            MockApplicationAsyncResource.setResult(hakemukset);
            MockSuoritusrekisteriAsyncResource.setResult(
                    new SuoritusrekisteriSpec.OppijaBuilder()
                        .setOppijanumero(PERSONOID1)
                        .suoritus()
                        .setHenkiloOid(PERSONOID1)
                        .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                        .setMyontaja(HAKEMUS1)
                        .arvosana()
                        .setAine(KIELIKOE)
                        .setLisatieto("FI")
                        .setArvosana("hyvaksytty")
                        .build()
                        .build()
                        .suoritus()
                        .setHenkiloOid(PERSONOID1)
                        .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                        .setMyontaja(HAKEMUS3)
                        .arvosana()
                        .setAine(KIELIKOE)
                        .setLisatieto("FI")
                        .setArvosana("hylatty")
                        .build()
                        .build()
                        .suoritus()
                        .setHenkiloOid(PERSONOID1)
                        .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                        .setMyontaja(HAKEMUS1)
                        .arvosana()
                        .setAine(KIELIKOE)
                        .setLisatieto("SV")
                        .setArvosana("hylatty")
                        .build()
                        .build()
                        .build());
            List<ApplicationAdditionalDataDTO> additionalDataResult = Arrays.asList(
                lisatiedot()
                    .setOid(HAKEMUS1)
                    .setPersonOid(PERSONOID1)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build(),
                lisatiedot()
                    .setOid(HAKEMUS2)
                    .setPersonOid(PERSONOID2)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .addLisatieto(TUNNISTE1, "true")
                    .addLisatieto(OSALLISTUMISENTUNNISTE1, "OSALLISTUI")
                    .build());
            MockApplicationAsyncResource.setAdditionalDataResult(additionalDataResult);

            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.anyCollectionOf(String.class), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), Collections.emptyList())));
            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.eq(HAKU1), Mockito.eq(HAKUKOHDE1), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResult))));
                Mockito.when(Mocks.getValintapisteAsyncResource().putValintapisteet(Mockito.eq(Optional.empty()), Mockito.anyListOf(Valintapisteet.class), Mockito.any(AuditSession.class)))
                    .thenAnswer((Answer<Observable<Set<String>>>) invocation -> {
                        tuodutPisteet = invocation.getArgumentAt(1, List.class);
                        return Observable.just(Collections.emptySet());
                    })
                    .thenReturn(Observable.just(Collections.emptySet()));

            ArgumentCaptor<InputStream> inputStreamArgumentCaptor = ArgumentCaptor.forClass(InputStream.class);
            Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(), Mockito.anyString(),
                    inputStreamArgumentCaptor.capture())).thenReturn(Observable.just(okResponse));

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
            assertRivi(hakemus1Rivi, new String[]{HAKEMUS1, "Sukunimi, Etunimi", "123456-789x", null, "Tyhjä", "Merkitsemättä", "Hyväksytty", "Osallistui"});
            Rivi hakemus2Rivi = rivit.stream().filter(rivi -> rivi.getSolut().stream().anyMatch(solu -> HAKEMUS2.equals(solu.toTeksti().getTeksti()))).findFirst().get();
            assertRivi(hakemus2Rivi, new String[]{HAKEMUS2, "Sukunimi, Etunimi", null, "1.1.1900", "Tyhjä", "Merkitsemättä", "Tyhjä", "Merkitsemättä"});

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
                assertEquals(expectedSolut[i], solut[i].toTeksti().getTeksti());
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
                        .setHakukohdeOid(HAKUKOHDE1)
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
                        .setHakukohdeOid(HAKUKOHDE1)
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
                        .setHakukohdeOid(HAKUKOHDE1)
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
        MockApplicationAsyncResource.setResult(hakemukset);
        MockApplicationAsyncResource.setResultByOid(hakemukset);
        MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
            List<ApplicationAdditionalDataDTO> additionalDataResult = Arrays.asList(
                lisatiedot()
                        .setOid(HAKEMUS1)
                        .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                        .build());
            MockApplicationAsyncResource.setAdditionalDataResult(additionalDataResult);
            List<ApplicationAdditionalDataDTO> additionalDataResultByOid = Arrays.asList(
                lisatiedot()
                        .setOid(HAKEMUS2)
                        .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                        .build(),
                lisatiedot()
                        .setOid(HAKEMUS3)
                        .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                        .build()
            );
            MockApplicationAsyncResource.setAdditionalDataResultByOid(additionalDataResultByOid);
        mockValintakokeetHakukohteille();
        mockDokumenttiAsyncResourceTallenna();
        MockSuoritusrekisteriAsyncResource.setResult(new Oppija());
        MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

        Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.anyCollectionOf(String.class), Mockito.any(AuditSession.class)))
            .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResultByOid))));
        Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.eq(HAKU1), Mockito.eq(HAKUKOHDE1), Mockito.any(AuditSession.class)))
            .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResult))));
            Mockito.when(Mocks.getValintapisteAsyncResource().putValintapisteet(Mockito.eq(Optional.empty()), Mockito.anyListOf(Valintapisteet.class), Mockito.any(AuditSession.class)))
                .thenAnswer((Answer<Observable<Set<String>>>) invocation -> {
                    tuodutPisteet = invocation.getArgumentAt(1, List.class);
                    return Observable.just(Collections.emptySet());
                })
                .thenReturn(Observable.just(Collections.emptySet()));

        PistesyottoExcel excel = new PistesyottoExcel(HAKU1, HAKUKOHDE1,
                KIELIKOE_TOIMIPISTE_OID, "", "", "",
                Optional.empty(),
                Arrays.asList(
                        hakemus()
                                .setOid(HAKEMUS1)
                                .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                .build(),
                        hakemus()
                                .setOid(HAKEMUS2)
                                .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                .build(),
                        hakemus()
                                .setOid(HAKEMUS3)
                                .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
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
                                .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                .build(),
                        lisatiedot()
                                .setOid(HAKEMUS2)
                                .addLisatieto(TUNNISTE1, "2")
                                .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                .build(),
                        lisatiedot()
                                .setOid(HAKEMUS3)
                                .addLisatieto(TUNNISTE1, "")
                                .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                .build()
                ), null);
        MockOrganisaationAsyncResource.setOrganisaationTyyppiHierarkia(kielikokeitaJarjestavanOppilaitoksenHierarkia);

        Response r =
                pistesyottoTuontiResource.getWebClient()
                        .query("hakuOid", HAKU1)
                        .query("hakukohdeOid",HAKUKOHDE1)
                        .post(Entity.entity(excel.getExcel().vieXlsx(),
                                MediaType.APPLICATION_OCTET_STREAM));
        assertEquals(204, r.getStatus());
        assertEquals("Oletettiin että hakukohteen hakemukselle että ulkopuoliselle hakemukselle tuotiin pisteet!", 3, tuodutPisteet.size());
        } finally {
            cleanMocks();
        }
    }

    @Test
    public void pistesyottoTuonti2Test() {
        cleanMocks();
        try {
            MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
            MockApplicationAsyncResource.setResult(hakemukset);
            MockApplicationAsyncResource.setResultByOid(hakemukset);
            List<ApplicationAdditionalDataDTO> additionalDataResult = Arrays.asList(
                lisatiedot()
                    .setPersonOid(PERSONOID1)
                    .setOid(HAKEMUS1)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build());
            MockApplicationAsyncResource.setAdditionalDataResult(additionalDataResult);
            List<ApplicationAdditionalDataDTO> additionalDataResultByOid = Arrays.asList(
                lisatiedot()
                    .setPersonOid(PERSONOID2)
                    .setOid(HAKEMUS2)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build(),
                lisatiedot()
                    .setPersonOid(PERSONOID3)
                    .setOid(HAKEMUS3)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build()
            );
            MockApplicationAsyncResource.setAdditionalDataResultByOid(additionalDataResultByOid);
            MockSuoritusrekisteriAsyncResource.setResult(
                new SuoritusrekisteriSpec.OppijaBuilder()
                    .setOppijanumero(PERSONOID1)
                    .suoritus()
                    .setHenkiloOid(PERSONOID1)
                    .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                    .arvosana()
                    .setAine(KIELIKOE)
                    .setLisatieto("FI")
                    .setArvosana("hyvaksytty")
                    .build()
                    .build()
                    .suoritus()
                    .setHenkiloOid(PERSONOID1)
                    .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                    .arvosana()
                    .setAine(KIELIKOE)
                    .setLisatieto("SV")
                    .setArvosana("hylatty")
                    .build()
                    .build()
                    .build());
            mockValintakokeetHakukohteille();
            mockDokumenttiAsyncResourceTallenna();
            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);

            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.anyCollectionOf(String.class), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResultByOid))));
            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.eq(HAKU1), Mockito.eq(HAKUKOHDE1), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResult))));
                Mockito.when(Mocks.getValintapisteAsyncResource().putValintapisteet(Mockito.eq(Optional.empty()), Mockito.anyListOf(Valintapisteet.class), Mockito.any(AuditSession.class)))
                    .thenAnswer((Answer<Observable<Set<String>>>) invocation -> {
                        tuodutPisteet = invocation.getArgumentAt(1, List.class);
                        return Observable.just(Collections.emptySet());
                    })
                    .thenReturn(Observable.just(Collections.emptySet()));

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
                    KIELIKOE_TOIMIPISTE_OID, "", "", "",
                    Optional.empty(),
                    Arrays.asList(
                            hakemus().setOid(HAKEMUS1).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build(),
                            hakemus().setOid(HAKEMUS2).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build(),
                            hakemus().setOid(HAKEMUS3).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build()),
                    Sets.newHashSet(Arrays.asList(VALINTAKOE1)), // KAIKKI KUTSUTAAN TUNNISTEET
                    Arrays.asList(VALINTAKOE1), // TUNNISTEET
                    osallistumistiedot,
                    valintaperusteet,
                    Arrays.asList(
                            lisatiedot().setOid(HAKEMUS1).setPersonOid(PERSONOID1).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                    .addLisatieto(TUNNISTE1, "3")
                                    .addLisatieto(TUNNISTE2, "true")
                                    .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                                    .addLisatieto(KIELIKOE_TUNNISTE, "true")
                                    .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                                    .build(),
                            lisatiedot().setOid(HAKEMUS2).setPersonOid(PERSONOID2).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                    .addLisatieto(TUNNISTE1, "2")
                                    .addLisatieto(TUNNISTE2, "true")
                                    .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                                    .addLisatieto(KIELIKOE_TUNNISTE, "false")
                                    .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                                    .build(),
                            lisatiedot().setOid(HAKEMUS3).setPersonOid(PERSONOID3).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                    .addLisatieto(TUNNISTE1, "")
                                    .build()),
                    Collections.emptyList());

            Response r =
                    pistesyottoTuontiResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid",HAKUKOHDE1)
                            .post(Entity.entity(excel.getExcel().vieXlsx(),
                                    MediaType.APPLICATION_OCTET_STREAM));
            assertEquals(204, r.getStatus());
            assertEquals("Oletettiin että hakukohteen hakemukselle että ulkopuoliselle hakemukselle tuotiin pisteet!", 3, tuodutPisteet.size());
            assertTrue("Kielikokeita ei löytyä valinta-piste-servicestä", tuodutPisteet.stream().anyMatch(v -> v.getPisteet().stream().anyMatch(p -> p.getTunniste().equals("kielikoe_fi"))));
            assertThat("Arvosanoilla on oikea lähde", MockSuoritusrekisteriAsyncResource.createdArvosanatRef.get().stream().filter(a -> "1.2.246.562.10.45698499378".equals(a.getSource())).collect(Collectors.toList()), hasSize(2));
            assertEquals("Suorituksilla on oikea myöntäjä", 1, MockSuoritusrekisteriAsyncResource.suorituksetRef.get().stream().filter(s -> s.getMyontaja().equals(HAKEMUS1)).count());
            assertEquals("Suorituksilla on oikea myöntäjä", 1, MockSuoritusrekisteriAsyncResource.suorituksetRef.get().stream().filter(s -> s.getMyontaja().equals(HAKEMUS2)).count());
            assertEquals("Kielikokeiden suoritukset löytyvät suresta", 2, MockSuoritusrekisteriAsyncResource.suorituksetRef.get().size());
            assertEquals("Kielikokeiden arvosanat löytyvät suresta", 2, MockSuoritusrekisteriAsyncResource.createdArvosanatRef.get().size());
        } finally {
            cleanMocks();
        }
    }

    @Test
    public void pistesyottoTuontiVirheellisestiSortatuillaHakemuksillaTest() {
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
        mockValintakokeetHakukohteille();
        mockDokumenttiAsyncResourceTallenna();
        MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
        Response r =
                pistesyottoTuontiResource.getWebClient()
                        .query("hakuOid", HAKU1)
                        .query("hakukohdeOid",HAKUKOHDE1)
                        .post(Entity.entity(
                                PistesyottoResourceTest.class.getResourceAsStream("/virheellisesti_sortattu_excel.xlsx"),
                                MediaType.APPLICATION_OCTET_STREAM));
        assertEquals(500, r.getStatus());
        List<ApplicationAdditionalDataDTO> tuodutLisatiedot = MockApplicationAsyncResource.getAdditionalDataInput();
        assertEquals("Oletettiin että hakukohteen hakemukselle että ulkopuoliselle hakemukselle tuotiin lisätiedot!",null, tuodutLisatiedot);
        } finally {
            cleanMocks();
        }
    }

    @Test
    public void pistesyottoTuontiSureVirheellaTest() {
        cleanMocks();
        try {
            MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
            MockApplicationAsyncResource.setResult(hakemukset);
            MockApplicationAsyncResource.setResultByOid(hakemukset);
            List<ApplicationAdditionalDataDTO> applicationAdditionalDataDtos = Arrays.asList(lisatiedot()
                .setOid(HAKEMUS1).setPersonOid(PERSONOID1).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build());
            MockApplicationAsyncResource.setAdditionalDataResult(applicationAdditionalDataDtos);
            List<ApplicationAdditionalDataDTO> applicationAddtionalDataDtosByOid = Arrays.asList(
                lisatiedot()
                    .setOid(HAKEMUS2).setPersonOid(PERSONOID2).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build(),
                lisatiedot()
                    .setOid(HAKEMUS3).setPersonOid(PERSONOID3).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build()
            );
            MockApplicationAsyncResource.setAdditionalDataResultByOid(
                applicationAddtionalDataDtosByOid
            );
            mockValintakokeetHakukohteille();
            mockDokumenttiAsyncResourceTallenna();

            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.anyCollectionOf(String.class), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(applicationAddtionalDataDtosByOid))));
            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.eq(HAKU1), Mockito.eq(HAKUKOHDE1), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(applicationAdditionalDataDtos))));
            Mockito.when(Mocks.getValintapisteAsyncResource().putValintapisteet(Mockito.eq(Optional.empty()), Mockito.anyListOf(Valintapisteet.class), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(Collections.emptySet()));

            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
            MockSuoritusrekisteriAsyncResource.setResult(
                new SuoritusrekisteriSpec.OppijaBuilder()
                    .setOppijanumero(PERSONOID1)
                    .suoritus()
                    .setHenkiloOid(PERSONOID1)
                    .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                    .setMyontaja(HAKEMUS1)
                    .arvosana()
                    .setAine(KIELIKOE)
                    .setLisatieto("FI")
                    .setArvosana("hyvaksytty")
                    .setSource(KIELIKOE_TOIMIPISTE_OID)
                    .build()
                    .arvosana()
                    .setAine(KIELIKOE)
                    .setLisatieto("SV")
                    .setArvosana("hylatty")
                    .setSource(KIELIKOE_TOIMIPISTE_OID)
                    .build()
                    .build()
                    .build());
            MockOrganisaationAsyncResource.setOrganisaationTyyppiHierarkia(
                kielikokeitaJarjestavanOppilaitoksenHierarkia
            );
            MockSuoritusrekisteriAsyncResource.setPostException(Optional.of(
                new HttpExceptionWithResponse("Something terrible happened", Response.serverError().entity("Boom").build())));

            Response r =
                    pistesyottoTuontiResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid",HAKUKOHDE1)
                            .post(Entity.entity(pistesyottoExcel.getExcel().vieXlsx(),
                                    MediaType.APPLICATION_OCTET_STREAM));
            assertEquals(500, r.getStatus());
        } finally {
            MockSuoritusrekisteriAsyncResource.clear();
            cleanMocks();
        }
    }

    @Test
    public void pistesyottoTuonti3Test() {
        cleanMocks();
        try {
            MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
            MockApplicationAsyncResource.setResult(hakemukset);
            MockApplicationAsyncResource.setResultByOid(hakemukset);
            List<ApplicationAdditionalDataDTO> additionalDataResult = Arrays.asList(
                lisatiedot()
                    .setPersonOid(PERSONOID1)
                    .setOid(HAKEMUS1)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build());
            MockApplicationAsyncResource.setAdditionalDataResult(additionalDataResult);
            List<ApplicationAdditionalDataDTO> additionalDataResultByOid = Arrays.asList(
                lisatiedot()
                    .setPersonOid(PERSONOID2)
                    .setOid(HAKEMUS2)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build(),
                lisatiedot()
                    .setPersonOid(PERSONOID3)
                    .setOid(HAKEMUS3)
                    .setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                    .build()
            );
            MockApplicationAsyncResource.setAdditionalDataResultByOid(additionalDataResultByOid);
            MockSuoritusrekisteriAsyncResource.setResult(
                    new SuoritusrekisteriSpec.OppijaBuilder()
                            .setOppijanumero(PERSONOID1)
                            .suoritus()
                            .setId("123-123-123-1")
                            .setSource("1.2.246.562.10.45698499379")
                            .setMyontaja(HAKEMUS1)
                            .setHenkiloOid(PERSONOID1)
                            .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                            .arvosana()
                                .setId("123-123-123-1-arvosana")
                                .setAine(KIELIKOE)
                                .setLisatieto("FI")
                                .setArvosana(hyvaksytty)
                                .build()
                            .build()
                            .suoritus()
                            .setId("123-123-123-2")
                            .setSource("1.2.246.562.10.45698499378")
                            .setMyontaja(HAKEMUS3)
                            .setHenkiloOid(PERSONOID1)
                            .setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                            .arvosana()
                                .setId("123-123-123-2-arvosana")
                                .setAine(KIELIKOE)
                                .setLisatieto("FI")
                                .setArvosana(hylatty)
                                .build()
                            .build()
                            .build());
            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
            MockOrganisaationAsyncResource.setOrganisaationTyyppiHierarkia(kielikokeitaJarjestavanOppilaitoksenHierarkia);
            mockValintakokeetHakukohteille();

            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.anyCollectionOf(String.class), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResultByOid))));
            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.eq(HAKU1), Mockito.eq(HAKUKOHDE1), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResult))));
                Mockito.when(Mocks.getValintapisteAsyncResource().putValintapisteet(Mockito.eq(Optional.empty()), Mockito.anyListOf(Valintapisteet.class), Mockito.any(AuditSession.class)))
                    .thenAnswer((Answer<Observable<Set<String>>>) invocation -> {
                        tuodutPisteet = invocation.getArgumentAt(1, List.class);
                        return Observable.just(Collections.emptySet());
                    })
                    .thenReturn(Observable.just(Collections.emptySet()));

            mockDokumenttiAsyncResourceTallenna();
            PistesyottoExcel excel = new PistesyottoExcel(HAKU1, HAKUKOHDE1,
                    KIELIKOE_TOIMIPISTE_OID, "", "", "",
                    Optional.empty(),
                    Arrays.asList(
                            hakemus().setOid(HAKEMUS1).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build(),
                            hakemus().setOid(HAKEMUS2).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build(),
                            hakemus().setOid(HAKEMUS3).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build()
                    ),
                    Sets.newHashSet(Arrays.asList(VALINTAKOE1)), // KAIKKI KUTSUTAAN TUNNISTEET
                    Arrays.asList(VALINTAKOE1), // TUNNISTEET
                    osallistumistiedot,
                    valintaperusteet,
                    Arrays.asList(
                            lisatiedot().setOid(HAKEMUS1).setPersonOid(PERSONOID1).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                    .addLisatieto(TUNNISTE1, "3")
                                    .addLisatieto(TUNNISTE2, "true")
                                    .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                                    .addLisatieto(KIELIKOE_TUNNISTE, "")
                                    .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "MERKITSEMATTA")
                                    .build(),
                            lisatiedot().setOid(HAKEMUS2).setPersonOid(PERSONOID2).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                    .addLisatieto(TUNNISTE1, "2")
                                    .addLisatieto(TUNNISTE2, "true")
                                    .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                                    .addLisatieto(KIELIKOE_TUNNISTE, "false")
                                    .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                                    .build(),
                            lisatiedot().setOid(HAKEMUS3).setPersonOid(PERSONOID3).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                                    .addLisatieto(TUNNISTE1, "")
                                    .build()),
                    Collections.emptyList());

            Response r =
                    pistesyottoTuontiResource.getWebClient()
                            .query("hakuOid", HAKU1)
                            .query("hakukohdeOid",HAKUKOHDE1)
                            .post(Entity.entity(excel.getExcel().vieXlsx(),
                                    MediaType.APPLICATION_OCTET_STREAM));
            assertEquals(204, r.getStatus());
            assertEquals("Oletettiin että hakukohteen hakemukselle että ulkopuoliselle hakemukselle tuotiin pisteet!", 3, tuodutPisteet.size());
            assertTrue("Kielikokeita löytyy pisteistä", tuodutPisteet.stream().anyMatch(a -> a.getPisteet().stream().anyMatch(p -> p.getTunniste().equals("kielikoe_fi"))));
            assertThat("Kielikokeen suoritus löytyy suresta", MockSuoritusrekisteriAsyncResource.suorituksetRef.get(), hasSize(1));
            //assertThat("Suresta löytyy oikea kielikoesuoritus", MockSuoritusrekisteriAsyncResource.suorituksetRef.get(), hasItem(withHenkiloOid(PERSONOID2))); // TODO eikö henkilöoid mene perille?
            assertThat("Suresta löytyy oikea kielikoesuoritus", MockSuoritusrekisteriAsyncResource.suorituksetRef.get(), not(hasItem(withHenkiloOid(PERSONOID3))));
            assertEquals("Suorituksella on oikea myöntäjä", 1, MockSuoritusrekisteriAsyncResource.suorituksetRef.get().stream().filter(s -> s.getMyontaja().equals(HAKEMUS2)).count());
            assertEquals("Suorituksella on oikea myöntäjä", 0, MockSuoritusrekisteriAsyncResource.suorituksetRef.get().stream().filter(s -> s.getMyontaja().equals(HAKEMUS3)).count());
            assertEquals("Arvosanoilla on oikea lähde", 1, MockSuoritusrekisteriAsyncResource.createdArvosanatRef.get().stream().filter(a -> a.getSource().equals("1.2.246.562.10.45698499378")).count());
            assertEquals("Kielikokeen arvosana löytyy suresta", 1, MockSuoritusrekisteriAsyncResource.createdArvosanatRef.get().size());
            assertThat("Oikea suoritus on deletoitu", MockSuoritusrekisteriAsyncResource.deletedSuorituksetRef.get(), hasItem("123-123-123-1"));
            assertThat("Oikea arvosana on deletoitu", MockSuoritusrekisteriAsyncResource.deletedArvosanatRef.get(), hasItem("123-123-123-1-arvosana"));
        } finally {
            cleanMocks();
        }
    }

    private Matcher<Suoritus> withHenkiloOid(String expectedHenkiloOid) {
        return new TypeSafeMatcher<Suoritus>() {
            @Override
            protected boolean matchesSafely(Suoritus item) {
                return expectedHenkiloOid.equals(item.getHenkiloOid());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("suoritus, jonka henkiloOid == " + expectedHenkiloOid);
            }
        };
    }

    @Test
    public void pistesyottoTuontiKunKaikkiAmmatillisenKielikoeTuloksetLoytyvatJoSurestaTest() {
        cleanMocks();
        try {
            MockValintaperusteetAsyncResource.setValintaperusteetResult(valintaperusteet);
            MockApplicationAsyncResource.setResult(hakemukset);
            MockApplicationAsyncResource.setResultByOid(hakemukset);
            List<ApplicationAdditionalDataDTO> additionalDataResult = Collections.singletonList(
                lisatiedot().setPersonOid(PERSONOID1).setOid(HAKEMUS1).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build());
            MockApplicationAsyncResource.setAdditionalDataResult(additionalDataResult);
            MockApplicationAsyncResource.setAdditionalDataResultByOid(Collections.emptyList());
            MockSuoritusrekisteriAsyncResource.setResult(new SuoritusrekisteriSpec.OppijaBuilder().setOppijanumero(PERSONOID1)
                .suoritus().setId("123-123-123-1").setMyontaja(HAKEMUS1).setHenkiloOid(PERSONOID1).setKomo(AMMATILLINEN_KIELIKOE_TYYPPI)
                    .setSource(KIELIKOE_OPPILAITOS_OID)
                    .arvosana().setAine(KIELIKOE).setLisatieto("FI").setArvosana(hyvaksytty).build()
                .build()
            .build());
            MockValintalaskentaValintakoeAsyncResource.setResult(osallistumistiedot);
            MockOrganisaationAsyncResource.setOrganisaationTyyppiHierarkia(kielikokeitaJarjestavanOppilaitoksenHierarkia);
            mockValintakokeetHakukohteille();

            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.anyCollectionOf(String.class), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), Collections.emptyList())));
            Mockito.when(Mocks.getValintapisteAsyncResource().getValintapisteet(Mockito.eq(HAKU1), Mockito.eq(HAKUKOHDE1), Mockito.any(AuditSession.class)))
                .thenReturn(Observable.just(new PisteetWithLastModified(Optional.empty(), asValintapisteet(additionalDataResult))));

            mockDokumenttiAsyncResourceTallenna();

            PistesyottoExcel excel = new PistesyottoExcel(HAKU1, HAKUKOHDE1, KIELIKOE_TOIMIPISTE_OID, "", "", "",
                Optional.empty(), Collections.singletonList(hakemus().setOid(HAKEMUS1).setEtunimiJaSukunimi("Etunimi", "Sukunimi").build()),
                Sets.newHashSet(Collections.singletonList(VALINTAKOE1)), // KAIKKI KUTSUTAAN TUNNISTEET
                Collections.singletonList(VALINTAKOE1), // TUNNISTEET
                osallistumistiedot,
                valintaperusteet,
                Collections.singletonList(lisatiedot().setOid(HAKEMUS1).setPersonOid(PERSONOID1).setEtunimiJaSukunimi("Etunimi", "Sukunimi")
                        .addLisatieto(TUNNISTE1, "3")
                        .addLisatieto(TUNNISTE2, "true")
                        .addLisatieto(OSALLISTUMISENTUNNISTE2, "OSALLISTUI")
                        .addLisatieto(KIELIKOE_TUNNISTE, "true")
                        .addLisatieto(KIELIKOE_OSALLISTUMISENTUNNISTE, "OSALLISTUI")
                        .build()),
                Collections.emptyList());

            Mockito.when(Mocks.getValintapisteAsyncResource().putValintapisteet(Mockito.eq(Optional.empty()), Mockito.anyListOf(Valintapisteet.class), Mockito.any(AuditSession.class)))
                .thenAnswer((Answer<Observable<Set<String>>>) invocation -> {
                    tuodutPisteet = invocation.getArgumentAt(1, List.class);
                    return Observable.just(Collections.emptySet());
                })
                .thenReturn(Observable.just(Collections.emptySet()));
            Response r = pistesyottoTuontiResource.getWebClient().query("hakuOid", HAKU1).query("hakukohdeOid", HAKUKOHDE1)
                .post(Entity.entity(excel.getExcel().vieXlsx(), MediaType.APPLICATION_OCTET_STREAM));

            assertEquals(204, r.getStatus());

            assertThat("Hakukohteen hakemukselle pisteet", tuodutPisteet, hasSize(1));
            assertTrue("Myös kielikokeet löytyvät valinta-piste-serviceen tuoduista pisteistä", tuodutPisteet.stream().anyMatch(a -> a.getPisteet().stream().anyMatch(p -> p.getTunniste().equals("kielikoe_fi"))));
            assertThat(MockSuoritusrekisteriAsyncResource.suorituksetRef.get(), hasSize(0));
            assertThat("Suorituksia ei deletoitu", MockSuoritusrekisteriAsyncResource.deletedSuorituksetRef.get(), hasSize(0));
        } finally {
            cleanMocks();
        }
    }

    public void mockDokumenttiAsyncResourceTallenna() {
        Mockito.when(Mocks.getDokumenttiAsyncResource().tallenna(
                Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyList(), Mockito.anyString(),
                Mockito.any(InputStream.class))).thenReturn(Observable.just(okResponse));
    }

    public void cleanMocks() {
        Mocks.reset();
        MockTarjontaAsyncService.clear();
        MockApplicationAsyncResource.clear();
        MockAtaruAsyncResource.clear();
        MockValintaperusteetAsyncResource.clear();
        MockValintalaskentaValintakoeAsyncResource.clear();
        MockSuoritusrekisteriAsyncResource.clear();
        MockOrganisaationAsyncResource.clear();
    }

    private void mockValintakokeetHakukohteille() {
        MockValintaperusteetAsyncResource.setHakukohdeResult(Collections.singletonList(
                hakukohdeJaValintakoe().addValintakoe(VALINTAKOE1).addValintakoe(KIELIKOE).build()));
    }

    private List<Valintapisteet> asValintapisteet(List<ApplicationAdditionalDataDTO> applicationAddtionalDataDtosByOid) {
        return applicationAddtionalDataDtosByOid.stream().map(PistesyotonTuontiTestBase.APPLICATION_ADDITIONAL_DATA_DTO_VALINTAPISTEET).collect(Collectors.toList());
    }
}
