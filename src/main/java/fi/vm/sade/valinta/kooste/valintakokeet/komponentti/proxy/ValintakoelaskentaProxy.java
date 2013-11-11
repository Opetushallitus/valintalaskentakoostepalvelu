package fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

import java.util.List;

public interface ValintakoelaskentaProxy {

    void valintakokeet(HakemusTyyppi hakemustyyppi, List<ValintaperusteetTyyppi> valintaperusteet);

}
