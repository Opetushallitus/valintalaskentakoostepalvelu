package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.service.sijoittelu.SijoitteluService;
import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.SijoitteluResource;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("sijoitteluSuoritaKomponentti")
public class SijoitteluSuoritaKomponentti {

    @Resource(name = "sijoitteluService")
    private SijoitteluService sijoitteluService;

    @Autowired
    private SijoitteluResource sijoitteluResource;

    public void suorita(@Property("hakutyyppi") HakuTyyppi hakutyyppi) {
        sijoitteluService.sijoittele(hakutyyppi);
    }

    public void sijottele(@Property(OPH.HAKUOID) String hakuOid) {
        sijoitteluResource.sijoittele(hakuOid);
    }
}
