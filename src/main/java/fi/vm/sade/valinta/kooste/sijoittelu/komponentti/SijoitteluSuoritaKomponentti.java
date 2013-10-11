package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import javax.annotation.Resource;

import org.apache.camel.Property;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.sijoittelu.SijoitteluService;
import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;

@Component("sijoitteluSuoritaKomponentti")
public class SijoitteluSuoritaKomponentti {

    @Resource(name = "sijoitteluService")
    private SijoitteluService sijoitteluService;

    public void suorita(@Property("hakutyyppi") HakuTyyppi hakutyyppi) {
        sijoitteluService.sijoittele(hakutyyppi);
    }
}
