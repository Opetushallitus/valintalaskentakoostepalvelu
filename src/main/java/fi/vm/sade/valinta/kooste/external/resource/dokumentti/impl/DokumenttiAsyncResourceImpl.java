package fi.vm.sade.valinta.kooste.external.resource.dokumentti.impl;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class DokumenttiAsyncResourceImpl extends UrlConfiguredResource implements DokumenttiAsyncResource {
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
}
