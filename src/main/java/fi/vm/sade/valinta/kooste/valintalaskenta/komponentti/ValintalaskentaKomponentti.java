package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
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
    private ValintalaskentaService valintalaskentaService;

    @Autowired
    private ValintalaskentaResource valintalaskentaResource;

    public void laske(@Property("hakemustyypit") List<HakemusTyyppi> hakemustyypit,
            @Property("valintaperusteet") List<ValintaperusteetTyyppi> valintaperusteet) {
        valintalaskentaService.laske(hakemustyypit, valintaperusteet);
    }

    public void laskeRest(@Property("hakemustyypit") List<HakemusDTO> hakemustyypit,
                      @Property("valintaperusteet") List<ValintaperusteetDTO> valintaperusteet) {

        LaskeDTO laskeDTO =new LaskeDTO();
        laskeDTO.setHakemus(hakemustyypit);
        laskeDTO.setValintaperuste(valintaperusteet);

        valintalaskentaResource.laske(laskeDTO);
    }
}
