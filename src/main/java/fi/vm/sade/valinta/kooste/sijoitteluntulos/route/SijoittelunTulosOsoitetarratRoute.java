package fi.vm.sade.valinta.kooste.sijoitteluntulos.route;

import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import org.springframework.security.core.Authentication;

public interface SijoittelunTulosOsoitetarratRoute {

  void osoitetarratHaulle(
      SijoittelunTulosProsessi prosessi,
      String hakuOid,
      String sijoitteluAjoId,
      Authentication auth);
}
