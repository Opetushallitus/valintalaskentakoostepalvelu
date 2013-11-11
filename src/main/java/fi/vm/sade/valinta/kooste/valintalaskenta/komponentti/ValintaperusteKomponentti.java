package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("valintaperusteKomponentti")
public class ValintaperusteKomponentti {

    @Autowired
    private ValintaperusteService valintaperusteService;

    public List<ValintaperusteetTyyppi> haeValintaperusteet(
            @Property("hakuparametrit") List<HakuparametritTyyppi> hakuparametrit) {
        return valintaperusteService.haeValintaperusteet(hakuparametrit);
    }
}
