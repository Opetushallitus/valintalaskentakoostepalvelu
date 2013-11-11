package fi.vm.sade.valinta.kooste.valintakokeet.komponentti;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("valintakoelaskentakomponentti")
public class ValintakoelaskentaKomponentti {

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    public void valintakokeet(@Property("hakemustyyppi") HakemusTyyppi hakemustyyppi,
            @Property("valintaperusteet") List<ValintaperusteetTyyppi> valintaperusteet) {
        valintalaskentaService.valintakokeet(hakemustyyppi, valintaperusteet);
    }
}
