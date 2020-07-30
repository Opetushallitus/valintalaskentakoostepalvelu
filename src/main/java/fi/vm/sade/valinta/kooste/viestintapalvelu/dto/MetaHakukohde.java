package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.valinta.kooste.util.KieliUtil;

/**
 * Wrapperi sijoittelun HakutoiveDTO:lle jossa meta dataa liittyen kaikkiin hakukohteen hakijoihin
 */
public class MetaHakukohde {
  private final Teksti hakukohdeNimi;
  private final Teksti tarjoajaNimi;
  private final String hakukohteenKieli;
  private final String opetuskieli;
  private final String tarjoajaOid;
  private final String ohjeetUudelleOpiskelijalle;

  public MetaHakukohde(String tarjoajaOid, Teksti hakukohdeNimi, Teksti tarjoajaNimi) {
    this(tarjoajaOid, hakukohdeNimi, tarjoajaNimi, null, null, null);
  }

  public MetaHakukohde(
      String tarjoajaOid,
      Teksti hakukohdeNimi,
      Teksti tarjoajaNimi,
      String hakukohteenKieli,
      String opetuskieli,
      String ohjeetUudelleOpiskelijalle) {
    this.hakukohdeNimi = hakukohdeNimi;
    this.tarjoajaNimi = tarjoajaNimi;
    this.hakukohteenKieli = hakukohteenKieli;
    this.opetuskieli = opetuskieli;
    this.tarjoajaOid = tarjoajaOid;
    this.ohjeetUudelleOpiskelijalle = ohjeetUudelleOpiskelijalle;
  }

  public MetaHakukohde(
      String tarjoajaOid, Teksti hakukohdeNimi, Teksti tarjoajaNimi, String hakukohteenKieli) {
    this(tarjoajaOid, hakukohdeNimi, tarjoajaNimi, hakukohteenKieli, null, null);
  }

  public String getTarjoajaOid() {
    return tarjoajaOid;
  }

  public String getOpetuskieli() {
    if (opetuskieli == null) {
      return getHakukohteenKieli();
    }
    return opetuskieli;
  }

  public String getHakukohteenKieli() {
    if (hakukohteenKieli == null) {
      if (hakukohdeNimi == null) {
        return KieliUtil.SUOMI;
      }
      return getHakukohdeNimi().getKieli();
    }
    return hakukohteenKieli;
  }

  public String getHakukohteenNonEmptyKieli() {
    if (hakukohteenKieli == null) {
      if (hakukohdeNimi == null) {
        return KieliUtil.SUOMI;
      }
      return getHakukohdeNimi().getNonEmptyKieli();
    }
    return hakukohteenKieli;
  }

  public String getOhjeetUudelleOpiskelijalle() {
    return ohjeetUudelleOpiskelijalle;
  }

  public Teksti getHakukohdeNimi() {
    return hakukohdeNimi;
  }

  public Teksti getTarjoajaNimi() {
    return tarjoajaNimi;
  }
}
