package fi.vm.sade.valinta.kooste.sijoitteluntulos.route;

import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import org.springframework.security.core.Authentication;

public interface SijoittelunTulosTaulukkolaskentaRoute {

  void taulukkolaskennatHaulle(
      SijoittelunTulosProsessi prosessi,
      String hakuOid,
      String sijoitteluAjoId,
      AuditSession session,
      Authentication auth);

}
