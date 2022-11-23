package fi.vm.sade.valinta.kooste.kela.route;

import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import java.util.concurrent.atomic.AtomicBoolean;

public interface KelaLuontiRoute {

  void kelaLuonti(KelaLuonti kelaLuonti, AtomicBoolean lopetusehto);
}
