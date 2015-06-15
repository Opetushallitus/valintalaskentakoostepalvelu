package fi.vm.sade.valinta.kooste.valvomo.service;

import java.util.Collection;

import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;

public interface ValvomoService<T> {
    ProsessiJaStatus<T> getAjossaOlevaProsessiJaStatus();

    ProsessiJaStatus<T> getProsessiJaStatus(String uuid);

    Collection<T> getUusimmatProsessit();

    Collection<ProsessiJaStatus<T>> getUusimmatProsessitJaStatukset();
}
