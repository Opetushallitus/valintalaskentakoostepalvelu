package fi.vm.sade.valinta.kooste;

import static javax.ws.rs.HttpMethod.*;
import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.google.gson.*;
import fi.vm.sade.integrationtest.util.PortChecker;
import fi.vm.sade.valinta.kooste.server.MockServer;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Parameter;
import org.mockserver.model.RegexBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jussi Jartamo
 *     <p>http://www.mock-server.com/
 */
public class Integraatiopalvelimet {
  private static final Logger LOG = LoggerFactory.getLogger(Integraatiopalvelimet.class);

  public static ClientAndServerWithHost mockServer =
      new ClientAndServerWithHost(PortChecker.findFreeLocalPort());

  static {
    ConfigurationProperties.maxSocketTimeout(TimeUnit.SECONDS.toMillis(15));
  }

  public static void mockForward(String method, MockServer server) {
    server.getPaths().forEach(p -> mockForward(method, p, server.getPort()));
  }

  public static void mockForward(MockServer server) {
    server
        .getPaths()
        .forEach(
            p -> {
              mockForward(GET, p, server.getPort());
              mockForward(POST, p, server.getPort());
              mockForward(PUT, p, server.getPort());
            });
  }

  public static void mockForward(String method, String path, int port) {
    LOG.info(
        "mockForward: "
            + method
            + " to path '"
            + path
            + "' to "
            + mockServer.getHost()
            + ":"
            + port);
    mockServer
        .when(request().withMethod(method).withPath(path))
        .forward(forward().withHost(mockServer.getHost()).withPort(port));
  }

  private static void mockToReturnValue(String method, String p, String r) {
    mockServer
        .when(request().withMethod(method).withPath(p))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(
                    new org.mockserver.model.Header(
                        "Content-Type", "application/json; charset=utf-8"))
                .withBody(r));
  }

  private static void mockToReturnValueWithCookie(
      String method, String p, String r, String cookieName, String cookieValue) {
    mockServer
        .when(request().withMethod(method).withPath(p))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(
                    new org.mockserver.model.Header("Set-Cookie", cookieName + "=" + cookieValue),
                    new org.mockserver.model.Header(
                        "Content-Type", "application/json; charset=utf-8"))
                .withBody(r));
  }

  private static void mockToReturnValueWithParams(
      String method, String p, String r, List<Parameter> parameters) {
    mockServer
        .when(request().withMethod(method).withPath(p).withQueryStringParameters(parameters))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(
                    new org.mockserver.model.Header(
                        "Content-Type", "application/json; charset=utf-8"))
                .withBody(r));
  }

  private static void mockToReturnValueAndCheckBody(
      String method, String p, String r, String regex) {
    mockServer
        .when(request().withMethod(method).withPath(p).withBody(new RegexBody(regex)))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(
                    new org.mockserver.model.Header(
                        "Content-Type", "application/json; charset=utf-8"))
                .withBody(r));
  }

  private static void mockToReturnInputStreamValueAndHeaders(
      String method, String p, String n, byte[] r) {
    mockServer
        .when(request().withMethod(method).withPath(p))
        .respond(
            response()
                .withStatusCode(200)
                .withHeaders(
                    new org.mockserver.model.Header(
                        "Content-Disposition", "attachment; filename=\"" + n + "\""))
                .withBody(r));
  }

  public static void mockToNoContent(String method, String p) {
    mockServer
        .when(request().withMethod(method).withPath(p))
        .respond(response().withStatusCode(204));
  }

  public static void mockToNotFound(String method, String p) {
    mockServer
        .when(request().withMethod(method).withPath(p))
        .respond(response().withStatusCode(404));
  }

  public static void mockToInternalServerError(String method, String p) {
    mockServer
        .when(request().withMethod(method).withPath(p))
        .respond(response().withStatusCode(500));
  }

  public static void mockToAccept(String method, String p) {
    mockServer
        .when(request().withMethod(method).withPath(p))
        .respond(response().withStatusCode(200));
  }

  public static void mockToReturnJsonWithParams(
      String method, String p, Object r, Map<String, String> parameters) {
    String s;
    mockToReturnValueWithParams(
        method,
        p,
        s = gson().toJson(r),
        parameters.keySet().stream()
            .map(name -> new Parameter(name, parameters.get(name)))
            .collect(Collectors.toList()));
    LOG.debug(s);
  }

  public static void mockToReturnJson(String method, String p, Object r) {
    String s;
    mockToReturnValue(method, p, s = gson().toJson(r));
    LOG.debug(s);
  }

  public static void mockToReturnJsonWithCookie(
      String method, String p, Object r, String cookieName, String cookieValue) {
    String s;
    mockToReturnValueWithCookie(method, p, s = gson().toJson(r), cookieName, cookieValue);
    LOG.debug(s);
  }

  public static void mockToReturnJsonAndCheckBody(String method, String p, Object r, String regex) {
    String s;
    mockToReturnValueAndCheckBody(method, p, s = gson().toJson(r), regex);
    LOG.debug(s);
  }

  public static void mockToReturnInputStreamAndHeaders(
      String method, String p, String n, byte[] r) {
    mockToReturnInputStreamValueAndHeaders(method, p, n, r);
    LOG.debug(n);
  }

  public static void mockToReturnString(String method, String p, String r) {
    mockToReturnValue(method, p, r);
    LOG.debug(r);
  }

  public static class ClientAndServerWithHost extends ClientAndServer {
    private final int port;

    ClientAndServerWithHost(int port) {
      super(port);
      this.port = port;
    }

    String getHost() {
      return remoteAddress().getHostString();
    }

    public String getUrl() {
      return "http://" + getHost() + ":" + port;
    }
  }

  private static JsonSerializer<Date> dateJsonSerializer =
      (Date src, Type typeOfSrc, JsonSerializationContext context) ->
          src == null ? null : new JsonPrimitive(src.getTime());

  private static Gson gson =
      DateDeserializer.gsonBuilder().registerTypeAdapter(Date.class, dateJsonSerializer).create();

  public static Gson gson() {
    return gson;
  }
}
