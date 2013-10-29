package fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy;

import java.util.List;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

public interface ValintakoelaskentaProxy {

    void valintakokeet(HakemusTyyppi hakemustyyppi, List<ValintaperusteetTyyppi> valintaperusteet);

}
