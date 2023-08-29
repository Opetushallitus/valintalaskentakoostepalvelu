package fi.vm.sade.valinta.kooste.external.resource;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class CasSession {
  private static final String CSRF_VALUE = "CSRF";
  private final HttpClient client;
  private final Duration requestTimeout;
  private final String callerId;
  private final URI ticketsUrl;
  private final String username;
  private final String password;
  private CompletableFuture<URI> ticketGrantingTicket;

  public CasSession(
      HttpClient client,
      Duration requestTimeout,
      String callerId,
      URI ticketsUrl,
      String username,
      String password) {
    this.client = client;
    this.requestTimeout = requestTimeout;
    this.callerId = callerId;
    this.ticketsUrl = ticketsUrl;
    this.username = username;
    this.password = password;
    this.ticketGrantingTicket =
        CompletableFuture.failedFuture(new IllegalStateException("uninitialized"));
  }

  public CompletableFuture<ServiceTicket> getServiceTicket(String service) {
    return this.getTicketGrantingTicket()
        .thenCompose(
            (currentTicketGrantingTicket) -> {
              return this.requestServiceTicket(currentTicketGrantingTicket, service)
                  .handle(
                      (serviceTicket, e) -> {
                        if (serviceTicket != null) {
                          return CompletableFuture.completedFuture(serviceTicket);
                        } else {
                          this.invalidateTicketGrantingTicket(currentTicketGrantingTicket);
                          return this.getTicketGrantingTicket()
                              .thenCompose(
                                  (newTicketGrantingTicket) -> {
                                    return this.requestServiceTicket(
                                        newTicketGrantingTicket, service);
                                  });
                        }
                      });
            })
        .thenCompose(
            (f) -> {
              return f;
            });
  }

  private CompletableFuture<URI> getTicketGrantingTicket() {
    CompletableFuture<URI> currentTicketGrantingTicket = this.ticketGrantingTicket;
    if (currentTicketGrantingTicket.isCompletedExceptionally()) {
      synchronized (this) {
        if (this.ticketGrantingTicket.isCompletedExceptionally()) {
          HttpRequest request =
              HttpRequest.newBuilder(this.ticketsUrl)
                  .POST(
                      BodyPublishers.ofString(
                          String.format(
                              "username=%s&password=%s",
                              URLEncoder.encode(this.username, Charset.forName("UTF-8")),
                              URLEncoder.encode(this.password, Charset.forName("UTF-8")))))
                  .header("Content-Type", "application/x-www-form-urlencoded")
                  .header("Caller-Id", this.callerId)
                  .header("CSRF", "CSRF")
                  .header("Cookie", String.format("CSRF=%s;", "CSRF"))
                  .timeout(this.requestTimeout)
                  .build();
          this.ticketGrantingTicket =
              this.client
                  .sendAsync(request, BodyHandlers.ofString(Charset.forName("UTF-8")))
                  .handle(
                      (response, e) -> {
                        if (e != null) {
                          throw new IllegalStateException(request.uri().toString(), e);
                        } else if (response.statusCode() != 201) {
                          throw new IllegalStateException(
                              String.format(
                                  "%s %d: %s",
                                  request.uri().toString(),
                                  response.statusCode(),
                                  response.body()));
                        } else {
                          return (URI)
                              response
                                  .headers()
                                  .firstValue("Location")
                                  .map(URI::create)
                                  .orElseThrow(
                                      () -> {
                                        return new IllegalStateException(
                                            String.format(
                                                "%s %d: %s",
                                                request.uri().toString(),
                                                response.statusCode(),
                                                "Could not parse TGT, no Location header found"));
                                      });
                        }
                      });
        }

        return this.ticketGrantingTicket;
      }
    } else {
      return currentTicketGrantingTicket;
    }
  }

  private synchronized void invalidateTicketGrantingTicket(URI invalidTicketGrantingTicket) {
    if (this.ticketGrantingTicket.isCompletedExceptionally()
        || invalidTicketGrantingTicket != null
            && invalidTicketGrantingTicket.equals(this.ticketGrantingTicket.getNow((URI) null))) {
      this.ticketGrantingTicket =
          CompletableFuture.failedFuture(new IllegalStateException("invalidated"));
    }
  }

  private CompletableFuture<ServiceTicket> requestServiceTicket(URI tgt, String service) {
    HttpRequest request =
        HttpRequest.newBuilder(tgt)
            .POST(
                BodyPublishers.ofString(
                    String.format(
                        "service=%s", URLEncoder.encode(service, Charset.forName("UTF-8")))))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Caller-Id", this.callerId)
            .header("CSRF", "CSRF")
            .header("Cookie", String.format("CSRF=%s;", "CSRF"))
            .timeout(this.requestTimeout)
            .build();
    return this.client
        .sendAsync(request, BodyHandlers.ofString(Charset.forName("UTF-8")))
        .handle(
            (response, e) -> {
              if (e != null) {
                throw new IllegalStateException(request.uri().toString(), e);
              } else if (response.statusCode() != 200) {
                throw new IllegalStateException(
                    String.format(
                        "%s %d: %s",
                        response.uri().toString(), response.statusCode(), response.body()));
              } else {
                return new ServiceTicket(service, (String) response.body());
              }
            });
  }
}
