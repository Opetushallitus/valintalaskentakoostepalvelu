package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.server.MockServer;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.*;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static javax.ws.rs.HttpMethod.*;

/**
 * @author Jussi Jartamo
 */
public class PistesyottoE2ETest {

    @Before
    public void startServer() throws Throwable{
        startShared();
    }
    @Test
    public void tuonnissaEiYlikirjoitetaEditoimattomiaKenttiaHakemuspalveluun() throws Throwable {
        mockToReturnString(GET, "/valintalaskenta-laskenta-service/resources/valintakoe/hakutoive/1.2.246.562.5.85532589612",
                IOUtils.toString(new ClassPathResource("pistesyotto/List_ValintakoeOsallistuminenDTO.json").getInputStream())
        );
        mockToReturnString(GET,
                "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/avaimet/1.2.246.562.5.85532589612/",
                IOUtils.toString(new ClassPathResource("pistesyotto/List_ValintaperusteDTO.json").getInputStream())
        );
        mockToReturnString(GET,
                "/haku-app/applications/additionalData/testioidi1/1.2.246.562.5.85532589612",
                IOUtils.toString(new ClassPathResource("pistesyotto/List_ApplicationAdditionalDataDTO.json").getInputStream())
        );
        mockToReturnJson(POST,
                "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/valintakoe",
                Collections.emptyList()
        );

        HttpResource http = new HttpResource(resourcesAddress + "/pistesyotto/tuonti");


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
                        Assert.assertEquals("Editoimattomat lisätietokentät ohitetaan, eli viedään vain 836/1260.", 836, additionalData.stream()
                                .flatMap(a -> a.getAdditionalData().entrySet().stream())
                                .count());
                        counter.release();
                        exchange.sendResponseHeaders(200, 0);
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
            Assert.assertTrue(counter.tryAcquire(1, 10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }
}
