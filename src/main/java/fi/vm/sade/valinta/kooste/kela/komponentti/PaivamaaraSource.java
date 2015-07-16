package fi.vm.sade.valinta.kooste.kela.komponentti;

import java.util.Date;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;

public interface PaivamaaraSource {

    Date lukuvuosi(HakuV1RDTO haku, String hakukohdeOid);

    Date poimintapaivamaara(HakuV1RDTO haku);

    Date valintapaivamaara(HakuV1RDTO haku);
}
