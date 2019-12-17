package fi.vm.sade.valinta.kooste.external.resource.dokumentti.impl;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class DokumenttiAsyncResourceImpl extends UrlConfiguredResource implements DokumenttiAsyncResource {
    private static final Logger LOG = LoggerFactory.getLogger(DokumenttiAsyncResourceImpl.class);
    private final HttpClient client;

    @Autowired
    public DokumenttiAsyncResourceImpl(
            @Qualifier("DokumenttiHttpClient") HttpClient client
    ) {
        super(TimeUnit.HOURS.toMillis(1));
        this.client = client;
    }

    @Override
    public CompletableFuture<Void> uudelleenNimea(String dokumenttiId, String filename) {
        return this.client.putResponse(
                getUrl("dokumenttipalvelu-service.dokumentit.uudelleennimea", dokumenttiId),
                Duration.ofMinutes(1),
                filename.getBytes(Charset.forName("UTF-8")),
                "text/plain"
        ).thenApply(response -> {
            try (InputStream is = response.body()) {
                is.readAllBytes();
                return null;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public Observable<Response> tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata) {
        return putAsObservableLazily(
                getUrl("dokumenttipalvelu-service.dokumentit.tallenna"),
                Entity.entity(filedata, MediaType.APPLICATION_OCTET_STREAM),
                client -> {
                    client.query("id", id);
                    client.query("filename", filename);
                    client.query("expirationDate", expirationDate);
                    client.query("tags", tags.toArray());
                    client.query("mimeType", mimeType);
                    client.accept(MediaType.WILDCARD_TYPE);
                    return client;
                }
        );
    }

    @Override
    public CompletableFuture<HttpResponse<InputStream>> lataa(String documentId) {
        return this.client.getResponse(
                getUrl("dokumenttipalvelu-service.dokumentit.lataa", documentId),
                Duration.ofMinutes(1),
                x -> x
        ).thenApply(response -> {
            if (response.statusCode() == 404) {
                LOG.error("Dokumentin " + documentId + " lataus dokumenttipalvelusta epäonnistui.");
            }
            return response;
        });
    }

    @Override
    public CompletableFuture<Void> tyhjenna() {
        return this.client.putResponse(
                getUrl("dokumenttipalvelu-service.dokumentit.tyhjenna"),
                Duration.ofMinutes(1),
                "empty body".getBytes(),
                "text/plain"
        ).thenApply(response -> {
            if (response.statusCode() == 404) {
                throw new RuntimeException("Dokumenttipalvelun vanhentuneiden dokumenttien tyhjennys epäonnistui.");
            }
            return null;
        });
    }
}
