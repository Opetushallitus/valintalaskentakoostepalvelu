package fi.vm.sade.valinta.kooste.viestintapalvelu;

import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.MockOpintopolkuCasAuthenticationFilter;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import fi.vm.sade.valinta.kooste.util.DokumenttiProsessiPoller;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.SecurityUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonAndCheckBody;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnString;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKEMUS1;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKEMUS2;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKU1;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKUKOHDE1;
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;

public class JalkiohjauskirjeetKokoHaulleServiceE2ETest {
    @BeforeClass
    public static void init() throws Throwable{
        startShared();
        MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication("ROLE_APP_HAKEMUS_READ_UPDATE_" + SecurityUtil.ROOTOID);
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

    @Test
    public void testJalkiohjauskirjeetYksiHylatty() throws Exception {
        mockYksiHylattyKutsu();
        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A123")
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A321")
                        .build()
        ));
        mockHakukohde1Kutsu();
        mockHaku1Kutsu();
        mockLetterKutsut("^(?!.*010111A321).*010111A123.*$");
        mockKoodisto();
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);
    }

    @Test
    public void testJalkiohjauskirjeetMolemmatHylatty() throws Exception {
        mockMolemmatHylattyKutsu();
        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A123")
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A321")
                        .build()
        ));
        mockHakukohde1Kutsu();
        mockHaku1Kutsu();
        mockLetterKutsut(".*010111A321.*010111A123.*|.*010111A123.*010111A321.*");
        mockKoodisto();
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);
    }

    @Test
    public void testJalkiohjauskirjeetSuodataAsiointikielella() throws Exception {
        mockMolemmatHylattyKutsu();
        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A123")
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.ENGLANTI)
                        .setHenkilotunnus("010111A321")
                        .build()
        ));
        mockHakukohde1Kutsu();
        mockHaku1Kutsu();
        mockLetterKutsut("^(?!.*010111A321).*010111A123.*$");
        mockKoodisto();
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);
    }

    @Test
    public void testJalkiohjauskirjeetMolemmilleVainSahkopostia() throws Exception {
        mockMolemmatHylattyKutsu();
        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A123")
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi2@sahkoposti.fi")
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A321")
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .build()
        ));
        mockHakukohde1Kutsu();
        mockHaku1Kutsu();
        mockLetterKutsut(".*010111A123.*(?=skipIPosti\":true).*010111A321.*(?=skipIPosti\":true).*");
        mockKoodisto();
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);
    }

    @Test
    public void testJalkiohjauskirjeetLahetaIPostiToiselle() throws Exception {
        mockMolemmatHylattyKutsu();
        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A123")
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A321")
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .build()
        ));
        mockHakukohde1Kutsu();
        mockHaku1Kutsu();
        mockLetterKutsut(".*010111A123.*(?=skipIPosti\":false).*010111A321.*(?=skipIPosti\":true).*");
        mockKoodisto();
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);
    }

    @Test
    public void testJalkiohjauskirjeetLahetaIPostiMolemmille() throws Exception {
        mockMolemmatHylattyKutsu();
        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A123")
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A321")
                        .setVainSahkoinenViestinta(false)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .build()
        ));
        mockHakukohde1Kutsu();
        mockHaku1Kutsu();
        mockLetterKutsut(".*010111A123.*(?=skipIPosti\":false).*010111A321.*(?=skipIPosti\":false).*");
        mockKoodisto();
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);
    }

    @Test
    public void testJalkiohjauskirjeetIPostiToinenAste() throws Exception {
        mockMolemmatHylattyKutsu();
        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A123")
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setHenkilotunnus("010111A321")
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .build()
        ));
        mockHakukohde1Kutsu();
        mockHaku1KutsuToinenAste();
        mockLetterKutsut(".*010111A123.*(?=skipIPosti\":false).*010111A321.*(?=skipIPosti\":false).*");
        mockKoodisto();
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);
    }

    private void mockLetterKutsut(String regex) {
        LetterResponse letterResponse = new LetterResponse();
        letterResponse.setBatchId("testBatchId");
        letterResponse.setStatus(LetterResponse.STATUS_SUCCESS);
        mockToReturnJsonAndCheckBody(POST, "/viestintapalvelu/api/v1/letter/async/letter", letterResponse, regex);

        LetterBatchStatusDto letterStatus = new LetterBatchStatusDto();
        letterStatus.setStatus("ready");
        mockToReturnJson(GET, "/viestintapalvelu/api/v1/letter/async/letter/status/testBatchId", letterStatus);
    }

    private void mockYksiHylattyKutsu() {
        HakijaDTO hakija1 = new HakijaDTO();
        hakija1.setHakemusOid(HAKEMUS1);
        HakutoiveDTO hakutoiveDTO = new HakutoiveDTO();
        hakutoiveDTO.setHakukohdeOid(HAKUKOHDE1);
        HakutoiveenValintatapajonoDTO jono = new HakutoiveenValintatapajonoDTO();
        jono.setTila(HakemuksenTila.HYLATTY);
        jono.setHyvaksytty(50);
        jono.setHakeneet(100);
        hakutoiveDTO.setHakutoiveenValintatapajonot(Collections.singletonList(jono));
        hakija1.setHakutoiveet(Sets.newTreeSet(Collections.singletonList(hakutoiveDTO)));
        HakijaPaginationObject hp = new HakijaPaginationObject();
        hp.setResults(Collections.singletonList(hakija1));
        hp.setTotalCount(hp.getResults().size());
        mockToReturnJson(GET, "/valinta-tulos-service/haku/HAKU1/ilmanHyvaksyntaa", hp);
    }

    private void mockMolemmatHylattyKutsu() {
        HakutoiveDTO hakutoiveDTO = new HakutoiveDTO();
        hakutoiveDTO.setHakukohdeOid(HAKUKOHDE1);
        HakutoiveenValintatapajonoDTO jono = new HakutoiveenValintatapajonoDTO();
        jono.setTila(HakemuksenTila.HYLATTY);
        jono.setHyvaksytty(50);
        jono.setHakeneet(100);
        hakutoiveDTO.setHakutoiveenValintatapajonot(Collections.singletonList(jono));

        HakijaDTO hakija1 = new HakijaDTO();
        hakija1.setHakemusOid(HAKEMUS1);
        hakija1.setHakutoiveet(Sets.newTreeSet(Collections.singletonList(hakutoiveDTO)));

        HakijaDTO hakija2 = new HakijaDTO();
        hakija2.setHakemusOid(HAKEMUS2);
        hakija2.setHakutoiveet(Sets.newTreeSet(Collections.singletonList(hakutoiveDTO)));

        HakijaPaginationObject hp = new HakijaPaginationObject();
        hp.setResults(Arrays.asList(hakija1, hakija2));
        hp.setTotalCount(hp.getResults().size());
        mockToReturnJson(GET, "/valinta-tulos-service/haku/HAKU1/ilmanHyvaksyntaa", hp);
    }

    private void mockHakukohde1Kutsu() {
        HakukohdeV1RDTO hk = new HakukohdeV1RDTO();
        hk.setOpetusKielet(Sets.newHashSet("FI"));
        hk.setTarjoajaOids(Sets.newHashSet("T1","T2"));
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/hakukohde/HAKUKOHDE1", new Result(hk));
    }

    private void mockHaku1Kutsu() {
        HakuV1RDTO haku = new HakuV1RDTO();
        haku.setKohdejoukkoUri("haunkohdejoukko_12#1");
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/haku/HAKU1", new Result(haku));

    }

    private void mockHaku1KutsuToinenAste() {
        HakuV1RDTO haku = new HakuV1RDTO();
        haku.setKohdejoukkoUri("haunkohdejoukko_11");
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/haku/HAKU1", new Result(haku));

    }

    private ProsessiId makeCallAndReturnDokumenttiId(String asiointikieli) {
        HttpResource http = new HttpResourceBuilder()
                .address(resourcesAddress + "/viestintapalvelu/jalkiohjauskirjeet/aktivoi")
                .build();
        WebClient client = http.getWebClient()
                .query("hakuOid", HAKU1)
                .query("templateName","jalkiohjauskirje")
                .query("tag", "testTag");
        DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot(null, "testTag", "letterBodyText", asiointikieli, null);
        Response response = client.post(Entity.json(lisatiedot));
        Assert.assertEquals(200, response.getStatus());
        return response.readEntity(ProsessiId.class);
    }

    private void pollAndAssertDokumenttiProsessi(ProsessiId dokumenttiId) throws InterruptedException {
        Prosessi valmisProsessi = DokumenttiProsessiPoller.pollDokumenttiProsessi(resourcesAddress, dokumenttiId, Prosessi::valmis);
        Assert.assertEquals(0, valmisProsessi.kokonaistyo.ohitettu);
        Assert.assertEquals(false, valmisProsessi.keskeytetty);
    }

    private void mockKoodisto() throws IOException {
        final String maatjavaltiot1 = IOUtils.toString(new ClassPathResource("/koodisto/maatjavaltiot1.json").getInputStream());
        mockToReturnString(GET, "/koodisto-service/rest/json/maatjavaltiot1/koodi", maatjavaltiot1);
        final String posti = IOUtils.toString(new ClassPathResource("/koodisto/posti.json").getInputStream());
        mockToReturnString(GET, "/koodisto-service/rest/json/posti/koodi", posti);
    }
}
