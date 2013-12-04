package fi.vm.sade.valinta.kooste.valvomo.service;

import java.util.Collection;

import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface ValvomoService<T> {

    Collection<T> getUusimmatProsessit();

    Collection<ProsessiJaStatus<T>> getUusimmatProsessitJaStatukset();

}
