package fi.vm.sade.valinta.kooste.external.resource.dokumentti.impl;

import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DokumenttiAsyncResourceImpl extends UrlConfiguredResource implements DokumenttiAsyncResource {

    public DokumenttiAsyncResourceImpl() {
        super(TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public Observable<String> uudelleenNimea(String dokumenttiId, String filename) {
        return putAsObservableLazily(getUrl("dokumenttipalvelu-service.dokumentit.uudelleennimea", dokumenttiId), new TypeToken<String>() {}.getType(), Entity.text(filename));
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
