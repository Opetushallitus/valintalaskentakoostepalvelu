package fi.vm.sade.valinta.kooste.pistesyotto.excel;

import static java.util.Arrays.*;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.excel.DataRivi;
import fi.vm.sade.valinta.kooste.excel.Rivi;
import fi.vm.sade.valinta.kooste.excel.arvo.Arvo;
import java.util.Collection;

public class PistesyottoDataRivi extends DataRivi {

  private final Collection<PistesyottoDataRiviKuuntelija> kuuntelijat;
  private final Collection<PistesyottoDataArvo> dataArvot;

  public PistesyottoDataRivi(
      Collection<Collection<Arvo>> arvot,
      Collection<PistesyottoDataRiviKuuntelija> kuuntelijat,
      Collection<PistesyottoDataArvo> dataArvot) {
    super(arvot);
    this.kuuntelijat = kuuntelijat;
    this.dataArvot = dataArvot;
  }

  @Override
  public boolean validoi(Rivi rivi) {
    String oid = rivi.getArvoAt(0);
    String nimi = rivi.getArvoAt(1);
    String hetu = rivi.getArvoAt(2);
    String pvm = rivi.getArvoAt(3);
    final boolean isEmptyRow = asList(oid, nimi, hetu, pvm).stream().allMatch(String::isEmpty);
    if (isEmptyRow) {
      return true;
    }
    Collection<PistesyottoArvo> arvot = Lists.newArrayList();
    {
      int i = 4;
      for (PistesyottoDataArvo dataArvo : dataArvot) {
        arvot.add(dataArvo.asPistesyottoArvo(rivi.getArvoAt(i), rivi.getArvoAt(i + 1)));
        i += 2;
      }
    }
    PistesyottoRivi pistesyottorivi = new PistesyottoRivi(oid, nimi, hetu, pvm, arvot);
    for (PistesyottoDataRiviKuuntelija kuuntelija : kuuntelijat) {
      kuuntelija.pistesyottoDataRiviTapahtuma(pistesyottorivi);
    }
    return true;
  }
}
