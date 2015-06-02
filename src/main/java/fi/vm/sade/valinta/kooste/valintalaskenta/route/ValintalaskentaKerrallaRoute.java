package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;

/**
 * @author Jussi Jartamo.
 */
public interface ValintalaskentaKerrallaRoute {

	void suoritaValintalaskentaKerralla(
			final HakuV1RDTO haku,
			final ParametritDTO parametritDTO,
			LaskentaAloitus laskentaAloitus);
}
