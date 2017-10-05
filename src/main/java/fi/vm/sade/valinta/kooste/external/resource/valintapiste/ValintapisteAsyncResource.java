package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import rx.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ValintapisteAsyncResource {
    String LAST_MODIFIED = "Last-Modified";
    String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    Observable<PisteetWithLastModified> getValintapisteet(String hakuOID, String hakukohdeOID, AuditSession auditSession);

    Observable<PisteetWithLastModified> getValintapisteet(String hakuOID, Collection<String> hakemusOIDs, AuditSession auditSession);

    Observable<Object> putValintapisteet(String hakuOID, String hakukohdeOID, Optional<String> ifUnmodifiedSince, List<Valintapisteet> pisteet, AuditSession auditSession);

}
