package fi.vm.sade.valinta.kooste.valintatieto.komponentti.proxy;

import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;

public interface ValintatietoProxy {

    HakuTyyppi haeValintatiedot(String hakuOid);
}
