package fi.vm.sade.valinta.kooste.sijoittelu.proxy;

import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;

public interface SijoitteluSuoritaProxy {

    void suorita(HakuTyyppi hakutyyppi);
}
