package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import java.util.List;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;

@Component("sijoitteluIlmankoulutuspaikkaaKomponentti")
public class SijoitteluIlmankoulutuspaikkaaKomponentti {

    private SijoitteluResource sijoitteluResource;
    private String sijoitteluResourceUrl;

    @Autowired
    public SijoitteluIlmankoulutuspaikkaaKomponentti(SijoitteluResource sijoitteluResource,
            @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}") String sijoitteluResourceUrl) {
        this.sijoitteluResource = sijoitteluResource;
        this.sijoitteluResourceUrl = sijoitteluResourceUrl;
    }

    public List<HakijaDTO> ilmankoulutuspaikkaa(@Property("hakuOid") String hakuOid,
            @Property("sijoitteluajoId") String sijoitteluajoId) {

        final HakijaPaginationObject result = sijoitteluResource.hakemukset(hakuOid, SijoitteluResource.LATEST, null,
                true, null, null, null, null);
        return result.getResults();
    }
}
