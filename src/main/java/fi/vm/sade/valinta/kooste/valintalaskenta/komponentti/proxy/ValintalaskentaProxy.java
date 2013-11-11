package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Retry valintalaskentaan
 */
public interface ValintalaskentaProxy {

    void laske(List<HakemusTyyppi> hakemustyypit, List<ValintaperusteetTyyppi> valintaperusteet);
}
