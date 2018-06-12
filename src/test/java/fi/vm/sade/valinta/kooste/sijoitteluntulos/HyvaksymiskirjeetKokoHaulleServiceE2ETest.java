package fi.vm.sade.valinta.kooste.sijoitteluntulos;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonAndCheckBody;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnString;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKEMUS2;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKU1;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKUKOHDE1;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.HAKUKOHDE2;
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.HAKEMUS1;
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;
import com.google.common.collect.Sets;

import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.MockOpintopolkuCasAuthenticationFilter;
import fi.vm.sade.valinta.kooste.erillishaku.resource.dto.Prosessi;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.util.DokumenttiProsessiPoller;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.util.SecurityUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class HyvaksymiskirjeetKokoHaulleServiceE2ETest {

    @Before
    public void init() {
        startShared();
        mockParams();
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
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleYksiHyvaksyttyHakija() throws IOException {

        mockHakukohde1Kutsu();
        mockKorkeakouluHaku1Kutsu();
        mockYksiHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .buildHakuappHakemus(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .buildHakuappHakemus()
        ));

        mockKoodisto();

        mockLetterKutsut("^(?!.*HAKEMUS2).*HAKEMUS1.*$");
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleKaksiHyvaksyttyaHakija() throws IOException {

        mockHakukohde1Kutsu();
        mockKorkeakouluHaku1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .buildHakuappHakemus(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .buildHakuappHakemus()
        ));


        mockKoodisto();
        mockLetterKutsut(".*HAKEMUS1.*HAKEMUS2.*|.*HAKEMUS2.*HAKEMUS1.*");
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleSuodataAsiointikielella() throws IOException {

        mockHakukohde1Kutsu();
        mockKorkeakouluHaku1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .buildHakuappHakemus(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.SUOMI)
                        .buildHakuappHakemus()
        ));

        mockKoodisto();

        mockLetterKutsut("^(?!.*HAKEMUS2).*HAKEMUS1.*$");
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    //skipIPosti

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleMolemmilleHakijoilleVainSahkoposti() throws IOException {

        mockHakukohde1Kutsu();
        mockKorkeakouluHaku1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi2@sahkoposti.fi")
                        .buildHakuappHakemus(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .buildHakuappHakemus()
        ));

        mockKoodisto();

        mockLetterKutsut(".*HAKEMUS1.*(?=skipIPosti\":true).*HAKEMUS2.*(?=skipIPosti\":true).*");
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleToiselleHakijalleEiIPostia() throws IOException {

        mockHakukohde1Kutsu();
        mockKorkeakouluHaku1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .buildHakuappHakemus(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .buildHakuappHakemus()
        ));

        mockKoodisto();

        mockLetterKutsut(".*HAKEMUS1.*(?=skipIPosti\":true).*HAKEMUS2.*(?=skipIPosti\":true).*");
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleMolemmilleHakijoilleIPostia() throws IOException {

        mockHakukohde1Kutsu();
        mockKorkeakouluHaku1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .buildHakuappHakemus(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setVainSahkoinenViestinta(false)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .buildHakuappHakemus()
        ));

        mockKoodisto();

        mockLetterKutsut(".*HAKEMUS1.*(?=skipIPosti\":true).*HAKEMUS2.*(?=skipIPosti\":true).*");
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleIPostiToinenAste() throws IOException {

        mockHakukohde1Kutsu();
        mockToinenAsteHaku1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi2@sahkoposti.fi")
                        .buildHakuappHakemus(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setVainSahkoinenViestinta(false)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .buildHakuappHakemus()
        ));

        mockKoodisto();

        mockLetterKutsut(".*HAKEMUS1.*(?=skipIPosti\":true).*HAKEMUS2.*(?=skipIPosti\":true).*");
        ProsessiId dokumenttiId = makeCallAndReturnDokumenttiId("SV");
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    private ProsessiId makeCallAndReturnDokumenttiId(String asiointikieli) {
        HttpResourceBuilder.WebClientExposingHttpResource http = new HttpResourceBuilder()
                .address(resourcesAddress + "/sijoitteluntuloshaulle/hyvaksymiskirjeet")
                .buildExposingWebClientDangerously();
        WebClient client = http.getWebClient()
                .query("hakuOid", HAKU1)
                .query("asiointikieli", asiointikieli)
                .query("letterBodyText","letterBodyText");
        Response response = client.post(Entity.json(Arrays.asList(HAKUKOHDE1, HAKUKOHDE2)));
        Assert.assertEquals(200, response.getStatus());
        return response.readEntity(ProsessiId.class);
    }

    private void mockYksiHyvaksyttyKutsu() {
        HakijaDTO hakija1 = new HakijaDTO();
        hakija1.setHakemusOid(HAKEMUS1);
        HakutoiveDTO hakutoiveDTO = new HakutoiveDTO();
        hakutoiveDTO.setHakukohdeOid(HAKUKOHDE1);
        HakutoiveenValintatapajonoDTO jono = new HakutoiveenValintatapajonoDTO();
        jono.setTila(HakemuksenTila.HYVAKSYTTY);
        jono.setHyvaksytty(50);
        jono.setHakeneet(100);
        hakutoiveDTO.setHakutoiveenValintatapajonot(Collections.singletonList(jono));
        hakija1.setHakutoiveet(Sets.newTreeSet(Collections.singletonList(hakutoiveDTO)));
        HakijaPaginationObject hp = new HakijaPaginationObject();
        hp.setResults(Collections.singletonList(hakija1));
        hp.setTotalCount(hp.getResults().size());
        mockToReturnJson(GET, "/valinta-tulos-service/haku/HAKU1/hyvaksytyt", hp);
    }

    private void mockMolemmatHyvaksyttyKutsu() {
        HakutoiveDTO hakutoiveDTO = new HakutoiveDTO();
        hakutoiveDTO.setHakukohdeOid(HAKUKOHDE1);
        HakutoiveenValintatapajonoDTO jono = new HakutoiveenValintatapajonoDTO();
        jono.setTila(HakemuksenTila.HYVAKSYTTY);
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
        mockToReturnJson(GET, "/valinta-tulos-service/haku/HAKU1/hyvaksytyt", hp);
    }
    private void mockParams() {
        mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/HAKU1", new ParametritDTO());

    }
    private void mockHakukohde1Kutsu() {
        HakukohdeV1RDTO hk = new HakukohdeV1RDTO();
        hk.setOpetusKielet(Sets.newHashSet("FI"));
        hk.setTarjoajaOids(Sets.newHashSet("T1","T2"));
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/hakukohde/HAKUKOHDE1", new Result(hk));

    }

    private void mockKorkeakouluHaku1Kutsu() {
        HakuV1RDTO haku = new HakuV1RDTO();
        haku.setKohdejoukkoUri("haunkohdejoukko_12#1");
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/haku/HAKU1", new Result(haku));
    }

    private void mockToinenAsteHaku1Kutsu() {
        HakuV1RDTO haku = new HakuV1RDTO();
        haku.setKohdejoukkoUri("haunkohdejoukko_17#1");
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/haku/HAKU1", new Result(haku));
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

    private void pollAndAssertDokumenttiProsessi(ProsessiId dokumenttiId) {
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
