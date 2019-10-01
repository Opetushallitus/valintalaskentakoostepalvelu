package fi.vm.sade.valinta.kooste.external.resource;

import com.google.gson.Gson;
import fi.vm.sade.javautils.cas.ApplicationSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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
                .header("Content-Type", "application/json")
                .GET()
                .timeout(timeout)
                .build();
        return this.makeRequest(request).thenCompose(response -> this.parseJson(response, outputType));
    }

    public <I, O> CompletableFuture<O> postJson(String url, Duration timeout, I body, Type inputType, Type outputType) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Caller-Id", CALLER_ID)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(this.gson.toJson(body, inputType), Charset.forName("UTF-8")))
                .timeout(timeout)
                .build();
        return this.makeRequest(request).thenCompose(response -> this.parseJson(response, outputType));
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

    private <O> CompletableFuture<O> parseJson(HttpResponse<InputStream> response, Type outputType) {
        if (299 < response.statusCode()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), Charset.forName("UTF-8")))) {
                String errorBody = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                return CompletableFuture.failedFuture(new IllegalStateException(String.format("URL %s responded %d %s", response.uri(), response.statusCode(), errorBody)));
            } catch (IOException e) {
                return CompletableFuture.failedFuture(new RuntimeException("Failed to read response body", e));
            }
        }
        if (response.headers().allValues("Content-Type").stream().noneMatch(contentType -> contentType.contains("application/json"))) {
            String contentType = String.join(", ", response.headers().allValues("Content-Type"));
            return CompletableFuture.failedFuture(new IllegalStateException(String.format("Unexpected Content-Type %s", contentType)));
        }
        return CompletableFuture.completedFuture(this.gson.fromJson(new InputStreamReader(response.body()), outputType));
    }

    private static boolean isRedirectToCas(HttpResponse<?> response) {
        return response.headers().allValues("Location").stream().anyMatch(location -> location.contains("/cas/login"));
    }

    private static boolean isUnauthenticated(HttpResponse<?> response) {
        return response.statusCode() == 401;
    }
}
