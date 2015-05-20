package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;

/**
 * @author Jussi Jartamo.
 */
public interface ValintalaskentaKerrallaRoute {

	void suoritaValintalaskentaKerralla(final ParametritDTO parametritDTO,LaskentaStartParams laskentaStartParams);
	void workAvailable();
}
