package fi.vm.sade.valinta.kooste.kela.route;

import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

public interface KelaRoute {

  /** Aloittaa Kela-siirtodokumentin luonnin. */
  void aloitaKelaLuonti(
      //@Property(ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI)
      KelaProsessi prosessi,
      //@Body
      KelaLuonti luonti);

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
