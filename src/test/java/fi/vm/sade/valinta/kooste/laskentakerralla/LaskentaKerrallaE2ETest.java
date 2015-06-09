package fi.vm.sade.valinta.kooste.laskentakerralla;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.http.HttpResource;
import static fi.vm.sade.valinta.kooste.ValintalaskentakoostepalveluJetty.*;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.*;

import static fi.vm.sade.valinta.kooste.spec.valintaperusteet.ValintaperusteetSpec.*;

import fi.vm.sade.valinta.kooste.Integraatiopalvelimet;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.server.SeurantaServerMock;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
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

/**
 * @author Jussi Jartamo
 */
@Ignore
public class LaskentaKerrallaE2ETest {
    private static final Logger LOG = LoggerFactory.getLogger(LaskentaKerrallaE2ETest.class);

    @Before
    public void startServer() throws Throwable{
        SeurantaServerMock.startServer();
        startShared();
    }

    @Test
    public void testaaLaskentaa() throws Throwable {
        HttpResource http = new HttpResource(resourcesAddress + "/valintalaskentakerralla/haku/HAKUOID1/tyyppi/HAKU/whitelist/true");
        mockGetToReturnJson("/valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/haku/.*",
                Arrays.asList(
                        hakukohdeviite()
                                .setHakukohdeOid(HAKUKOHDE1)
                                .build(),
                        hakukohdeviite()
                                .setHakukohdeOid(HAKUKOHDE2)
                        .build()
                )
        );

        mockGetToReturnJson("/ohjausparametrit-service/api/v1/rest/parametri/.*",
                new ParametriDTO()
        );
        mockGetToReturnJson("/tarjonta-service/rest/v1/haku/.*",
                new HakuV1RDTO()
        );

        mockGetToReturnJson(
                "/valintaperusteet-service/resources/valintalaskentakoostepalvelu/valintaperusteet/HAKUKOHDE1",
                Arrays.asList(
                        valintaperusteet()

                                .build()
                )
        );

        //mockPostToReturnString("/seuranta-service/resources/seuranta/kuormantasaus/laskenta/HAKUOID1/tyyppi/HAKU",UUID1);
        Response r = http.getWebClient().post(Entity.json(Arrays.asList(HAKUKOHDE1)));
        System.err.println(resourcesAddress + " " + r.getStatus());
        System.err.println(IOUtils.toString((InputStream)r.getEntity()));
        Thread.sleep(3000L);
    }
}
