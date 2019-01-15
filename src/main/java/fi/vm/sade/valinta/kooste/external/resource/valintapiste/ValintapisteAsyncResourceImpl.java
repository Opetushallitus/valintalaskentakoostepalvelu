package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import io.reactivex.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ValintapisteAsyncResourceImpl extends UrlConfiguredResource implements ValintapisteAsyncResource {
    public static final String OK = "";
    Logger LOG = LoggerFactory.getLogger(ValintapisteAsyncResource.class);

    public ValintapisteAsyncResourceImpl() {
        super(TimeUnit.MINUTES.toMillis(30));
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

    @Override
    public Observable<PisteetWithLastModified> getValintapisteet(String hakuOID, String hakukohdeOID, AuditSession auditSession) {
        Observable<Response> response = getAsObservableLazily(
                getUrl("valintapiste-service.get.pisteet", hakuOID, hakukohdeOID),
                //new GenericType<List<Valintapisteet>>(){}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    setAuditInfo(client, auditSession);
                    return client;
                });

        return response.switchMap(this::handleResponse);
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
    public Observable<Set<String>> putValintapisteet(Optional<String> ifUnmodifiedSince, List<Valintapisteet> pisteet, AuditSession auditSession) {
        return putAsObservableLazily(
                getUrl("valintapiste-service.put.pisteet"),
                new TypeToken<Set<String>>() { }.getType(),
                Entity.entity(DEFAULT_GSON.toJson(pisteet), MediaType.APPLICATION_JSON_TYPE)
                , client -> {
                    ifUnmodifiedSince.ifPresent(since -> client.header(IF_UNMODIFIED_SINCE, since));
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    setAuditInfo(client, auditSession);
                    client.query("save-partially", "true");
                    return client;
                }
        );
    }
}
