package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.service.sijoittelu.SijoitteluService;
import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;
import org.apache.camel.Property;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("sijoitteluSuoritaKomponentti")
public class SijoitteluSuoritaKomponentti {

    @Resource(name = "sijoitteluService")
    private SijoitteluService sijoitteluService;

    public void suorita(@Property("hakutyyppi") HakuTyyppi hakutyyppi) {
        sijoitteluService.sijoittele(hakutyyppi);
    }
}
