package fi.vm.sade.valinta.kooste.valintalaskentatulos.route;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import java.io.InputStream;
import org.apache.camel.Property;
import org.springframework.security.core.Authentication;

public interface SijoittelunTulosExcelRoute {

  final String SEDA_SIJOITTELU_EXCEL =
      "seda:sijoittelunTulos_xls?"
          +
          // jos palvelin sammuu niin ei suorita loppuun tyojonoa
          "purgeWhenStopping=true"
          +
          // reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
          "&waitForTaskToComplete=Never"
          +
          // tyojonossa on yksi tyostaja
          "&concurrentConsumers=5";

  public InputStream luoXls(
      @Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) DokumenttiProsessi prosessi,
      @Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
      @Property(OPH.SIJOITTELUAJOID) String sijoitteluajoId,
      @Property(OPH.HAKUOID) String hakuOid,
      @Property(SecurityPreprocessor.SECURITY_CONTEXT_HEADER) Authentication auth);
}
