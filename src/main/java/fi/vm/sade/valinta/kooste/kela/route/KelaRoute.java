package fi.vm.sade.valinta.kooste.kela.route;

import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import org.apache.camel.Body;
import org.apache.camel.Property;

public interface KelaRoute {

  /** Aloittaa Kela-siirtodokumentin luonnin. */
  void aloitaKelaLuonti(
      @Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI) KelaProsessi prosessi,
      @Body KelaLuonti luonti);

  /** Camel route description. */
  final String SEDA_KELA_LUONTI =
      "seda:kela_luonti?"
          +
          // jos palvelin sammuu niin ei suorita loppuun tyojonoa
          "purgeWhenStopping=true"
          +
          // reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
          "&waitForTaskToComplete=Never"
          +
          // tyojonossa on yksi tyostaja
          "&concurrentConsumers=1";
  /** Camel route description. */
  final String DIRECT_KELA_FAILED = "direct:kela_failed";
  /** Property hakuOid */
  final String PROPERTY_HAKUOID = "hakuOid";
  /** Property lukuvuosi */
  final String PROPERTY_LUKUVUOSI = "lukuvuosi";
  /** Property poimintapaivamaara */
  final String PROPERTY_POIMINTAPAIVAMAARA = "poimintapaivamaara";
  /** Property aineistonNimi */
  final String PROPERTY_AINEISTONNIMI = "aineistonNimi";
  /** Property siirtoTunnus */
  final String PROPERTY_SIIRTOTUNNUS = "siirtoTunnus";
  /** Property organisaationNimi */
  final String PROPERTY_ORGANISAATIONNIMI = "organisaationNimi";
  /** Property lukuvuosi */
  final String PROPERTY_DOKUMENTTI_ID = "dokumenttiId";
}
