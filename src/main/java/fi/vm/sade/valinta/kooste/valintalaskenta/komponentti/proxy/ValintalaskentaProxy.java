package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import java.util.List;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Retry valintalaskentaan
 */
public interface ValintalaskentaProxy {

    void laske(List<HakemusTyyppi> hakemustyypit, List<ValintaperusteetTyyppi> valintaperusteet);
}
