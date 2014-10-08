package fi.vm.sade.valinta.kooste.valintalaskenta.actor;

import java.util.function.Function;

import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface LaskentaSupervisor extends ValintalaskentaKerrallaRouteValvomo {

	void valmis(String uuid);

	void luoJaKaynnistaLaskenta(String uuid, String hakuOid,
			boolean osittainen,
			Function<LaskentaSupervisor, LaskentaActor> laskentaProducer);
}
