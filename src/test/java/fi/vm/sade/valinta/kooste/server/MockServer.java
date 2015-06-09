package fi.vm.sade.valinta.kooste.server;

import com.google.common.collect.Lists;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fi.vm.sade.integrationtest.util.PortChecker;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Jussi Jartamo
 */
public class MockServer {

    private final HttpServer httpServer;
    private final List<String> paths = Lists.newArrayList();

    public MockServer() {
        try {
            this.httpServer = HttpServer.create(new InetSocketAddress(PortChecker.findFreeLocalPort()), 0);
            httpServer.setExecutor(null);
            httpServer.start();
        } catch(Throwable t) {
            throw new RuntimeException(t);
        }
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
