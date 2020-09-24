package fi.vm.sade.valinta.kooste.kela.dto;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import java.util.List;

public class KelaLuontiJaHaut {
  private final KelaLuonti luonti;
  private final List<Haku> haut;

  public KelaLuontiJaHaut(KelaLuonti kelaLuonti, List<Haku> haut) {
    this.luonti = kelaLuonti;
    this.haut = haut;
  }

  public List<Haku> getHaut() {
    return haut;
  }

  public KelaLuonti getLuonti() {
    return luonti;
  }
}
