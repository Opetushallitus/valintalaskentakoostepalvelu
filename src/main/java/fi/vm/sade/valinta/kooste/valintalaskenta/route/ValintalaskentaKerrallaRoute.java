package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;

/**
 * @author Jussi Jartamo.
 */
public interface ValintalaskentaKerrallaRoute {

	void suoritaValintalaskentaKerralla(LaskentaAloitus laskentaAloitus);
}
