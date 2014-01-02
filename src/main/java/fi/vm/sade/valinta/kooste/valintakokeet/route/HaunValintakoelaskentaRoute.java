package fi.vm.sade.valinta.kooste.valintakokeet.route;

import java.util.concurrent.Future;

import org.apache.camel.Property;

import fi.vm.sade.valinta.kooste.OPH;

/**
 * @author Jussi Jartamo
 */
public interface HaunValintakoelaskentaRoute {
    final String DIRECT_HAUN_VALINTAKOELASKENTA = "direct:kaynnistaHaunValintakoelaskentaReitti";

    void aktivoiValintakoelaskenta(@Property(OPH.HAKUOID) String hakuOid);

    Future<Void> aktivoiValintakoelaskentaAsync(@Property(OPH.HAKUOID) String hakuOid);
}
