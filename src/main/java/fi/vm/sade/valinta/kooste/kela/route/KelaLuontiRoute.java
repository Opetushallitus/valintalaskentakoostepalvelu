package fi.vm.sade.valinta.kooste.kela.route;

import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.camel.Body;
import org.apache.camel.InOnly;
import org.apache.camel.Property;

public interface KelaLuontiRoute {
  static final String LOPETUSEHTO = "lopetusehto";
  static final String SEDA_KELA_LUONTI = "direct:kela_luonti";

  @InOnly
  void kelaLuonti(@Body KelaLuonti kelaLuonti, @Property(LOPETUSEHTO) AtomicBoolean lopetusehto);
}
