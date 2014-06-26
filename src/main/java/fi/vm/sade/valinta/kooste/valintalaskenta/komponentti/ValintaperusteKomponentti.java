package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.dto.HakuparametritDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("valintaperusteKomponentti")
public class ValintaperusteKomponentti {

    @Autowired
    private ValintaperusteService valintaperusteService;

    @Autowired
    private ValintaperusteetRestResource valintaperusteetResource;

    public List<ValintaperusteetTyyppi> haeValintaperusteet(
            @Property("hakuparametrit") List<HakuparametritTyyppi> hakuparametrit) {
        return valintaperusteService.haeValintaperusteet(hakuparametrit);
    }

    public List<ValintaperusteetDTO> haeValintaperusteetRest(
            @Property("hakukohdeOid") String hakukohdeOid, @Property("vaihe") Integer vaihe) {
        return valintaperusteetResource.haeValintaperusteet(hakukohdeOid, vaihe);
    }
}
