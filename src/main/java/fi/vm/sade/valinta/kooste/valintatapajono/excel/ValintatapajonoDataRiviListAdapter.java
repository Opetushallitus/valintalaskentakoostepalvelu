package fi.vm.sade.valinta.kooste.valintatapajono.excel;

import com.google.common.collect.Lists;
import java.util.List;

public class ValintatapajonoDataRiviListAdapter implements ValintatapajonoDataRiviKuuntelija {
  private final List<ValintatapajonoRivi> rivit = Lists.newArrayList();

  @Override
  public void valintatapajonoDataRiviTapahtuma(ValintatapajonoRivi valintatapajonoRivi) {
    rivit.add(valintatapajonoRivi);
  }

  public List<ValintatapajonoRivi> getRivit() {
    return rivit;
  }
}
