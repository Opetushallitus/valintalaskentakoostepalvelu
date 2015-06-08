package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import java.util.List;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;

/**
 * @author Jussi Jartamo.
 */
public interface ValintalaskentaKerrallaRouteValvomo {

	Laskenta fetchLaskenta(String uuid);

	List<Laskenta> runningLaskentas();
}
