package fi.vm.sade.valinta.kooste.external.resource;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class ApplicationSession {
  private static final String CSRF_VALUE = "CSRF";
  private final HttpClient client;
  private final CookieManager cookieManager;
  private final String callerId;
  private final Duration authenticationTimeout;
  private final CasSession casSession;
  private final String service;
  private final String cookieName;
  private CompletableFuture<SessionToken> sessionToken;

  public ApplicationSession(
      HttpClient client,
      CookieManager cookieManager,
      String callerId,
      Duration authenticationTimeout,
      CasSession casSession,
      String service,
      String cookieName) {
    this.client = client;
    this.cookieManager = cookieManager;
    this.callerId = callerId;
    this.authenticationTimeout = authenticationTimeout;
    this.casSession = casSession;
    this.service = service;
    this.cookieName = cookieName;
    this.sessionToken = CompletableFuture.failedFuture(new IllegalStateException("uninitialized"));
  }

  public CompletableFuture<SessionToken> getSessionToken() {
    CompletableFuture<SessionToken> currentSessionToken = this.sessionToken;
    if (currentSessionToken.isCompletedExceptionally()) {
      synchronized (this) {
        if (this.sessionToken.isCompletedExceptionally()) {
          this.sessionToken =
              this.casSession.getServiceTicket(this.service).thenComposeAsync(this::requestSession);
        }

        currentSessionToken = this.sessionToken;
      }
    }

    return currentSessionToken;
  }

  public synchronized void invalidateSession(SessionToken sessionToken) {
    if (this.sessionToken.isCompletedExceptionally()
        || sessionToken.equals(this.sessionToken.getNow((SessionToken) null))) {
      this.sessionToken = CompletableFuture.failedFuture(new IllegalStateException("invalidated"));
    }
  }

  private CompletableFuture<SessionToken> requestSession(ServiceTicket serviceTicket) {
    HttpRequest request =
        HttpRequest.newBuilder(serviceTicket.getLoginUrl())
            .GET()
            .header("Caller-Id", this.callerId)
            .header("CSRF", "CSRF")
            .header("Cookie", String.format("CSRF=%s;", "CSRF"))
            .timeout(this.authenticationTimeout)
            .build();
    return this.client
        .sendAsync(request, BodyHandlers.discarding())
        .handle(
            (response, e) -> {
              if (e != null) {
                throw new IllegalStateException(
                    String.format("%s: Failed to establish session", request.uri().toString()), e);
              } else {
                return new SessionToken(serviceTicket, this.getCookie(response, serviceTicket));
              }
            });
  }

  private HttpCookie getCookie(HttpResponse<Void> response, ServiceTicket serviceTicket) {
    URI loginUrl = serviceTicket.getLoginUrl();
    return (HttpCookie)
        this.cookieManager.getCookieStore().get(loginUrl).stream()
            .filter(
                (cookie) -> {
                  return loginUrl.getPath().startsWith(cookie.getPath())
                      && this.cookieName.equals(cookie.getName());
                })
            .findAny()
            .orElseThrow(
                () -> {
                  return new IllegalStateException(
                      String.format(
                          "%s %d: Failed to establish session. No cookie %s set. Headers: %s",
                          response.uri().toString(),
                          response.statusCode(),
                          this.cookieName,
                          response.headers()));
                });
  }
}
