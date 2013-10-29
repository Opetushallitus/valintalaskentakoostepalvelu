package fi.vm.sade.valinta.kooste.valintalaskenta.komponentti;

import java.util.List;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("valintalaskentaKomponentti")
public class ValintalaskentaKomponentti {

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    public void laske(@Property("hakemustyypit") List<HakemusTyyppi> hakemustyypit,
            @Property("valintaperusteet") List<ValintaperusteetTyyppi> valintaperusteet) {
        valintalaskentaService.laske(hakemustyypit, valintaperusteet);
    }
}
