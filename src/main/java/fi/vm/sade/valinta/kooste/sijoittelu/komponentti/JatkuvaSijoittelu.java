package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import java.util.Collection;

public interface JatkuvaSijoittelu {
  Collection<DelayedSijoittelu> haeJonossaOlevatSijoittelut();

  void teeJatkuvaSijoittelu();

  void kaynnistaJatkuvaSijoittelu(DelayedSijoittelu sijoittelu);
}
