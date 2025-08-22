package fi.vm.sade.valinta.kooste.external.resource.koski.impl;

import static fi.vm.sade.valinta.kooste.test.KoostepalveluTestingHttpUtil.createMockJsonResponse;
import static fi.vm.sade.valinta.kooste.test.KoostepalveluTestingHttpUtil.listenableFuture;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.google.gson.Gson;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.test.RequestMatcher;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.asynchttpclient.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class KoskiAsyncResourceImplTest {
  private final String username = "koostepalvelu2koskiUser";
  private final String password = "koostepalvelu2koskiPass";
  private final int maxOppijatPostSize = 1;
  private final String massaLuovutusUrl = "http://localhost/koski/would/be/here";
  private final String queryStatusTemplate =
      "fi/vm/sade/valinta/kooste/external/resource/koski/impl/KoskiAsyncResourceImplTest-querystatus-template.json";
  private final String queryResultTemplate =
      "fi/vm/sade/valinta/kooste/external/resource/koski/impl/KoskiAsyncResourceImplTest-result-template.json";
  private final String emptyResultTemplate =
      "fi/vm/sade/valinta/kooste/external/resource/koski/impl/KoskiAsyncResourceImplTest-noResult-template.json";
  private final String queryId1 = "1111-2222-3333-4444";
  private final String queryId2 = "2222-3333-4444-5555";
  private final List<String> oppijaNumerotToTest =
      new ArrayList<>(Arrays.asList("1.2.246.562.24.30338561184", "1.2.246.562.24.32656706483"));

  ListenableFuture<Response> startResponse1,
      startResponse2,
      pollResponse1,
      pollResponse2,
      readyResponse1,
      readyResponse2,
      result1,
      result2;

  private final AsyncHttpClient mockHttpClient = mock(AsyncHttpClient.class);

  private static class KoskiAsyncHttpClientTester extends KoskiAsyncHttpClient {
    public KoskiAsyncHttpClientTester(int nbrOfWorkers, Gson gson, AsyncHttpClient mockHttpClient) {
      super(nbrOfWorkers, gson);
      this.httpClient = mockHttpClient;
    }

    @Override
    protected AsyncHttpClient createHttpClient() {
      return this.httpClient;
    }
  }

  private final KoskiAsyncHttpClient koskiHttpClient =
      new KoskiAsyncHttpClientTester(
          1, DateDeserializer.gsonBuilder().create(), this.mockHttpClient);

  private final UrlConfiguration urlConfiguration = mock(UrlConfiguration.class);
  private final KoskiAsyncResourceImpl koskiAsyncResource =
      new KoskiAsyncResourceImpl(
          urlConfiguration, koskiHttpClient, username, password, maxOppijatPostSize, 8);

  @BeforeEach
  public void setUpTestdata() throws Exception {
    when(urlConfiguration.url("koski.massaluovutus.post")).thenReturn(massaLuovutusUrl);
    startResponse1 = statusResponse(queryId1, oppijaNumerotToTest.get(0), "pending");
    startResponse2 = statusResponse(queryId2, oppijaNumerotToTest.get(1), "pending");

    pollResponse1 = statusResponse(queryId1, oppijaNumerotToTest.get(0), "running");
    pollResponse2 = statusResponse(queryId2, oppijaNumerotToTest.get(1), "running");

    readyResponse1 = statusResponse(queryId1, oppijaNumerotToTest.get(0), "complete");
    readyResponse2 = statusResponse(queryId2, oppijaNumerotToTest.get(1), "complete");

    result1 =
        listenableFuture(
            createMockJsonResponse(queryResultTemplate, 200, oppijaNumerotToTest.get(0)));
    result2 =
        listenableFuture(
            createMockJsonResponse(queryResultTemplate, 200, oppijaNumerotToTest.get(1)));
  }

  @Test
  public void findKoskiOppijat() throws Exception {
    when(mockHttpClient.executeRequest(requestArg("POST", massaLuovutusUrl)))
        .thenReturn(startResponse1)
        .thenReturn(startResponse2);
    when(mockHttpClient.executeRequest(
            requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1))))
        .thenReturn(pollResponse1)
        .thenReturn(readyResponse1);
    when(mockHttpClient.executeRequest(
            requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId2))))
        .thenReturn(pollResponse2)
        .thenReturn(readyResponse2);
    when(mockHttpClient.executeRequest(
            requestArg(
                "GET",
                String.format(
                    "%s/%s/%s.json", massaLuovutusUrl, queryId1, oppijaNumerotToTest.get(0)))))
        .thenReturn(result1);
    when(mockHttpClient.executeRequest(
            requestArg(
                "GET",
                String.format(
                    "%s/%s/%s.json", massaLuovutusUrl, queryId2, oppijaNumerotToTest.get(1)))))
        .thenReturn(result2);

    Set<KoskiOppija> koskiOppijas =
        koskiAsyncResource.findKoskiOppijat(oppijaNumerotToTest, LocalDate.now()).get(10, SECONDS);

    verify(mockHttpClient, times(2)).executeRequest(requestArg("POST", massaLuovutusUrl));
    verify(mockHttpClient, times(2))
        .executeRequest(requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1)));
    verify(mockHttpClient, times(2))
        .executeRequest(requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId2)));
    verify(mockHttpClient)
        .executeRequest(
            requestArg(
                "GET",
                String.format(
                    "%s/%s/%s.json", massaLuovutusUrl, queryId1, oppijaNumerotToTest.get(0))));
    verify(mockHttpClient)
        .executeRequest(
            requestArg(
                "GET",
                String.format(
                    "%s/%s/%s.json", massaLuovutusUrl, queryId2, oppijaNumerotToTest.get(1))));
    verifyNoMoreInteractions(mockHttpClient);

    assertThat(koskiOppijas, hasSize(2));
    Iterator<KoskiOppija> iter = koskiOppijas.iterator();
    while (iter.hasNext()) {
      KoskiOppija oppija = iter.next();
      assertTrue(oppijaNumerotToTest.contains(oppija.getOppijaOid()));

      assertThat(
          oppija.getOpiskeluoikeudet().toString(),
          containsString("\"oid\":\"1.2.246.562.15.36706825456\""));
      assertThat(
          oppija.getOpiskeluoikeudet().toString(),
          containsString(
              "\"oppilaitosnumero\":{\"koodiarvo\":\"00000\",\"nimi\":{\"fi\":\"Tuntematon\",\"sv\":\"Okänd\",\"en\":\"Unknown\"}"));
      assertThat(
          oppija.getOpiskeluoikeudet().toString(),
          containsString("\"tyyppi\":{\"koodiarvo\":\"ammatillinenkoulutus\""));

      assertEquals(5, oppija.getOpiskeluoikeudet().size());
    }
  }

  @Test
  public void findKoskioppijat_no_oppijat_in_koski() throws Exception {
    when(mockHttpClient.executeRequest(requestArg("POST", massaLuovutusUrl)))
        .thenReturn(startResponse1)
        .thenReturn(startResponse2);
    ListenableFuture<Response> emptyResult1 =
        listenableFuture(
            createMockJsonResponse(
                emptyResultTemplate, 200, queryId1, oppijaNumerotToTest.get(0), "complete"));
    ListenableFuture<Response> emptyResult2 =
        listenableFuture(
            createMockJsonResponse(
                emptyResultTemplate, 200, queryId2, oppijaNumerotToTest.get(1), "complete"));
    when(mockHttpClient.executeRequest(
            requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1))))
        .thenReturn(emptyResult1);
    when(mockHttpClient.executeRequest(
            requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId2))))
        .thenReturn(emptyResult2);
    Set<KoskiOppija> koskiOppijas =
        koskiAsyncResource.findKoskiOppijat(oppijaNumerotToTest, LocalDate.now()).get(10, SECONDS);

    verify(mockHttpClient, times(2)).executeRequest(requestArg("POST", massaLuovutusUrl));
    verify(mockHttpClient)
        .executeRequest(requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1)));
    verify(mockHttpClient)
        .executeRequest(requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId2)));
    verifyNoMoreInteractions(mockHttpClient);

    assertTrue(koskiOppijas.isEmpty());
  }

  @Test
  public void findKoskioppijat_internalErrorFromQueryStart() throws Exception {
    ListenableFuture<Response> errorResponse =
        listenableFuture(
            createMockJsonResponse(
                queryStatusTemplate, 500, queryId1, oppijaNumerotToTest.get(0), "pending"));
    when(mockHttpClient.executeRequest(requestArg("POST", massaLuovutusUrl)))
        .thenReturn(errorResponse);

    try {
      koskiAsyncResource
          .findKoskiOppijat(singletonList(oppijaNumerotToTest.get(0)), LocalDate.now())
          .get(10, SECONDS);
      fail("No exception caught");
    } catch (ExecutionException ee) {
      assertInstanceOf(IllegalStateException.class, ee.getCause());
    }
    verify(mockHttpClient).executeRequest(requestArg("POST", massaLuovutusUrl));
    verifyNoMoreInteractions(mockHttpClient);
  }

  @Test
  public void findKoskioppijat_koskiErrorFromQueryStart() throws Exception {
    ListenableFuture<Response> errorResponse =
        listenableFuture(
            createMockJsonResponse(
                queryStatusTemplate, 200, queryId1, oppijaNumerotToTest.get(0), "failed"));
    when(mockHttpClient.executeRequest(requestArg("POST", massaLuovutusUrl)))
        .thenReturn(errorResponse);

    try {
      koskiAsyncResource
          .findKoskiOppijat(singletonList(oppijaNumerotToTest.get(0)), LocalDate.now())
          .get(10, SECONDS);
      fail("No exception caught");
    } catch (ExecutionException ee) {
      assertInstanceOf(RuntimeException.class, ee.getCause());
    }
    verify(mockHttpClient).executeRequest(requestArg("POST", massaLuovutusUrl));
    verifyNoMoreInteractions(mockHttpClient);
  }

  @Test
  public void findKoskioppijat_internalErrorFromQueryPoll() throws Exception {
    ListenableFuture<Response> errorResponse =
        listenableFuture(
            createMockJsonResponse(
                queryStatusTemplate, 500, queryId1, oppijaNumerotToTest.get(0), "pending"));
    when(mockHttpClient.executeRequest(requestArg("POST", massaLuovutusUrl)))
        .thenReturn(startResponse1);
    when(mockHttpClient.executeRequest(
            requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1))))
        .thenReturn(errorResponse);

    try {
      koskiAsyncResource
          .findKoskiOppijat(singletonList(oppijaNumerotToTest.get(0)), LocalDate.now())
          .get(10, SECONDS);
      fail("No exception caught");
    } catch (ExecutionException ee) {
      assertInstanceOf(IllegalStateException.class, ee.getCause());
    }
    verify(mockHttpClient).executeRequest(requestArg("POST", massaLuovutusUrl));
    verify(mockHttpClient)
        .executeRequest(requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1)));
    verifyNoMoreInteractions(mockHttpClient);
  }

  @Test
  public void findKoskioppijat_koskiErrorFromQueryPoll() throws Exception {
    ListenableFuture<Response> errorResponse =
        listenableFuture(
            createMockJsonResponse(
                queryStatusTemplate, 200, queryId1, oppijaNumerotToTest.get(0), "failed"));
    when(mockHttpClient.executeRequest(requestArg("POST", massaLuovutusUrl)))
        .thenReturn(startResponse1);
    when(mockHttpClient.executeRequest(
            requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1))))
        .thenReturn(errorResponse);

    try {
      koskiAsyncResource
          .findKoskiOppijat(singletonList(oppijaNumerotToTest.get(0)), LocalDate.now())
          .get(10, SECONDS);
      fail("No exception caught");
    } catch (ExecutionException ee) {
      assertInstanceOf(RuntimeException.class, ee.getCause());
    }
    verify(mockHttpClient).executeRequest(requestArg("POST", massaLuovutusUrl));
    verify(mockHttpClient)
        .executeRequest(requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1)));
    verifyNoMoreInteractions(mockHttpClient);
  }

  @Test
  public void findKoskioppijat_internalErrorFromGetResult() throws Exception {
    ListenableFuture<Response> errorResponse =
        listenableFuture(
            createMockJsonResponse(queryResultTemplate, 500, oppijaNumerotToTest.get(0)));
    when(mockHttpClient.executeRequest(requestArg("POST", massaLuovutusUrl)))
        .thenReturn(startResponse1);
    when(mockHttpClient.executeRequest(
            requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1))))
        .thenReturn(readyResponse1);
    when(mockHttpClient.executeRequest(
            requestArg(
                "GET",
                String.format(
                    "%s/%s/%s.json", massaLuovutusUrl, queryId1, oppijaNumerotToTest.get(0)))))
        .thenReturn(errorResponse);

    try {
      koskiAsyncResource
          .findKoskiOppijat(singletonList(oppijaNumerotToTest.get(0)), LocalDate.now())
          .get(10, SECONDS);
      fail("No exception caught");
    } catch (ExecutionException ee) {
      assertInstanceOf(IllegalStateException.class, ee.getCause());
    }
    verify(mockHttpClient).executeRequest(requestArg("POST", massaLuovutusUrl));
    verify(mockHttpClient)
        .executeRequest(requestArg("GET", String.format("%s/%s", massaLuovutusUrl, queryId1)));
    verify(mockHttpClient)
        .executeRequest(
            requestArg(
                "GET",
                String.format(
                    "%s/%s/%s.json", massaLuovutusUrl, queryId1, oppijaNumerotToTest.get(0))));
    verifyNoMoreInteractions(mockHttpClient);
  }

  private Request requestArg(String method, String url) {
    Request request = new RequestBuilder().setMethod(method).setUrl(url).build();
    return argThat(new RequestMatcher(request));
  }

  private ListenableFuture<Response> statusResponse(String queryId, String oppijaOid, String status)
      throws Exception {
    return listenableFuture(
        createMockJsonResponse(queryStatusTemplate, 200, queryId, oppijaOid, status));
  }
}
