package fi.vm.sade.valinta.kooste.laskentakerralla;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockForward;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.*;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.*;

import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.*;

import fi.vm.sade.valinta.kooste.Integraatiopalvelimet;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valinta.kooste.server.SeurantaServerMock;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.verify.VerificationTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.HttpMethod.*;

/**
 * @author Jussi Jartamo
 */
public class LaskentaKerrallaE2ETest {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaKerrallaE2ETest.class);
    private final SeurantaServerMock seurantaServerMock = new SeurantaServerMock();

    @Before
    public void startServer() throws Throwable{
        mockForward(seurantaServerMock);
        startShared();
    }

    @Test
    public void testaaLaskentaa() throws Throwable {
        try {
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
            mockToReturnJson(GET, "/ohjausparametrit-service/api/v1/rest/parametri/.*",
                    new ParametriDTO()
            );
            mockToReturnJson(GET, "/tarjonta-service/rest/v1/haku/.*",
                    new HakuV1RDTO()
            );

            mockToReturnJson(GET,
                    "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/.*",
                    Arrays.asList(
                            valintaperusteet()
                                    .build()
                    )
            );
            mockToReturnJson(GET, "/suoritusrekisteri/rest/v1/oppijat.*",
                    Arrays.asList()
            );
            mockToReturnJson(GET, "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/hakijaryhma/.*",
                    Arrays.asList()
            );
            mockToReturnJson(GET, "/haku-app/applications/listfull.*",
                    Arrays.asList()
            );
            mockToReturnString(POST,
                    "/seuranta-service/resources/seuranta/kuormantasaus/laskenta/" + UUID1 + "/hakukohde/" + HAKUKOHDE1 + "/tila/.*",
                    "OK!");
            MockServer fakeValintalaskenta = new MockServer();
            CyclicBarrier barrier = new CyclicBarrier(2);
            mockForward(
                    fakeValintalaskenta.addHandler("/valintalaskenta-laskenta-service/resources/valintalaskenta/laskekaikki", exchange -> {
                        try {
                            barrier.await(3L, TimeUnit.SECONDS);
                            String resp = "OK!";
                            exchange.sendResponseHeaders(200, resp.length());
                            exchange.getResponseBody().write(resp.getBytes());
                            exchange.getResponseBody().close();
                        } catch (Throwable t) {
                            throw new RuntimeException(t);
                        }
                    }));
            Assert.assertEquals(200, http.getWebClient().post(Entity.json(Arrays.asList(HAKUKOHDE1))).getStatus());
            barrier.await(3L, TimeUnit.SECONDS);
        } finally {
            mockServer.reset();
        }
    }
}
