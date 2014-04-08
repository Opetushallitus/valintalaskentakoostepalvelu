package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;

public interface HakukohdeSource {

	HakukohdeDTO getHakukohdeByOid(String oid);
}
