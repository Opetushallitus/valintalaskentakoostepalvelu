package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import io.reactivex.Observable;
import org.apache.commons.io.IOUtils;
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
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private void setAuditInfo(Map<String, String> query, AuditSession auditSession) {
        query.put("sessionId", auditSession.getSessionId());
        query.put("uid", auditSession.getPersonOid());
        query.put("inetAddress", auditSession.getInetAddress());
        query.put("userAgent", auditSession.getUserAgent());
    }

    @Override
    public CompletableFuture<PisteetWithLastModified> getValintapisteet(String hakuOID, String hakukohdeOID, AuditSession auditSession) {
        Map<String, String> query = new HashMap<>();
        setAuditInfo(query, auditSession);
        String url = getUrl("valintapiste-service.get.pisteet", hakuOID, hakukohdeOID, query);
        return httpClient.getJson(
            url,
            Duration.ofSeconds(10),
            inputStreamHttpResponse -> {
                List<Valintapisteet> pisteet = httpClient.parseJson(
                    inputStreamHttpResponse,
                    new GenericType<List<Valintapisteet>>() {
                    }.getType());
                return new PisteetWithLastModified(
                    inputStreamHttpResponse.headers().firstValue(LAST_MODIFIED),
                    pisteet);
            });
    }

    @Override
    public Observable<PisteetWithLastModified> getValintapisteet(Collection<String> hakemusOIDs, AuditSession auditSession) {
        Map<String, String> query = new HashMap<>();
        setAuditInfo(query, auditSession);
        String url = getUrl("valintapiste-service.get.pisteet.with.hakemusoids", query);
        Observable<Response> response = postAsObservableLazily(
                url,
                Entity.entity(hakemusOIDs, MediaType.APPLICATION_JSON_TYPE), client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    return client;
                });
        return response.switchMap(this::handleResponse);
    }

    @Override
    public CompletableFuture<Set<String>> putValintapisteet(Optional<String> ifUnmodifiedSince, List<Valintapisteet> pisteet, AuditSession auditSession) {
        Map<String, String> query = new HashMap<>();
        query.put("save-partially", "true");
        setAuditInfo(query, auditSession);
        String url = getUrl("valintapiste-service.put.pisteet", query);
        return httpClient.putJson(
            url,
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
