package fi.vm.sade.valinta.kooste.sijoitteluntulos.route;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

public interface SijoittelunTulosOsoitetarratRoute {
  final String SEDA_SIJOITTELUNTULOS_OSOITETARRAT_HAULLE =
      "seda:sijoitteluntulos_osoitetarrat_haulle?"
          +
          // jos palvelin sammuu niin ei suorita loppuun tyojonoa
          "purgeWhenStopping=true"
          +
          // reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
          "&waitForTaskToComplete=Never"
          +
          // tyojonossa on yksi tyostaja
          "&concurrentConsumers=1";

  void osoitetarratHaulle(
      @Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) SijoittelunTulosProsessi prosessi,
      @Property(OPH.HAKUOID) String hakuOid,
      @Property(OPH.SIJOITTELUAJOID) String sijoitteluAjoId,
      @Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
