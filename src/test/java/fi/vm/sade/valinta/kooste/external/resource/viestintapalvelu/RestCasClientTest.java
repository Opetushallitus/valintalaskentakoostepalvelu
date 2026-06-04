package fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import fi.vm.sade.javautils.nio.cas.CasClient;
import fi.vm.sade.javautils.nio.cas.CasClientBuilder;
import fi.vm.sade.javautils.nio.cas.CasConfig;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.asynchttpclient.cookie.CookieStore;
import org.asynchttpclient.uri.Uri;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RestCasClientTest {

  private static final String CAS_URL = "https://testivirkailija.testiopintopolku.fi/cas";
  private static final String SERVICE_URL =
      "https://testivirkailija.testiopintopolku.fi/suoritusrekisteri";

  private AsyncHttpClient mockHttpClient;

  @BeforeEach
  void setupMockHttpClient() {
    mockHttpClient = mock(AsyncHttpClient.class);
    final AsyncHttpClientConfig mockConfig = mock(AsyncHttpClientConfig.class);
    final CookieStore mockCookieStore = mock(CookieStore.class);
    when(mockHttpClient.getConfig()).thenReturn(mockConfig);
    when(mockConfig.getCookieStore()).thenReturn(mockCookieStore);
  }

  @Test
  void kunSetNumberOfRetriesNolla_http302EiUusiSessiotaVaanHeittaaPoikkeuksen() throws Exception {
    setupCasFlow();
    final ListenableFuture<Response> service302 = withListenableFuture(mock302());
    when(mockHttpClient.executeRequest(matchingUrl(this::isServiceRequest))).thenReturn(service302);

    final CasConfig config = casConfigBuilder().setNumberOfRetries(0).build();
    final RestCasClient client = restClientWith(config);

    final ExecutionException thrown =
        assertThrows(
            ExecutionException.class,
            () -> client.get(SERVICE_URL + "/rest/v1/oppijat", Collections.emptyMap(), 5000).get());

    assertInstanceOf(RestCasClient.RestCasClientException.class, thrown.getCause());
    assertEquals(
        302,
        ((RestCasClient.RestCasClientException) thrown.getCause()).getResponse().getStatusCode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void oletusNumberOfRetries_http302JalkeenSessioUusitaanJaKutsuOnnistuu() throws Exception {
    setupCasFlow();
    final ListenableFuture<Response> service302 = withListenableFuture(mock302());
    final ListenableFuture<Response> service200 = withListenableFuture(mock200("[]"));
    when(mockHttpClient.executeRequest(matchingUrl(this::isServiceRequest)))
        .thenReturn(service302, service200);

    final CasConfig config = casConfigBuilder().build(); // numberOfRetries=1 oletuksena
    final RestCasClient client = restClientWith(config);

    final Response result =
        client.get(SERVICE_URL + "/rest/v1/oppijat", Collections.emptyMap(), 5000).get();

    assertEquals(200, result.getStatusCode());
  }

  @Test
  void http200VastausOnnistuu() throws Exception {
    setupCasFlow();
    final ListenableFuture<Response> service200 = withListenableFuture(mock200("{}"));
    when(mockHttpClient.executeRequest(matchingUrl(this::isServiceRequest))).thenReturn(service200);

    final RestCasClient client = restClientWith(casConfigBuilder().build());

    final Response result =
        client.get(SERVICE_URL + "/rest/v1/oppijat", Collections.emptyMap(), 5000).get();
    assertEquals(200, result.getStatusCode());
  }

  @Test
  void http500VastausHeittaaRestCasClientExceptionin() throws Exception {
    setupCasFlow();
    final Response response500 = mock(Response.class);
    when(response500.getStatusCode()).thenReturn(500);
    when(response500.getResponseBody()).thenReturn("Internal Server Error");
    final ListenableFuture<Response> service500 = withListenableFuture(response500);
    when(mockHttpClient.executeRequest(matchingUrl(this::isServiceRequest))).thenReturn(service500);

    final RestCasClient client = restClientWith(casConfigBuilder().build());

    final ExecutionException thrown =
        assertThrows(
            ExecutionException.class,
            () -> client.get(SERVICE_URL + "/rest/v1/oppijat", Collections.emptyMap(), 5000).get());

    assertInstanceOf(RestCasClient.RestCasClientException.class, thrown.getCause());
    assertEquals(
        500,
        ((RestCasClient.RestCasClientException) thrown.getCause()).getResponse().getStatusCode());
  }

  private void setupCasFlow() throws Exception {
    final Response tgt = mock(Response.class);
    when(tgt.getStatusCode()).thenReturn(201);
    when(tgt.getHeader("Location")).thenReturn(CAS_URL + "/v1/tickets/TGT-test");

    final Response st = mock(Response.class);
    when(st.getStatusCode()).thenReturn(200);
    when(st.getResponseBody()).thenReturn("ST-test");
    when(st.getUri()).thenReturn(Uri.create(CAS_URL + "/v1/tickets/TGT-test"));

    final Response session = mock(Response.class);
    when(session.getStatusCode()).thenReturn(200);
    when(session.getCookies()).thenReturn(List.of(new DefaultCookie("JSESSIONID", "session-id")));

    final ListenableFuture<Response> tgtFuture = withListenableFuture(tgt);
    final ListenableFuture<Response> stFuture = withListenableFuture(st);
    final ListenableFuture<Response> sessionFuture = withListenableFuture(session);
    when(mockHttpClient.executeRequest(matchingUrl(this::isTgtRequest))).thenReturn(tgtFuture);
    when(mockHttpClient.executeRequest(matchingUrl(this::isStRequest))).thenReturn(stFuture);
    when(mockHttpClient.executeRequest(matchingUrl(this::isSessionRequest)))
        .thenReturn(sessionFuture);
  }

  private boolean isTgtRequest(final String url) {
    return url.equals(CAS_URL + "/v1/tickets");
  }

  private boolean isStRequest(final String url) {
    return url.startsWith(CAS_URL + "/v1/tickets/TGT");
  }

  private boolean isSessionRequest(final String url) {
    return url.startsWith(SERVICE_URL) && url.contains("ticket=");
  }

  private boolean isServiceRequest(final String url) {
    return url.startsWith(SERVICE_URL) && !url.contains("ticket=");
  }

  private CasConfig.CasConfigBuilder casConfigBuilder() {
    return new CasConfig.CasConfigBuilder(
            "user", "pass", CAS_URL, SERVICE_URL, "CSRF", "caller-id", "")
        .setJsessionName("JSESSIONID");
  }

  private RestCasClient restClientWith(final CasConfig config) {
    final CasClient casClient =
        CasClientBuilder.buildFromConfigAndHttpClient(config, mockHttpClient);
    return new RestCasClient(
        request ->
            casClient.executeAndRetryWithCleanSessionOnStatusCodes(request, Set.of(302, 401)));
  }

  private static Request matchingUrl(final Predicate<String> urlPredicate) {
    return argThat(r -> r != null && urlPredicate.test(r.getUrl()));
  }

  @SuppressWarnings("unchecked")
  private static ListenableFuture<Response> withListenableFuture(final Response response)
      throws Exception {
    final ListenableFuture<Response> future = mock(ListenableFuture.class);
    when(future.get()).thenReturn(response);
    when(future.toCompletableFuture()).thenReturn(CompletableFuture.completedFuture(response));
    return future;
  }

  private static Response mock302() {
    final Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(302);
    when(response.getResponseBody()).thenReturn("");
    return response;
  }

  private static Response mock200(final String body) {
    final Response response = mock(Response.class);
    when(response.getStatusCode()).thenReturn(200);
    when(response.getResponseBody()).thenReturn(body);
    return response;
  }
}
