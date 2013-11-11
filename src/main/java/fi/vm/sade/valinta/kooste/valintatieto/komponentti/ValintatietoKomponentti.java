package fi.vm.sade.valinta.kooste.valintatieto.komponentti;

import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.service.valintatiedot.schema.HakuTyyppi;
import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("valintatietokomponentti")
public class ValintatietoKomponentti {

    @Autowired
    private ValintatietoService valintatietoService;

    public HakuTyyppi valintatieto(@Simple("${property.hakuOid}") String hakuOid) {
        return valintatietoService.haeValintatiedot(hakuOid);
    }
}
