package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * @author Jussi Jartamo
 *         <p/>
 *         Portti(proxy), jonka Camel toteuttaa, valintalaskennan reitityksen
 *         käynnistykseen
 */
public interface HakukohteenValintalaskentaRoute {

    /**
     * Camel route description.
     */
    String DIRECT_VALINTALASKENTA_HAKUKOHTEELLE = "direct:valintalaskenta_hakukohteelle";

    void aktivoiValintalaskenta(@Property(OPH.HAKUKOHDEOID) String hakukohdeOid,
            @Property(OPH.VALINNANVAIHE) Integer valinnanvaihe);

}
