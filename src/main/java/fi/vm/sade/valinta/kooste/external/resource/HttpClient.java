package fi.vm.sade.valinta.kooste.external.resource;

import com.google.gson.Gson;

import fi.vm.sade.javautils.cas.ApplicationSession;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class HttpClient {
    private static final String CALLER_ID = "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu";
    private static final String CSRF_VALUE = "CSRF";

    private final java.net.http.HttpClient client;
    private final ApplicationSession session;
    private final Gson gson;

    public HttpClient(java.net.http.HttpClient client, ApplicationSession session, Gson gson) {
        this.client = client;
        this.session = session;
        this.gson = gson;
    }

    public <O> CompletableFuture<O> getJson(String url, Duration timeout, Type outputType) {
        HttpRequest request = buildWithCallerIdAndCsrfHeaders(HttpRequest.newBuilder(URI.create(url)))
                .header("Accept", "application/json")
                .GET()
                .timeout(timeout)
                .build();
        return this.makeRequest(request).thenApply(response -> this.parseJson(response, outputType));
    }

    public CompletableFuture<HttpResponse<InputStream>> getResponse(String url,
                                                                    Duration timeout,
                                                                    Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomisation) {
        HttpRequest request = requestCustomisation.apply(
                buildWithCallerIdAndCsrfHeaders(HttpRequest.newBuilder(URI.create(url)))
                        .header("Accept", "*/*")
                        .GET()
                        .timeout(timeout))
                .build();
        return this.makeRequest(request);
    }

    public <O> CompletableFuture<O> post(String url,
                                         Duration timeout,
                                         HttpRequest.BodyPublisher bodyPublisher,
                                         Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomisation,
                                         Function<HttpResponse<InputStream>, O> parseResponse) {
        HttpRequest request = requestCustomisation.apply(
                buildWithCallerIdAndCsrfHeaders(HttpRequest.newBuilder(URI.create(url)))
                .POST(bodyPublisher)
                .timeout(timeout))
            .build();
        return this.makeRequest(request).thenApply(parseResponse);
    }

    public <I, O> CompletableFuture<O> postJson(String url,
                                                Duration timeout,
                                                I body,
                                                Type inputType,
                                                Type outputType,
                                                Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomisation) {

        Function<HttpRequest.Builder, HttpRequest.Builder> addJsonHeaders = builder ->
                buildWithCallerIdAndCsrfHeaders(builder)
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/json");

        return this.post(
            url,
            timeout,
            createJsonBodyPublisher(body, inputType),
            addJsonHeaders.andThen(requestCustomisation),
            response -> this.parseJson(response, outputType));
    }

    public <I, O> CompletableFuture<O> postJson(String url, Duration timeout, I body, Type inputType, Type outputType) {
        return postJson(url, timeout, body, inputType, outputType, Function.identity());
    }

    public <I, O> CompletableFuture<O> putJson(String url,
                                               Duration timeout,
                                               I body,
                                               Type inputType,
                                               Type outputType,
                                               Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomisation) {
        HttpRequest request = requestCustomisation.apply(
                buildWithCallerIdAndCsrfHeaders(HttpRequest.newBuilder(URI.create(url)))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(this.gson.toJson(body, inputType), StandardCharsets.UTF_8))
                .timeout(timeout))
            .build();
        return this.makeRequest(request).thenApply(response -> this.parseJson(response, outputType));
    }
    public CompletableFuture<HttpResponse<InputStream>> putResponse(String url,
                                                                    Duration timeout,
                                                                    byte[] body,
                                                                    String contentType,
                                                                    Function<HttpRequest.Builder, HttpRequest.Builder> requestCustomisation) {
        HttpRequest request = requestCustomisation.apply(
                buildWithCallerIdAndCsrfHeaders(HttpRequest.newBuilder(URI.create(url)))
                .header("Accept", "*/*")
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .timeout(timeout))
            .build();
        return this.makeRequest(request);
    }

    public CompletableFuture<HttpResponse<InputStream>> putResponse(String url, Duration timeout, byte[] body, String contentType) {
        return putResponse(url, timeout, body, contentType, Function.identity());
    }

    public CompletableFuture<String> delete(String url, Duration timeout) {
        HttpRequest request = buildWithCallerIdAndCsrfHeaders(HttpRequest.newBuilder(URI.create(url)))
                .header("Accept", "*/*")
                .DELETE()
                .timeout(timeout)
                .build();
        return this.makeRequest(request).thenApply(this::parseTxt);
    }

    private HttpRequest.Builder buildWithCallerIdAndCsrfHeaders(HttpRequest.Builder builder) {
        return builder.header("Caller-Id", CALLER_ID)
                .header("CSRF", CSRF_VALUE)
                .header("Cookie", String.format("CSRF=%s;", CSRF_VALUE));
    }

    private CompletableFuture<HttpResponse<InputStream>> makeRequest(HttpRequest request) {
        if (this.session == null) {
            return this.sendAsync(request);
        }
        return this.session.getSessionToken()
                .thenComposeAsync(sessionToken -> this.sendAsync(request)
                        .thenComposeAsync(response -> {
                            if (isUnauthenticated(response) || isRedirectToCas(response)) {
                                this.session.invalidateSession(sessionToken);
                                return this.session.getSessionToken().thenComposeAsync(s -> this.sendAsync(request));
                            }
                            return CompletableFuture.completedFuture(response);
                        }));
    }

    private CompletableFuture<HttpResponse<InputStream>> sendAsync(HttpRequest request) {
        return this.client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .handle((response, e) -> {
                    if (e != null) {
                        throw new IllegalStateException(request.uri().toString(), e);
                    }
                    return response;
                });
    }

    public <O> O parseJson(HttpResponse<InputStream> response, Type outputType) {
        if (isSuccess(response)) {
            if (!isJson(response)) {
                throw new IllegalStateException(String.format(
                        "%s %d: Unexpected Content-Type %s",
                        response.uri().toString(),
                        response.statusCode(),
                        String.join(", ", response.headers().allValues("Content-Type"))
                ));
            }
            try (Reader r = new InputStreamReader(response.body())) {
                return this.gson.fromJson(r, outputType);
            } catch (IOException e) {
                throw new IllegalStateException(String.format(
                        "%s %d: Failed to parse JSON response",
                        response.uri().toString(),
                        response.statusCode()
                ), e);
            }
        }
        try (InputStream is = response.body()) {
            throw new IllegalStateException(String.format(
                    "%s %d: %s",
                    response.uri().toString(),
                    response.statusCode(),
                    new String(is.readAllBytes(), Charset.forName("UTF-8"))
            ));
        } catch (IOException e) {
            throw new IllegalStateException(String.format(
                    "%s %d: Failed to parse error response",
                    response.uri().toString(),
                    response.statusCode()
            ), e);
        }
    }

    public String parseTxt(HttpResponse<InputStream> response) {
        if (isSuccess(response)) {
            try (Reader r = new InputStreamReader(response.body())) {
                return IOUtils.toString(r);
            } catch (IOException e) {
                throw new IllegalStateException(String.format(
                    "%s %d: Failed to parse String response",
                    response.uri().toString(),
                    response.statusCode()
                ), e);
            }
        }
        try (InputStream is = response.body()) {
            throw new IllegalStateException(String.format(
                "%s %d: %s",
                response.uri().toString(),
                response.statusCode(),
                new String(is.readAllBytes(), StandardCharsets.UTF_8)
            ));
        } catch (IOException e) {
            throw new IllegalStateException(String.format(
                "%s %d: Failed to parse error response",
                response.uri().toString(),
                response.statusCode()
            ), e);
        }
    }

    public <I> HttpRequest.BodyPublisher createJsonBodyPublisher(I body, Type inputType) {
        return HttpRequest.BodyPublishers.ofString(this.gson.toJson(body, inputType), StandardCharsets.UTF_8);
    }

    private static boolean isJson(HttpResponse<InputStream> response) {
        return response.headers().allValues("Content-Type").stream().anyMatch(contentType -> contentType.contains("application/json"));
    }

    private static boolean isSuccess(HttpResponse<InputStream> response) {
        return response.statusCode() < 300;
    }

    private static boolean isRedirectToCas(HttpResponse<?> response) {
        return response.statusCode() == 302 && response.headers().allValues("Location").stream().anyMatch(location -> location.contains("/cas/login"));
    }

    private static boolean isUnauthenticated(HttpResponse<?> response) {
        return response.statusCode() == 401;
    }
}
