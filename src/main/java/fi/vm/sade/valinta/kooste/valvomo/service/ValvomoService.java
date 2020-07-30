package fi.vm.sade.valinta.kooste.valvomo.service;

import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import java.util.Collection;

public interface ValvomoService<T> {
  ProsessiJaStatus<T> getAjossaOlevaProsessiJaStatus();

  ProsessiJaStatus<T> getProsessiJaStatus(String uuid);

  Collection<T> getUusimmatProsessit();

  Collection<ProsessiJaStatus<T>> getUusimmatProsessitJaStatukset();
}
