package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import fi.vm.sade.tarjonta.service.resources.HakuResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("hakuTarjonnaltaKomponentti")
public class HaeHakuTarjonnaltaKomponentti {

    @Autowired
    private HakuResource hakuResource;

    public HakuDTO getHaku(@Property("hakuOid") String hakuOid) {
        HakuDTO haku = hakuResource.getByOID(hakuOid);
        return haku;
    }
}
