package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("sijoitteluIlmankoulutuspaikkaaKomponentti")
public class SijoitteluIlmankoulutuspaikkaaKomponentti {

    @Autowired
    private SijoitteluResource sijoitteluResource;

    @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}")
    private String sijoitteluResourceUrl;

    public List<HakijaDTO> ilmankoulutuspaikkaa(@Property("hakuOid") String hakuOid,
            @Property("sijoitteluajoId") String sijoitteluajoId) {
        return sijoitteluResource.ilmanhyvaksyntaa(hakuOid, sijoitteluajoId);
    }
}
