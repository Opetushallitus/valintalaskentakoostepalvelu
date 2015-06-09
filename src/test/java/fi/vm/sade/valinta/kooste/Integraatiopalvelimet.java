package fi.vm.sade.valinta.kooste;


import com.google.gson.Gson;
import fi.vm.sade.integrationtest.util.PortChecker;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ConnectionOptions;
import org.mockserver.model.Header;
import org.mockserver.model.Parameter;

import static javax.ws.rs.HttpMethod.*;

import static org.mockserver.model.Body.*;
import static org.mockserver.matchers.Times.*;
import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpForward.*;
import static org.mockserver.model.HttpResponse.*;
import static org.mockserver.model.ConnectionOptions.*;
import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import java.net.InetAddress;
import java.net.URL;

/**
 * @author Jussi Jartamo
 *
 * http://www.mock-server.com/
 */
public class Integraatiopalvelimet {

    public static ClientAndServerWithHost mockServer =
            new ClientAndServerWithHost(PortChecker.findFreeLocalPort());

    public static void mockGetForward(String path, int port) {
        mockServer
                .when(
                        request()
                                .withMethod(GET)
                                .withPath(path)
                )
                .forward(
                        forward()
                                .withHost(mockServer.getHost())
                                .withPort(port));

    }
    public static void mockPostForward(String path, int port) {
        mockServer
                .when(
                        request()
                                .withMethod(POST)
                                .withPath(path)
                )
                .forward(
                        forward()
                                .withHost(mockServer.getHost())
                                .withPort(port));

    }
    public static void mockGetToReturnJson(String p, Object r) {
        mockServer
                .when(
                        request()
                                .withMethod(GET)
                                .withPath(p)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withHeaders(
                                        new Header("Content-Type", "application/json; charset=utf-8")
                                )
                                .withBody(new Gson().toJson(r))

                );

    }
    public static void mockPostToReturnString(String p, String r) {
        mockServer
                .when(
                        request()
                                .withMethod(POST)
                                .withPath(p)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(r)

                );

    }
    public static class ClientAndServerWithHost extends ClientAndServer {
        public ClientAndServerWithHost(int port){
            super(port);
        }

        public String getHost() {
            return super.host;
        }
    }
}
