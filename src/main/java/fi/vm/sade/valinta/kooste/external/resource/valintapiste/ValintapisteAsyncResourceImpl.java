package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.GsonResponseCallback;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusOid;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ListFullSearchDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Muutoshistoria;
import org.apache.commons.io.IOUtils;
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
import java.util.stream.Collectors;

@Service
public class ValintapisteAsyncResourceImpl extends UrlConfiguredResource implements ValintapisteAsyncResource {
    public static final String OK = "";

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

    private Observable<PisteetWithLastModified> handleResponse(Response response) {
        if(response.getStatus() != 200) {
            return Observable.error(new RuntimeException("Valintapisteitä ei saatu luettua palvelusta!"));
        } else {
            try {
                List<Valintapisteet> vp = gson().fromJson(body(response), new GenericType<List<Valintapisteet>>() {
                }.getType());
                return Observable.just(new PisteetWithLastModified(Optional.ofNullable(response.getHeaderString(LAST_MODIFIED)), vp));
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
    public Observable<PisteetWithLastModified> getValintapisteet(String hakuOID, Collection<String> hakemusOIDs, AuditSession auditSession) {
        Observable<Response> response = postAsObservable(
                getUrl("valintapiste-service.get.pisteet.with.hakemusoids", hakuOID),
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
    public Observable<Object> putValintapisteet(String hakuOID, String hakukohdeOID, Optional<String> ifUnmodifiedSince, List<Valintapisteet> pisteet, AuditSession auditSession) {
        Observable<Response> response = putAsObservable(
                getUrl("valintapiste-service.put.pisteet", hakuOID, hakukohdeOID),
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
        return response.switchMap(r -> {
            if(r.getStatus() == 200) {
                return Observable.just(OK);
            } else {
                String body = body(r);
                if(r.getStatus() == 409) {

                    return Observable.error(new RuntimeException("Ei voida tallentaa, koska kannassa oli välissä muuttuneita pistetietoja hakemuksilla: " + body));
                } else {
                    return Observable.error(new RuntimeException("Tuntematon virhe tallennuksessa! " + body));
                }
            }
        });
    }
}
