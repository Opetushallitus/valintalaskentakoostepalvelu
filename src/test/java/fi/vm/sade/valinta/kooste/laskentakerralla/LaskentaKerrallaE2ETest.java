package fi.vm.sade.valinta.kooste.laskentakerralla;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJson;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.resourcesAddress;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.startShared;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.HAKUKOHDE1;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.HAKUKOHDE2;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.hakukohdeviite;
import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.valintaperusteet;
import static javax.ws.rs.HttpMethod.GET;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valinta.kooste.server.SeurantaServerMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Ignore
public class LaskentaKerrallaE2ETest {
    private final SeurantaServerMock seurantaServerMock = new SeurantaServerMock();

    @Before
    public void startServer() {
        startShared();
    }

    @Test
    public void testaaLaskentaa() {
        mockForward(seurantaServerMock);
        HttpResourceBuilder.WebClientExposingHttpResource http = new HttpResourceBuilder()
                .address(resourcesAddress + "/valintalaskentakerralla/haku/HAKUOID1/tyyppi/HAKU/whitelist/true")
                .buildExposingWebClientDangerously();
        mockToReturnJson(GET, "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/haku/.*",
                Arrays.asList(
                        hakukohdeviite()
                                .setHakukohdeOid(HAKUKOHDE1)
                                .build(),
                        hakukohdeviite()
                                .setHakukohdeOid(HAKUKOHDE2)
                                .build()
                )
        );
        mockToReturnJson(GET,
                "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/.*",
                Collections.singletonList(valintaperusteet().build())
        );
        mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/.*",new ParametriDTO());
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/haku/.*",new HakuV1RDTO());
        mockToReturnJson(GET, "/suoritusrekisteri/rest/v1/oppijat.*", Arrays.asList());
        mockToReturnJson(GET, "/haku-app/applications/listfull.*", Arrays.asList());
        MockServer fakeValintalaskenta = new MockServer();
        final Semaphore counter = new Semaphore(0);
        AtomicBoolean first = new AtomicBoolean(true);
        mockForward(
                fakeValintalaskenta.addHandler("/valintalaskenta-laskenta-service/resources/valintalaskenta/valintakokeet", exchange -> {
                    try {
                        // failataan tarkoituksella ensimmainen laskenta
                        if (first.getAndSet(false)) {
                            String resp = "ERR!";
                            exchange.sendResponseHeaders(505, resp.length());
                            exchange.getResponseBody().write(resp.getBytes());
                            exchange.getResponseBody().close();
                            return;
                        }
                        counter.release();
                        String resp = "OK!";
                        exchange.sendResponseHeaders(200, resp.length());
                        exchange.getResponseBody().write(resp.getBytes());
                        exchange.getResponseBody().close();
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }));
        Assert.assertEquals(200, http.getWebClient()
                .query("valintakoelaskenta", "true")
                .post(Entity.json(Arrays.asList(HAKUKOHDE1, HAKUKOHDE2))).getStatus());
        try {
            Assert.assertTrue(counter.tryAcquire(1, 10, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }
}
