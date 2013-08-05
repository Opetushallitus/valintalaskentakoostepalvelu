package fi.vm.sade.valinta.kooste.paasykokeet.komponentti.proxy;

import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

import java.util.List;
import java.util.Set;

/**
 * User: wuoti
 * Date: 5.8.2013
 * Time: 12.58
 */
public interface HakukohteenValintaperusteetProxy {
    List<ValintaperusteetTyyppi> haeValintaperusteet(Set<String> hakukohdeOids);

    List<ValintaperusteetTyyppi> haeValintaperusteet(String hakukohdeOid);
}
