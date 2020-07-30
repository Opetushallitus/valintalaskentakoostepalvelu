package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import com.google.common.collect.Lists;
import java.util.Collection;

public class PistesyottoDataRiviListAdapter implements PistesyottoDataRiviKuuntelija {
  private final Collection<PistesyottoRivi> rivit;

  public PistesyottoDataRiviListAdapter() {
    this.rivit = Lists.newArrayList();
  }

  @Override
  public void pistesyottoDataRiviTapahtuma(PistesyottoRivi pistesyottoRivi) {
    rivit.add(pistesyottoRivi);
  }

  public Collection<PistesyottoRivi> getRivit() {
    return rivit;
  }
}
