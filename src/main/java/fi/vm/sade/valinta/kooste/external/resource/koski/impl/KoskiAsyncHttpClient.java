package fi.vm.sade.valinta.kooste.external.resource.koski.impl;

import static org.asynchttpclient.Dsl.asyncHttpClient;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.concurrent.*;
import org.asynchttpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KoskiAsyncHttpClient {
  private static final Logger LOG = LoggerFactory.getLogger(KoskiAsyncHttpClient.class);

  private static final String CALLER_ID = "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu";
  private static final String CSRF_VALUE = "CSRF";

  protected final Gson gson;
  protected AsyncHttpClient httpClient;

  protected final ExecutorService executorService;

  public KoskiAsyncHttpClient(int nbrOfWorkers, final Gson gson) {
    this.executorService = Executors.newFixedThreadPool(nbrOfWorkers);
    this.httpClient = createHttpClient();
    this.gson = gson;
  }

  protected AsyncHttpClient createHttpClient() {
    return asyncHttpClient(
        new DefaultAsyncHttpClientConfig.Builder()
            .setMaxRedirects(5)
            .setConnectTimeout(Duration.ofMillis(10 * 1000))
            .build());
  }

  public <T> CompletableFuture<T> postJson(
      final String url,
      final Object body,
      final Type outputType,
      final Map<String, String> additionalHeaders,
      final int requestTimeoutInSeconds) {
    final Request request =
        requestWithHeaders(url, "POST", requestTimeoutInSeconds, additionalHeaders)
            .setBody(this.gson.toJson(body))
            .build();
    return CompletableFuture.supplyAsync(
        () ->
            this.httpClient
                .executeRequest(request)
                .toCompletableFuture()
                .<T>thenApply(r -> parseResponse(r, outputType))
                .join(),
        executorService);
  }

  public <T> CompletableFuture<T> getJson(
      final String url,
      final Type outputType,
      final Map<String, String> additionalHeaders,
      final int requestTimeoutInSeconds,
      boolean allowRedirect,
      final Realm realm) {
    RequestBuilder builder =
        newGetRequestBuilder(url, additionalHeaders, requestTimeoutInSeconds)
            .setFollowRedirect(allowRedirect);
    builder = realm != null ? builder.setRealm(realm) : builder;
    final Request request = builder.build();
    return CompletableFuture.supplyAsync(
        () ->
            this.httpClient
                .executeRequest(request)
                .toCompletableFuture()
                .<T>thenApply(r -> parseResponse(r, outputType))
                .join(),
        executorService);
  }

  public <T> CompletableFuture<T> getJsonAfterDelay(
      final String url,
      final Type outputType,
      final Duration delay,
      final Map<String, String> additionalHeaders,
      final int requestTimeoutInSeconds) {
    Executor delayedExecutor =
        CompletableFuture.delayedExecutor(delay.getSeconds(), TimeUnit.SECONDS, executorService);
    return CompletableFuture.supplyAsync(
        () ->
            this.httpClient
                .executeRequest(
                    newGetRequestBuilder(url, additionalHeaders, requestTimeoutInSeconds).build())
                .toCompletableFuture()
                .<T>thenApply(r -> parseResponse(r, outputType))
                .join(),
        delayedExecutor);
  }

  private static RequestBuilder requestWithHeaders(
      final String url,
      final String method,
      int timeoutInSeconds,
      final Map<String, String> additionalHeaders) {
    final RequestBuilder requestBuilder =
        new RequestBuilder()
            .setUrl(url)
            .setMethod(method)
            .setRequestTimeout(Duration.ofMillis(timeoutInSeconds * 1000))
            .setReadTimeout(Duration.ofMillis(timeoutInSeconds * 1000))
            .addHeader("Caller-Id", CALLER_ID)
            .addHeader("CSRF", CSRF_VALUE)
            .addHeader("Cookie", String.format("CSRF=%s;", CSRF_VALUE))
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json");

    for (Map.Entry<String, String> header : additionalHeaders.entrySet()) {
      requestBuilder.setHeader(header.getKey(), header.getValue());
    }
    return requestBuilder;
  }

  private RequestBuilder newGetRequestBuilder(
      final String url,
      final Map<String, String> additionalHeaders,
      final int requestTimeoutInSeconds) {
    return requestWithHeaders(url, "GET", requestTimeoutInSeconds, additionalHeaders);
  }

  private <O> O parseResponse(Response response, Type outputType) {
    if (response.getStatusCode() >= 300) {
      String errorMsg =
          String.format(
              "%s %d: %s",
              response.getUri().toString(), response.getStatusCode(), response.getResponseBody());

      LOG.error(errorMsg);
      throw new IllegalStateException(errorMsg);
    }
    try {
      return this.gson.fromJson(response.getResponseBody(), outputType);
    } catch (JsonSyntaxException jse) {
      String errorMsg =
          String.format(
              "%s %d: Failed to parse JSON response; ",
              response.getUri().toString(), response.getStatusCode());
      LOG.error(errorMsg, jse);
      throw new IllegalStateException(errorMsg, jse);
    }
  }
}
