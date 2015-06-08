package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import akka.actor.ActorRef;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface LaskentaSupervisor extends ValintalaskentaKerrallaRouteValvomo {
	void ready(String uuid);
	void fetchAndStartLaskenta();
}
