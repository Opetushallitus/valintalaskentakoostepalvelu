package fi.vm.sade.valinta.kooste.external.resource.dokumentti.impl;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import io.reactivex.Observable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class DokumenttiAsyncResourceImpl implements DokumenttiAsyncResource {
  private static final Logger LOG = LoggerFactory.getLogger(DokumenttiAsyncResourceImpl.class);
  private final HttpClient client;

  private final UrlConfiguration urlConfiguration;

  @Autowired
  public DokumenttiAsyncResourceImpl(@Qualifier("DokumenttiHttpClient") HttpClient client) {
    this.client = client;
    this.urlConfiguration = UrlConfiguration.getInstance();
  }

  @Override
  public CompletableFuture<Void> uudelleenNimea(String dokumenttiId, String filename) {
    return this.client
        .putResponse(
            this.urlConfiguration.url(
                "dokumenttipalvelu-service.dokumentit.uudelleennimea", dokumenttiId),
            Duration.ofMinutes(1),
            filename.getBytes(Charset.forName("UTF-8")),
            "text/plain")
        .thenApply(
            response -> {
              try (InputStream is = response.body()) {
                is.readAllBytes();
                return null;
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  @Override
  public Observable<ResponseEntity> tallenna(
      String id,
      String filename,
      Long expirationDate,
      List<String> tags,
      String mimeType,
      InputStream filedata) {

    try {
      String url = this.urlConfiguration.url("dokumenttipalvelu-service.dokumentit.tallenna");
      url += "?id=" + id;
      url += "&filename=" + filename;
      url += "&expirationDate=" + expirationDate;
      url += "&tags=" + tags.stream().collect(Collectors.joining(","));
      url += "&mimetype=" + mimeType;

      return Observable.fromFuture(
          this.client
              .putResponse(
                  url,
                  Duration.ofHours(1l),
                  IOUtils.toByteArray(filedata),
                  "application/octet-stream")
              .thenApply(r -> ResponseEntity.ok().build()));
    } catch (IOException e) {
      return Observable.error(e);
    }
  }

  @Override
  public CompletableFuture<HttpResponse<InputStream>> lataa(String documentId) {
    return this.client
        .getResponse(
            this.urlConfiguration.url("dokumenttipalvelu-service.dokumentit.lataa", documentId),
            Duration.ofMinutes(1),
            x -> x)
        .thenApply(
            response -> {
              if (response.statusCode() == 404) {
                LOG.error("Dokumentin " + documentId + " lataus dokumenttipalvelusta epäonnistui.");
              }
              return response;
            });
  }

  @Override
  public CompletableFuture<Void> tyhjenna() {
    return this.client
        .putResponse(
            this.urlConfiguration.url("dokumenttipalvelu-service.dokumentit.tyhjenna"),
            Duration.ofMinutes(1),
            "I wanna be some body".getBytes(),
            "text/plain")
        .thenApply(
            response -> {
              if (response.statusCode() == 204) {
                return null;
              }
              throw new RuntimeException(
                  "Dokumenttipalvelun vanhentuneiden dokumenttien tyhjennys epäonnistui: "
                      + response.statusCode());
            });
  }
}
