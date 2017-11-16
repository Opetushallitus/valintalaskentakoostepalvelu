package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import fi.vm.sade.valinta.http.FailedHttpException;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
                    LOG.error("Valintapisteet null!");
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

    @Override
    public Observable<PisteetWithLastModified> getValintapisteet(String hakuOID, String hakukohdeOID, AuditSession auditSession) {
        Observable<Response> response = getAsObservable(
                getUrl("valintapiste-service.get.pisteet", hakuOID, hakukohdeOID),
                //new GenericType<List<Valintapisteet>>(){}.getType(),
                client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("sessionId", auditSession.getSessionId());
                    client.query("uid", auditSession.getUid());
                    client.query("inetAddress", auditSession.getInetAddress());
                    client.query("userAgent", auditSession.getUserAgent());
                    return client;
                });

        return response.switchMap(this::handleResponse);
    }

    @Override
    public Observable<PisteetWithLastModified> getValintapisteet(Collection<String> hakemusOIDs, AuditSession auditSession) {
        Observable<Response> response = postAsObservable(
                getUrl("valintapiste-service.get.pisteet.with.hakemusoids"),
                Entity.entity(hakemusOIDs, MediaType.APPLICATION_JSON_TYPE), client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("sessionId", auditSession.getSessionId());
                    client.query("uid", auditSession.getUid());
                    client.query("inetAddress", auditSession.getInetAddress());
                    client.query("userAgent", auditSession.getUserAgent());
                    return client;
                });
        return response.switchMap(this::handleResponse);
    }

    @Override
    public Observable<Response> putValintapisteet(Optional<String> ifUnmodifiedSince, List<Valintapisteet> pisteet, AuditSession auditSession) {
        Observable<Response> response = putAsObservable(
                getUrl("valintapiste-service.put.pisteet"),
                Entity.entity(pisteet, MediaType.APPLICATION_JSON_TYPE)
                , client -> {
                    ifUnmodifiedSince.ifPresent(since -> client.header(IF_UNMODIFIED_SINCE, since));
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("sessionId", auditSession.getSessionId());
                    client.query("uid", auditSession.getUid());
                    client.query("inetAddress", auditSession.getInetAddress());
                    client.query("userAgent", auditSession.getUserAgent());
                    return client;
                }
        );

        return response.onErrorResumeNext(t -> {
            if(t instanceof FailedHttpException) {
                FailedHttpException f = (FailedHttpException)t;
                if(f.response.getStatus() == 409) {
                    String body = body(f.response);
                    return Observable.error(new RuntimeException("Ei voida tallentaa, koska kannassa oli välissä muuttuneita pistetietoja hakemuksilla: " + body));
                }
            }
            return Observable.error(t);
        });
    }
}
