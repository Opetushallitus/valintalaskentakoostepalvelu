package fi.vm.sade.valinta.kooste.server;

import static fi.vm.sade.valinta.sharedutils.http.HttpResource.CALLER_ID;
import com.google.common.collect.Lists;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import fi.vm.sade.integrationtest.util.PortChecker;
import org.hamcrest.Matchers;
import org.junit.Assert;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author Jussi Jartamo
 */
public class MockServer {

    private final HttpServer httpServer;
    private final List<String> paths = Lists.newArrayList();

    public MockServer() {
        this.httpServer = startServer();
    }

    private HttpServer startServer() {
        boolean succeededToStart = false;
        int maxAttempts = 10;
        int numberOfAttempt = 0;
        HttpServer server = null;
        while (++numberOfAttempt <= maxAttempts && !succeededToStart) {
            try {
                server = HttpServer.create(new InetSocketAddress(PortChecker.findFreeLocalPort()), 0);
                server.setExecutor(null);
                server.start();
                succeededToStart = true;
            } catch (Exception e) {
                System.err.println(getClass().getName() + " WARNING : Could not start server on attempt " +
                    numberOfAttempt + "/" + maxAttempts + " , exception was:");
                e.printStackTrace();
            }
        }
        if (!succeededToStart) {
            throw new RuntimeException("Could not start mock server with " + maxAttempts + "attempts");
        }
        return server;
    }

    public int getPort() {
        return httpServer.getAddress().getPort();
    }

    public MockServer addHandler(String path, HttpHandler handler) {
        paths.add(path);
        this.httpServer.createContext(path, wrapWithCallerIdAssertion(handler));
        return this;
    }

    private HttpHandler wrapWithCallerIdAssertion(HttpHandler handler) {
        return exchange -> {
            try {
                Headers requestHeaders = exchange.getRequestHeaders();
                List<String> callerIdHeader = requestHeaders.get(CALLER_ID);
                Assert.assertThat("Expected '" + CALLER_ID + "' header in " + requestHeaders.entrySet(),
                    callerIdHeader, Matchers.contains("1.2.246.562.10.00000000001.valintalaskentakoostepalvelu"));
                handler.handle(exchange);
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            }
        };
    }

    public List<String> getPaths() {
        return paths;
    }
}
