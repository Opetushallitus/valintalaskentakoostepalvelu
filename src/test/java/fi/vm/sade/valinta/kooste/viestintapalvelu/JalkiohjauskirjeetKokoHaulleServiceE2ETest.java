package fi.vm.sade.valinta.kooste.viestintapalvelu;

import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
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
import static fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec.hakemus;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.POST;

public class JalkiohjauskirjeetKokoHaulleServiceE2ETest {
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
    public void testJalkiohjauskirjeet() throws InterruptedException {
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
        mockLetterKutsut("^(?!.*010111A321).*010111A123.*$");
        String dokumenttiId = makeCallAndReturnDokumenttiId("SV", null);
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
        mockToReturnJson(GET, "/sijoittelu-service/resources/sijoittelu/HAKU1/sijoitteluajo/latest/hakemukset.*", hp);
    }

    private void mockHakukohde1Kutsu() {
        HakukohdeV1RDTO hk = new HakukohdeV1RDTO();
        hk.setOpetusKielet(Sets.newHashSet("FI"));
        hk.setTarjoajaOids(Sets.newHashSet("T1","T2"));
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/hakukohde/HAKUKOHDE1/", new Result(hk));

    }

    private String makeCallAndReturnDokumenttiId(String asiointikieli, Boolean sahkoposti) {
        HttpResource http = new HttpResource(resourcesAddress + "/viestintapalvelu/jalkiohjauskirjeet/aktivoi");
        WebClient client = http.getWebClient()
                .query("hakuOid", HAKU1)
                .query("templateName","jalkiohjauskirje")
                .query("tag", "testTag");
        if(null != sahkoposti) {
            client.query("sahkoposti", sahkoposti);
        }
        DokumentinLisatiedot lisatiedot = new DokumentinLisatiedot(null, "testTag", "letterBodyText", asiointikieli, null);
        Response response = client.post(Entity.json(lisatiedot));
        Assert.assertEquals(200, response.getStatus());
        String body = response.readEntity(String.class);
        return body.substring(7, body.length()-2);
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
