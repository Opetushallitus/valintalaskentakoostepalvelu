package fi.vm.sade.valinta.kooste.kela.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.AbstractHakukohde;

public interface HakukohdeSource {
  AbstractHakukohde getHakukohdeByOid(String oid);
}
