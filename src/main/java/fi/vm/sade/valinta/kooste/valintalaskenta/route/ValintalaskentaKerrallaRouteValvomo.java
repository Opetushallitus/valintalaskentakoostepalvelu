package fi.vm.sade.valinta.kooste.valintalaskenta.route;

import java.util.List;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;

/**
 * @author Jussi Jartamo.
 */
public interface ValintalaskentaKerrallaRouteValvomo {

	Laskenta haeLaskenta(String uuid);

	List<Laskenta> ajossaOlevatLaskennat();
}
