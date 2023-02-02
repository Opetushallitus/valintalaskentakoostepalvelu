package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.AjastettuSijoitteluInfo;
import java.util.List;

public interface JatkuvaSijoittelu {

  List<AjastettuSijoitteluInfo> haeAjossaOlevatAjastetutSijoittelut();

  void teeJatkuvaSijoittelu();
}
