package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintalaskentaResource;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("valintalaskentaKomponentti")
public class ValintalaskentaKomponentti {

    @Autowired
    private ValintalaskentaResource valintalaskentaResource;

    public void laskeRest(@Property("hakemustyypit") List<HakemusDTO> hakemustyypit,
                      @Property("valintaperusteet") List<ValintaperusteetDTO> valintaperusteet) {

        LaskeDTO laskeDTO =new LaskeDTO();
        laskeDTO.setHakemus(hakemustyypit);
        laskeDTO.setValintaperuste(valintaperusteet);

        valintalaskentaResource.laske(laskeDTO);
    }
}
