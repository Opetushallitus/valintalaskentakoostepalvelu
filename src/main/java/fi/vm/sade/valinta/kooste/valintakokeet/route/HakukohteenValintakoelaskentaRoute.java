package fi.vm.sade.valinta.kooste.valintakokeet.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * User: wuoti Date: 9.9.2013 Time: 9.22
 */
public interface HakukohteenValintakoelaskentaRoute {
    final String DIRECT_HAKUKOHTEEN_VALINTAKOELASKENTA = "direct:kaynnistaHakukohteenValintakoelaskentaReitti";

    void aktivoiValintakoelaskenta(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid);
}
