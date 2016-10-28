package fi.vm.sade.valinta.kooste.server;

import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import fi.vm.sade.integrationtest.util.PortChecker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * @author Jussi Jartamo
 */
public class MockServer {

    private final HttpServer httpServer;
    private final List<String> paths = Lists.newArrayList();

    public MockServer() {
        try {
            this.httpServer = startServer();
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private HttpServer startServer() throws IOException {
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

    public MockServer addHandler(String path, HttpHandler exchange) {
        paths.add(path);
        this.httpServer.createContext(path, exchange);
        return this;
    }

    public List<String> getPaths() {
        return paths;
    }
}
