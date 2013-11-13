package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import java.util.Collection;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;

@Component("sijoitteluKaikkiKoulutuspaikkallisetKomponentti")
public class SijoitteluKaikkiKoulutuspaikkallisetKomponentti {

    @Autowired
    private SijoitteluResource sijoitteluResource;

    @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}")
    private String sijoitteluResourceUrl;

    public Collection<HakijaDTO> ilmankoulutuspaikkaa(@Property("hakuOid") String hakuOid) {
        return sijoitteluResource.koulutuspaikalliset(hakuOid, SijoitteluResource.LATEST);
    }

}
