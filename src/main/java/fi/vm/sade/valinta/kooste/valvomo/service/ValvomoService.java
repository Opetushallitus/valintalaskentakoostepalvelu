package fi.vm.sade.valinta.kooste.valvomo.service;

import java.util.Collection;

import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValvomoService<T> {

	/**
	 * @return null if idle
	 */
	ProsessiJaStatus<T> getAjossaOlevaProsessiJaStatus();

	ProsessiJaStatus<T> getProsessiJaStatus(String uuid);

	Collection<T> getUusimmatProsessit();

	Collection<ProsessiJaStatus<T>> getUusimmatProsessitJaStatukset();

}
