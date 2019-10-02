package fi.vm.sade.valinta.kooste.external.resource;

import com.google.gson.Gson;
import fi.vm.sade.javautils.cas.ApplicationSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class HttpClient {
    private static final String CALLER_ID = "1.2.246.562.10.00000000001.valintalaskentakoostepalvelu";

    private final java.net.http.HttpClient client;
    private final ApplicationSession session;
    private final Gson gson;

    public HttpClient(java.net.http.HttpClient client, ApplicationSession session, Gson gson) {
        this.client = client;
        this.session = session;
        this.gson = gson;
    }

    public <O> CompletableFuture<O> getJson(String url, Duration timeout, Type outputType) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Caller-Id", CALLER_ID)
                .header("Accept", "application/json")
                .GET()
                .timeout(timeout)
                .build();
        return this.makeRequest(request).thenApply(response -> this.parseJson(response, outputType));
    }

    public CompletableFuture<HttpResponse<InputStream>> getResponse(String url, Duration timeout) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Caller-Id", CALLER_ID)
                .header("Accept", "*/*")
                .GET()
                .timeout(timeout)
                .build();
        return this.makeRequest(request);
    }

    public <I, O> CompletableFuture<O> postJson(String url, Duration timeout, I body, Type inputType, Type outputType) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Caller-Id", CALLER_ID)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(body, inputType), Charset.forName("UTF-8")))
                .timeout(timeout)
                .build();
        return this.makeRequest(request).thenApply(response -> this.parseJson(response, outputType));
    }

    public CompletableFuture<HttpResponse<InputStream>> putResponse(String url, Duration timeout, byte[] body, String contentType) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Caller-Id", CALLER_ID)
                .header("Accept", "*/*")
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .timeout(timeout)
                .build();
        return this.makeRequest(request);
    }

    private CompletableFuture<HttpResponse<InputStream>> makeRequest(HttpRequest request) {
        if (this.session == null) {
            return this.client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream());
        }
        return this.session.getSessionToken()
                .thenCompose(sessionToken -> this.client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                        .thenCompose(response -> {
                            if (isUnauthenticated(response) || isRedirectToCas(response)) {
                                this.session.invalidateSession(sessionToken);
                                return this.session.getSessionToken()
                                        .thenCompose(s -> this.client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()));
                            }
                            return CompletableFuture.completedFuture(response);
                        }));
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
                throw new UncheckedIOException(e);
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
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isJson(HttpResponse<InputStream> response) {
        return response.headers().allValues("Content-Type").stream().anyMatch(contentType -> contentType.contains("application/json"));
    }

    private static boolean isSuccess(HttpResponse<InputStream> response) {
        return response.statusCode() < 300;
    }

    private static boolean isRedirectToCas(HttpResponse<?> response) {
        return response.headers().allValues("Location").stream().anyMatch(location -> location.contains("/cas/login"));
    }

    private static boolean isUnauthenticated(HttpResponse<?> response) {
        return response.statusCode() == 401;
    }
}
