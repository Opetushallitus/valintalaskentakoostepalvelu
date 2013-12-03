package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import java.util.Collection;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.PaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;

@Component("sijoitteluKaikkiPaikanVastaanottaneetKomponentti")
public class SijoitteluKaikkiPaikanVastaanottaneetKomponentti implements SijoitteluKaikkiPaikanVastaanottaneet {

    @Autowired
    private SijoitteluResource sijoitteluResource;

    @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}")
    private String sijoitteluResourceUrl;

    public Collection<HakijaDTO> vastaanottaneet(@Property("hakuOid") String hakuOid) {
        final PaginationObject<HakijaDTO> result = sijoitteluResource.hakemukset(hakuOid, SijoitteluResource.LATEST,
                null, null, true, null, null, null);
        return result.getResults();
    }

}
