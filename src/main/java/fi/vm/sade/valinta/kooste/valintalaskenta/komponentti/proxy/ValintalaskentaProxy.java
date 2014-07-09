package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Retry valintalaskentaan
 */
public interface ValintalaskentaProxy {

    void laske(List<HakemusDTO> hakemustyypit, List<ValintaperusteetDTO> valintaperusteet);
}
