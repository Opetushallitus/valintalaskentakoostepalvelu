package fi.vm.sade.valinta.kooste.server;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.*;
import static javax.ws.rs.HttpMethod.*;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.*;
import static fi.vm.sade.valinta.kooste.spec.ConstantsSpec.UUID1;

/**
 * @author Jussi Jartamo
 */
public class SeurantaServerMock extends MockServer {

    private static final int mockSeurantaPort = PortChecker.findFreeLocalPort();
    private static final ConcurrentLinkedQueue<LaskentaDto> laskentaQueue = new ConcurrentLinkedQueue<>();

    public SeurantaServerMock() {
        super();
        addHandler("/seuranta-service/resources/seuranta/kuormantasaus/laskenta/facecafe-testbeef",
                 httpExchange -> {
            if(laskentaQueue.isEmpty()) {
                httpExchange.sendResponseHeaders(204, -1);
                httpExchange.getResponseBody().close();
            } else {
                String resp = new Gson().toJson(laskentaQueue.poll());
                httpExchange.sendResponseHeaders(200, resp.length());
                httpExchange.getResponseBody().write(resp.getBytes());
                httpExchange.getResponseBody().close();
            }
        });
        addHandler("/seuranta-service/resources/seuranta/laskenta/otaSeuraavaLaskentaTyonAlle", httpExchange -> {
            if(laskentaQueue.isEmpty()) {
                httpExchange.sendResponseHeaders(204, -1);
                httpExchange.getResponseBody().close();
            } else {
                String resp = laskentaQueue.peek().getUuid();
                httpExchange.sendResponseHeaders(200, resp.length());
                httpExchange.getResponseBody().write(resp.getBytes());
                httpExchange.getResponseBody().close();
            }
        });
        addHandler("/seuranta-service/resources/seuranta/kuormantasaus/laskenta/HAKUOID1/tyyppi/HAKU", httpExchange -> {
            HakukohdeDto h = new HakukohdeDto(HAKUKOHDE1, TARJOAJA1);
            LaskentaDto l = new LaskentaDto(UUID1, HAKU1, 0,
                    LaskentaTila.ALOITTAMATTA, LaskentaTyyppi.HAKU,
                    Arrays.asList(h),
                    false,
                    null,
                    false);
            laskentaQueue.add(l);
            httpExchange.sendResponseHeaders(200, UUID1.length());
            httpExchange.getResponseBody().write(UUID1.getBytes());
            httpExchange.getResponseBody().close();

        });
    }

}
