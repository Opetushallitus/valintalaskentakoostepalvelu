package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import java.util.Date;

public interface PaivamaaraSource {

  Date lukuvuosi(Haku haku, String hakukohdeOid);

  Date poimintapaivamaara(Haku haku);

  Date valintapaivamaara(Haku haku);
}
