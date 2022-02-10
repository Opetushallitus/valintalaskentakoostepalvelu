package fi.vm.sade.valinta.kooste.viestintapalvelu.dto;

import fi.vm.sade.valinta.kooste.util.KieliUtil;
import java.util.List;

/**
 * Wrapperi sijoittelun HakutoiveDTO:lle jossa meta dataa liittyen kaikkiin hakukohteen hakijoihin
 */
public class MetaHakukohde {
  private final Teksti hakukohdeNimi;
  private final List<Teksti> tarjoajaNimet;
  private final String hakukohteenKieli;
  private final String opetuskieli;
  private final String tarjoajaOid;
  private final Teksti ohjeetUudelleOpiskelijalle;

  public MetaHakukohde(String tarjoajaOid, Teksti hakukohdeNimi, List<Teksti> tarjoajaNimet) {
    this(tarjoajaOid, hakukohdeNimi, tarjoajaNimet, null, null, null);
  }

  public MetaHakukohde(
      String tarjoajaOid,
      Teksti hakukohdeNimi,
      List<Teksti> tarjoajaNimet,
      String hakukohteenKieli,
      String opetuskieli,
      Teksti ohjeetUudelleOpiskelijalle) {
    this.hakukohdeNimi = hakukohdeNimi;
    this.tarjoajaNimet = tarjoajaNimet;
    this.hakukohteenKieli = hakukohteenKieli;
    this.opetuskieli = opetuskieli;
    this.tarjoajaOid = tarjoajaOid;
    this.ohjeetUudelleOpiskelijalle = ohjeetUudelleOpiskelijalle;
  }

  public MetaHakukohde(
      String tarjoajaOid,
      Teksti hakukohdeNimi,
      List<Teksti> tarjoajaNimet,
      String hakukohteenKieli) {
    this(tarjoajaOid, hakukohdeNimi, tarjoajaNimet, hakukohteenKieli, null, null);
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

  public Teksti getOhjeetUudelleOpiskelijalle() {
    return ohjeetUudelleOpiskelijalle;
  }

  public Teksti getHakukohdeNimi() {
    return hakukohdeNimi;
  }

  public List<Teksti> getTarjoajaNimet() {
    return tarjoajaNimet;
  }
}
