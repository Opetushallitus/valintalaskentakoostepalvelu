package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("valintaperusteKomponentti")
public class ValintaperusteKomponentti {

    @Autowired
    private ValintaperusteetRestResource valintaperusteetResource;

    public List<ValintaperusteetDTO> haeValintaperusteet(
            @Property("hakukohdeOid") String hakukohdeOid, @Property("vaihe") Integer vaihe) {
        return valintaperusteetResource.haeValintaperusteet(hakukohdeOid, vaihe);
    }
}
