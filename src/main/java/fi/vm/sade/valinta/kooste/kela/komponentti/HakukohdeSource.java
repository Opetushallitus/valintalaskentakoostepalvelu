package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;

public interface HakukohdeSource {

  HakukohdeV1RDTO getHakukohdeByOid(String oid);
}
