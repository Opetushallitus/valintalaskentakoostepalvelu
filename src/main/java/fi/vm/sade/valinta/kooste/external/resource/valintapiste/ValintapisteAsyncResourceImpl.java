package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import com.google.gson.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import io.mikael.urlbuilder.UrlBuilder;
import io.reactivex.Observable;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ValintapisteAsyncResourceImpl extends UrlConfiguredResource implements ValintapisteAsyncResource {
    public static final String OK = "";
    private final HttpClient httpClient;
    Logger LOG = LoggerFactory.getLogger(ValintapisteAsyncResource.class);

    public ValintapisteAsyncResourceImpl(@Qualifier("ValintapisteServiceHttpClient") HttpClient httpClient) {
        super(TimeUnit.MINUTES.toMillis(30));
        this.httpClient = httpClient;
    }

    private String body(Response r) {
        try {
            InputStream e = (InputStream) r.getEntity();
            return IOUtils.toString(e, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private String getUrl(Response response) {
        try {
            ResponseImpl r = (ResponseImpl) response;
            Object url = r.getOutMessage().get("org.apache.cxf.request.uri");
            return url.toString();
        } catch (Exception e) {
            LOG.error(String.format("Urlin selvittämisessä tapahtui virhe: " + e));
            return "null";
        }
    }
    private Observable<PisteetWithLastModified> handleResponse(Response response) {
        if(response.getStatus() != 200) {
            return Observable.error(new RuntimeException("Valintapisteitä ei saatu luettua palvelusta!"));
        } else {
            try {
                final String entity = body(response);
                List<Valintapisteet> pisteet = gson().fromJson(entity, new GenericType<List<Valintapisteet>>() {
                }.getType());
                if(pisteet == null) {
                    String s = response.readEntity(String.class);
                    LOG.error("Valintapisteet null! Response {}", s);
                    String url = getUrl(response);
                    return Observable.error(new RuntimeException(String.format("Null response for url %s", url)));
                } else {
                    return Observable.just(new PisteetWithLastModified(Optional.ofNullable(response.getHeaderString(LAST_MODIFIED)), pisteet));
                }
            } catch (Exception e) {
                return Observable.error(e);
            }
        }
    }

    private void setAuditInfo(WebClient client, AuditSession auditSession) {
        client.query("sessionId", auditSession.getSessionId());
        client.query("uid", auditSession.getPersonOid());
        client.query("inetAddress", auditSession.getInetAddress());
        client.query("userAgent", auditSession.getUserAgent());
    }

    private String setAuditInfo(String url, AuditSession auditSession) {
        URI uri = UrlBuilder.fromString(url)
            .addParameter("sessionId", auditSession.getSessionId())
            .addParameter("uid", auditSession.getPersonOid())
            .addParameter("inetAddress", auditSession.getInetAddress())
            .addParameter("userAgent", auditSession.getUserAgent())
            .toUri();
        return uri.toString();
    }

    @Override
    public CompletableFuture<PisteetWithLastModified> getValintapisteet(String hakuOID, String hakukohdeOID, AuditSession auditSession) {
        String url = getUrl("valintapiste-service.get.pisteet", hakuOID, hakukohdeOID);
        return httpClient.getJson(
            setAuditInfo(url, auditSession),
            Duration.ofSeconds(10),
            inputStreamHttpResponse -> {
                List<Valintapisteet> pisteet = httpClient.parseJson(
                    inputStreamHttpResponse,
                    new GenericType<List<Valintapisteet>>() {
                    }.getType());
                return new PisteetWithLastModified(
                    Optional.ofNullable(inputStreamHttpResponse.headers().firstValue(LAST_MODIFIED).toString()),
                    pisteet);
            });
    }

    @Override
    public Observable<PisteetWithLastModified> getValintapisteet(Collection<String> hakemusOIDs, AuditSession auditSession) {
        Observable<Response> response = postAsObservableLazily(
                getUrl("valintapiste-service.get.pisteet.with.hakemusoids"),
                Entity.entity(hakemusOIDs, MediaType.APPLICATION_JSON_TYPE), client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    setAuditInfo(client, auditSession);
                    return client;
                });
        return response.switchMap(this::handleResponse);
    }

    @Override
    public CompletableFuture<Set<String>> putValintapisteet(Optional<String> ifUnmodifiedSince, List<Valintapisteet> pisteet, AuditSession auditSession) {
        URI uri = UrlBuilder.fromString(setAuditInfo(getUrl("valintapiste-service.put.pisteet"), auditSession))
            .addParameter("save-partially", "true")
            .toUri();
        return httpClient.putJson(
            uri.toString(),
            Duration.ofMinutes(30),
            pisteet,
            new TypeToken<List<Valintapisteet>>() {}.getType(),
            new TypeToken<Set<String>>() {}.getType(),
            requestBuilder -> {
                ifUnmodifiedSince.ifPresent(since -> requestBuilder.header(IF_UNMODIFIED_SINCE, since));
                return requestBuilder;
            });
    }
}
