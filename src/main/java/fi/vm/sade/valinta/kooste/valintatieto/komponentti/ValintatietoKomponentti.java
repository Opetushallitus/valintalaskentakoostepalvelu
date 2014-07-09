package fi.vm.sade.valinta.kooste.valintatieto.komponentti;

import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintatietoResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakuDTO;
import org.apache.camel.language.Simple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("valintatietokomponentti")
public class ValintatietoKomponentti {

    @Autowired
    private ValintatietoResource valintatietoService;

    public HakuDTO valintatieto(@Simple("${property.hakuOid}") String hakuOid) {
        return valintatietoService.haeValintatiedot(hakuOid);
    }
}
