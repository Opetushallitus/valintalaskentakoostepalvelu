package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.UrlConfiguredResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusOid;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ListFullSearchDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Muutoshistoria;
import org.springframework.stereotype.Service;
import rx.Observable;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ValintapisteAsyncResourceImpl extends UrlConfiguredResource implements ValintapisteAsyncResource {

    public ValintapisteAsyncResourceImpl() {
        super(TimeUnit.MINUTES.toMillis(30));
    }

    @Override
    public Observable<List<Valintapisteet>> getValintapisteet(String hakuOID, String hakukohdeOID, AuditSession auditSession) {
        return  getAsObservable(
                getUrl("valintapiste-service.get.pisteet", hakuOID, hakukohdeOID),
                new GenericType<List<Valintapisteet>>(){}.getType(),client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("sessionId", auditSession.getSessionId());
                    client.query("uid", auditSession.getUid());
                    client.query("inetAddress", auditSession.getInetAddress());
                    client.query("userAgent", auditSession.getUserAgent());
                    return client;
                });
    }

    @Override
    public Observable<Void> putValintapisteet(String hakuOID, String hakukohdeOID, List<Valintapisteet> pisteet, AuditSession auditSession) {
        return putAsObservable(
                getUrl("valintapiste-service.put.pisteet", hakuOID, hakukohdeOID),
                Void.class,
                Entity.entity(pisteet, MediaType.APPLICATION_JSON_TYPE)
                ,client -> {
                    client.accept(MediaType.APPLICATION_JSON_TYPE);
                    client.query("sessionId", auditSession.getSessionId());
                    client.query("uid", auditSession.getUid());
                    client.query("inetAddress", auditSession.getInetAddress());
                    client.query("userAgent", auditSession.getUserAgent());
                    return client;
                }
        );
    }
}
