package fi.vm.sade.valinta.kooste.server;

import static fi.vm.sade.valinta.sharedutils.http.HttpResource.CALLER_ID;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.Lists;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fi.vm.sade.integrationtest.util.PortChecker;
import java.net.InetSocketAddress;
import java.util.List;
import org.hamcrest.Matchers;

/**
 * @author Jussi Jartamo
 */
public class MockServer {

  private final HttpServer httpServer;
  private final List<String> paths = Lists.newArrayList();
  private int freeLocalPort;

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
        freeLocalPort = PortChecker.findFreeLocalPort();
        server = HttpServer.create(new InetSocketAddress(freeLocalPort), 0);
        server.setExecutor(null);
        server.start();
        succeededToStart = true;
        System.out.println(
            "Started " + getClass().getName() + " listening in port " + freeLocalPort);
      } catch (Exception e) {
        System.err.println(
            getClass().getName()
                + " WARNING : Could not start server on attempt "
                + numberOfAttempt
                + "/"
                + maxAttempts
                + " , exception was:");
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
    System.out.println(
        getClass().getName()
            + " listening in port "
            + freeLocalPort
            + " added handler for path '"
            + path
            + "'");
    paths.add(path);
    this.httpServer.createContext(path, wrapWithCallerIdAssertion(handler));
    return this;
  }

  private HttpHandler wrapWithCallerIdAssertion(HttpHandler handler) {
    return exchange -> {
      try {
        Headers requestHeaders = exchange.getRequestHeaders();
        List<String> callerIdHeader = requestHeaders.get(CALLER_ID);
        assertThat(
            "Expected '" + CALLER_ID + "' header in " + requestHeaders.entrySet(),
            callerIdHeader,
            Matchers.contains("1.2.246.562.10.00000000001.valintalaskentakoostepalvelu"));
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
