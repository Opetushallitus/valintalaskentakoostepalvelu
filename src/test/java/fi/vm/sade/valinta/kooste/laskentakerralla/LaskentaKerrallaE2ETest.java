package fi.vm.sade.valinta.kooste.laskentakerralla;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.*;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.*;

import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.*;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valinta.kooste.server.SeurantaServerMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Action0;

import javax.ws.rs.client.Entity;
import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static javax.ws.rs.HttpMethod.*;

/**
 * @author Jussi Jartamo
 */
public class LaskentaKerrallaE2ETest {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaKerrallaE2ETest.class);
    private final SeurantaServerMock seurantaServerMock = new SeurantaServerMock();

    @Before
    public void startServer() throws Throwable{
        startShared();
    }

    @Test
    public void testaaLaskentaa() throws Throwable {
        mockForward(seurantaServerMock);
        HttpResource http = new HttpResource(resourcesAddress + "/valintalaskentakerralla/haku/HAKUOID1/tyyppi/HAKU/whitelist/true");
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
                Arrays.asList(
                        valintaperusteet()
                                .build()
                )
        );
        mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/.*",new ParametriDTO());
        mockToReturnJson(GET, "/tarjonta-service/rest/v1/haku/.*",new HakuV1RDTO());
        mockToReturnJson(GET, "/suoritusrekisteri/rest/v1/oppijat.*", Arrays.asList());
        mockToReturnJson(GET, "/haku-app/applications/listfull.*", Arrays.asList());
        MockServer fakeValintalaskenta = new MockServer();
        CyclicBarrier barrier = new CyclicBarrier(2);
        Action0 waitRequestForMax7Seconds = () ->{
            try {
                barrier.await(7L, TimeUnit.SECONDS);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        };
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
                        waitRequestForMax7Seconds.call();
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
        waitRequestForMax7Seconds.call();
    }
}
