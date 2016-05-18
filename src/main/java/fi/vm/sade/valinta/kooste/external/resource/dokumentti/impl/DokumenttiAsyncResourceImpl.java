package fi.vm.sade.valinta.kooste.external.resource.dokumentti.impl;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.http.ResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.*;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class DokumenttiAsyncResourceImpl extends HttpResource implements DokumenttiAsyncResource {

    @Autowired
    public DokumenttiAsyncResourceImpl(
            @Value("${web.url.cas}") String webCasUrl,
            @Value("${cas.service.dokumenttipalvelu}/j_spring_cas_security_check") String targetService,
            @Value("${valintalaskentakoostepalvelu.app.username.to.valintaperusteet}") String appClientUsername,
            @Value("${valintalaskentakoostepalvelu.app.password.to.valintaperusteet}") String appClientPassword,
            @Value("${valintalaskentakoostepalvelu.dokumenttipalvelu.rest.url}") String address,
            ApplicationContext context) {
        super(address, TimeUnit.HOURS.toMillis(1));
    }

    @Override
    public Observable<String> uudelleenNimea(String dokumenttiId, String filename) {
        return putAsObservable("/dokumentit/uudelleennimea/" + dokumenttiId, new TypeToken<String>() {}.getType(), Entity.text(filename));
    }
    @Override
    public Observable<Response> tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata) {
        String url = "/dokumentit/tallenna";


        return putAsObservable(
                url,
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
    public Peruutettava tallenna(String id, String filename, Long expirationDate, List<String> tags, String mimeType, InputStream filedata, Consumer<Response> responseCallback, Consumer<Throwable> failureCallback) {
        String url = "/dokumentit/tallenna";

        try {
            return new PeruutettavaImpl(
                    getWebClient()
                            .path(url)
                            .query("id", id)
                            .query("filename", filename)
                            .query("expirationDate", expirationDate)
                            .query("tags", tags.toArray())
                            .query("mimeType", mimeType)
                           .async()
                            .put(Entity.entity(filedata, MediaType.APPLICATION_OCTET_STREAM), new ResponseCallback(address + url, responseCallback, failureCallback)));
        } catch (Exception e) {
            failureCallback.accept(e);
            return TyhjaPeruutettava.tyhjaPeruutettava();
        }
    }
}
