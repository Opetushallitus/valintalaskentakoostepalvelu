package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface LaskentaSupervisor extends ValintalaskentaKerrallaRouteValvomo {
	void valmis(String uuid);
	void haeJaKaynnistaLaskenta();
}
