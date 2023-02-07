package fi.vm.sade.valinta.kooste.valintatapajono.route;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;

public interface ValintatapajonoVientiRoute {

  void vie(
      DokumenttiProsessi prosessi, String hakuOid, String hakukohdeOid, String valintatapajonoOid);
}
