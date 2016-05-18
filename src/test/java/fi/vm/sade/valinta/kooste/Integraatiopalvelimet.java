package fi.vm.sade.valinta.kooste;


import com.google.gson.Gson;
import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.server.MockServer;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;

import java.util.concurrent.TimeUnit;

import static javax.ws.rs.HttpMethod.*;

import static org.mockserver.model.HttpRequest.*;
import static org.mockserver.model.HttpForward.*;
import static org.mockserver.model.HttpResponse.*;

/**
 * @author Jussi Jartamo
 *
 * http://www.mock-server.com/
 */
public class Integraatiopalvelimet {

    public static ClientAndServerWithHost mockServer = new ClientAndServerWithHost(PortChecker.findFreeLocalPort());
    private static HttpResource httpResource = new HttpResource("");
    static {
        ConfigurationProperties.maxSocketTimeout(TimeUnit.SECONDS.toMillis(5));
    }
    public static void mockForward(String method, MockServer server) {
        server.getPaths().forEach(
                p -> {
                    mockForward(method, p, server.getPort());
                }
        );
    }
    public static void mockForward(MockServer server) {
        server.getPaths().forEach(
                p -> {
                    mockForward(GET, p, server.getPort());
                    mockForward(POST, p, server.getPort());
                    mockForward(PUT, p, server.getPort());
                }
        );
    }
    public static void mockForward(String method, String path, int port) {
        mockServer
                .when(
                        request()
                                .withMethod(method)
                                .withPath(path)
                )
                .forward(
                        forward()
                                .withHost(mockServer.getHost())
                                .withPort(port));
    }
    private static void mockToReturnValue(String method, String p, String r) {
        mockServer
                .when(
                        request()
                                .withMethod(method)
                                .withPath(p)
                )
                .respond(
                        response()
                                .withStatusCode(200)
                                .withBody(r)

                );
    }
    public static void mockToNoContent(String method, String p) {
        mockServer
                .when(
                        request()
                                .withMethod(method)
                                .withPath(p)
                )
                .respond(
                        response()
                                .withStatusCode(204)

                );
    }
    public static void mockToNotFound(String method, String p) {
        mockServer
                .when(
                        request()
                                .withMethod(method)
                                .withPath(p)
                )
                .respond(
                        response()
                                .withStatusCode(404)

                );
    }
    public static void mockToAccept(String method, String p) {
        mockServer
                .when(
                        request()
                                .withMethod(method)
                                .withPath(p)
                )
                .respond(
                        response()
                                .withStatusCode(200)

                );
    }
    public static void mockToReturnJson(String method, String p, Object r) {
        String s;
        mockToReturnValue(method, p, s = httpResource.gson().toJson(r));
        System.err.println(s);
    }
    public static void mockToReturnString(String method, String p, String r) {
        mockToReturnValue(method, p, r);
    }

    public static class ClientAndServerWithHost extends ClientAndServer {
        public ClientAndServerWithHost(int port){
            super(port);
        }

        public String getHost() {
            return super.host;
        }

        public String getUrl() {
            return new StringBuilder("http://").append(super.host).append(":").append(port).toString();
        }
    }
}
