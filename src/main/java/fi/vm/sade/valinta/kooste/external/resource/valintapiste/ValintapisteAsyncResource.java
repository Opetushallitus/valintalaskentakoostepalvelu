package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import io.reactivex.Observable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ValintapisteAsyncResource {
    String LAST_MODIFIED = "Last-Modified";
    String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    Observable<PisteetWithLastModified> getValintapisteet(String hakuOID, String hakukohdeOID, AuditSession auditSession);

    Observable<PisteetWithLastModified> getValintapisteet(Collection<String> hakemusOIDs, AuditSession auditSession);

    Observable<Set<String>> putValintapisteet(Optional<String> ifUnmodifiedSince, List<Valintapisteet> pisteet, AuditSession auditSession);

}
