package fi.vm.sade.valinta.kooste.kela.komponentti;

import java.util.Date;

import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;

public interface PaivamaaraSource {

	Date lukuvuosi(HakuDTO haku);

	Date poimintapaivamaara(HakuDTO haku);

	Date valintapaivamaara(HakuDTO haku);
}
