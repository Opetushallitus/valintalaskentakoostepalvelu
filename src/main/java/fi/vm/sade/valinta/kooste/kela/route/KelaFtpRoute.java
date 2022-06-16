package fi.vm.sade.valinta.kooste.kela.route;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Taman voisi yhdistaa KelaRouteen kun proxyyn tekisi produce annotaatiotuen.
 */
public interface KelaFtpRoute {
  Boolean aloitaKelaSiirto(String dokumenttiId) throws InterruptedException, ExecutionException, TimeoutException;
}
