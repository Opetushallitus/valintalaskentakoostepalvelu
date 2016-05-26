package fi.vm.sade.valinta.kooste.sijoitteluntulos;

import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonAndCheckBody;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.*;
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.HAKEMUS1;
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;

/**
 * @author Jussi Jartamo
 */
public class HyvaksymiskirjeetKokoHaulleServiceE2ETest {
    @Before
    public void startServer() throws Throwable{
        startShared();
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
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleYksiHyvaksyttyHakija() throws InterruptedException {

        mockHakukohde1Kutsu();
        mockYksiHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .build()
        ));

        //TODO: Don't use Luokka (see KoosteTestProfileConfiguration)
        //mockToReturnJson(GET, "/koodisto-service/rest/json/maatjavaltiot1/koodi", Collections.singletonList(templateHistory));

        mockLetterKutsut("^(?!.*HAKEMUS2).*HAKEMUS1.*$");
        String dokumenttiId = makeCallAndReturnDokumenttiId("SV", null);
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleKaksiHyvaksyttyaHakija() throws InterruptedException {

        mockHakukohde1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .build()
        ));

        //TODO: Don't use Luokka (see KoosteTestProfileConfiguration)
        //mockToReturnJson(GET, "/koodisto-service/rest/json/maatjavaltiot1/koodi", Collections.singletonList(templateHistory));

        mockLetterKutsut(".*HAKEMUS1.*HAKEMUS2.*|.*HAKEMUS2.*HAKEMUS1.*");
        String dokumenttiId = makeCallAndReturnDokumenttiId("SV", null);
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleSuodataAsiointikielella() throws InterruptedException {

        mockHakukohde1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.SUOMI)
                        .build()
        ));

        //TODO: Don't use Luokka (see KoosteTestProfileConfiguration)
        //mockToReturnJson(GET, "/koodisto-service/rest/json/maatjavaltiot1/koodi", Collections.singletonList(templateHistory));

        mockLetterKutsut("^(?!.*HAKEMUS2).*HAKEMUS1.*$");
        String dokumenttiId = makeCallAndReturnDokumenttiId("SV", null);
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleSahkoposti() throws InterruptedException {

        mockHakukohde1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .build()
        ));

        //TODO: Don't use Luokka (see KoosteTestProfileConfiguration)
        //mockToReturnJson(GET, "/koodisto-service/rest/json/maatjavaltiot1/koodi", Collections.singletonList(templateHistory));

        mockLetterKutsut("^(?!.*HAKEMUS1).*HAKEMUS2.*$");
        String dokumenttiId = makeCallAndReturnDokumenttiId("SV", true);
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleIPosti1() throws InterruptedException {

        mockHakukohde1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setVainSahkoinenViestinta(true)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .build()
        ));

        //TODO: Don't use Luokka (see KoosteTestProfileConfiguration)
        //mockToReturnJson(GET, "/koodisto-service/rest/json/maatjavaltiot1/koodi", Collections.singletonList(templateHistory));

        mockLetterKutsut("^(?!.*HAKEMUS2).*HAKEMUS1.*$");
        String dokumenttiId = makeCallAndReturnDokumenttiId("SV", false);
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    @Test
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulleIPosti2() throws InterruptedException {

        mockHakukohde1Kutsu();
        mockMolemmatHyvaksyttyKutsu();

        mockToReturnJson(POST, "/haku-app/applications/list.*", Arrays.asList(
                hakemus()
                        .setOid(HAKEMUS1)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .build(),
                hakemus()
                        .setOid(HAKEMUS2)
                        .setAsiointikieli(KieliUtil.RUOTSI)
                        .setVainSahkoinenViestinta(false)
                        .setSahkoposti("testi@sahkoposti.fi")
                        .build()
        ));

        //TODO: Don't use Luokka (see KoosteTestProfileConfiguration)
        //mockToReturnJson(GET, "/koodisto-service/rest/json/maatjavaltiot1/koodi", Collections.singletonList(templateHistory));

        mockLetterKutsut(".*HAKEMUS1.*HAKEMUS2.*|.*HAKEMUS2.*HAKEMUS1.*");
        String dokumenttiId = makeCallAndReturnDokumenttiId("SV", false);
        pollAndAssertDokumenttiProsessi(dokumenttiId);

    }

    private String makeCallAndReturnDokumenttiId(String asiointikieli, Boolean sahkoposti) {
        HttpResource http = new HttpResource(resourcesAddress + "/sijoitteluntuloshaulle/hyvaksymiskirjeet");
        WebClient client = http.getWebClient()
                .query("hakuOid", HAKU1)
                .query("asiointikieli", asiointikieli)
                .query("letterBodyText","letterBodyText");
        if(null != sahkoposti) {
            client.query("sahkoposti", sahkoposti);
        }
        Response response = client.post(Entity.json(Arrays.asList(HAKUKOHDE1, HAKUKOHDE2)));
        Assert.assertEquals(200, response.getStatus());
        String body = response.readEntity(String.class);
        return body.substring(7, body.length()-2);
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
        mockToReturnJson(GET, "/sijoittelu-service/resources/sijoittelu/HAKU1/hyvaksytyt/.*", hp);
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
        mockToReturnJson(GET, "/sijoittelu-service/resources/sijoittelu/HAKU1/hyvaksytyt/.*", hp);
    }

    private void mockHakukohde1Kutsu() {
        HakukohdeV1RDTO hk = new HakukohdeV1RDTO();
        hk.setOpetusKielet(Sets.newHashSet("FI"));
        hk.setTarjoajaOids(Sets.newHashSet("T1","T2"));
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/hakukohde/HAKUKOHDE1/", new Result(hk));

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

    private void pollAndAssertDokumenttiProsessi(String dokumenttiId) throws InterruptedException {
        HttpResource http2 = new HttpResource(resourcesAddress + "/dokumenttiprosessi/" + dokumenttiId);
        String body2 = "";
        for(int i = 0; i < 10; i++) {
            Response response2 = http2.getWebClient().get();
            Assert.assertEquals(200, response2.getStatus());
            body2 = response2.readEntity(String.class);
            if(body2.contains("\"valmis\":false")) {
                Thread.sleep(2000);
            } else {
                break;
            }
        }
        Assert.assertTrue("valmis!=true " + body2, body2.contains("\"valmis\":true"));
        Assert.assertTrue("ohitettu!=0 " + body2, body2.contains("\"ohitettu\":0"));
        Assert.assertTrue("keskeytetty!=false " + body2, body2.contains("\"keskeytetty\":false"));
    }
}
