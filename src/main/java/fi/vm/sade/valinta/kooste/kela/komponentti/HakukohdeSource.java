package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Hakukohde;

public interface HakukohdeSource {
  Hakukohde getHakukohdeByOid(String oid);
}
