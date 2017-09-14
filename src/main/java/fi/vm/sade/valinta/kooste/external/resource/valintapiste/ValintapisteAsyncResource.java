package fi.vm.sade.valinta.kooste.external.resource.valintapiste;

import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import rx.Observable;

import java.util.List;

public interface ValintapisteAsyncResource {

    Observable<List<Valintapisteet>> getValintapisteet(String hakuOID, String hakukohdeOID, AuditSession auditSession);

    Observable<Void> putValintapisteet(String hakuOID, String hakukohdeOID, List<Valintapisteet> pisteet, AuditSession auditSession);

}
