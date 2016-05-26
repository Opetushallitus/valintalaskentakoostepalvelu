package fi.vm.sade.valinta.kooste.sijoitteluntulos;

import com.google.common.collect.Sets;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveenValintatapajonoDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatchStatusDto;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterResponse;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
@Ignore
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
    public void testaaHyvaksymiskirjeenLuontiaKokoHaulle() throws InterruptedException {
        HttpResource http = new HttpResource(resourcesAddress + "/sijoitteluntuloshaulle/hyvaksymiskirjeet");

        HakukohdeV1RDTO hk = new HakukohdeV1RDTO();
        hk.setOpetusKielet(Sets.newHashSet("FI"));
        hk.setTarjoajaOids(Sets.newHashSet("T1","T2"));
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/hakukohde/HAKUKOHDE1/", new Result(hk));

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

        TemplateHistory templateHistory = new TemplateHistory();
        templateHistory.setName("default");
        TemplateDetail detail = new TemplateDetail();
        detail.setName("sisalto");
        templateHistory.setTemplateReplacements(Collections.singletonList(detail));
        mockToReturnJson(GET, "/viestintapalvelu/api/v1/template/getHistory.*", Collections.singletonList(templateHistory));


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

        LetterResponse letterResponse = new LetterResponse();
        letterResponse.setBatchId("testBatchId");
        letterResponse.setStatus(LetterResponse.STATUS_SUCCESS);
        mockToReturnJsonAndCheckBody(POST, "/viestintapalvelu/api/v1/letter/async/letter", letterResponse, "^(?!.*HAKEMUS2).*HAKEMUS1.*$");

        LetterBatchStatusDto letterStatus = new LetterBatchStatusDto();
        letterStatus.setStatus("ready");
        mockToReturnJson(GET, "/viestintapalvelu/api/v1/letter/async/letter/status/testBatchId", letterStatus);

        Response response = http.getWebClient()
                .query("hakuOid", HAKU1)
                .query("asiointikieli", "SV")
                .query("letterBodyText","letterBodyText")
                .post(Entity.json(Arrays.asList(HAKUKOHDE1, HAKUKOHDE2)));

        String body = response.readEntity(String.class);

        String dokumenttiId = body.substring(7, body.length()-2);

        Assert.assertEquals(200, response.getStatus());

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
