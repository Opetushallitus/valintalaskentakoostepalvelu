package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component("sijoitteluKoulutuspaikkallisetKomponentti")
public class SijoitteluKoulutuspaikkallisetKomponentti {

    @Autowired
    private SijoitteluResource sijoitteluResource;

    @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}")
    private String sijoitteluResourceUrl;

    public Collection<HakijaDTO> ilmankoulutuspaikkaa(@Property("hakuOid") String hakuOid,
                                                      @Property("hakukohdeOid") String hakukohdeOid,
                                                      @Property("sijoitteluajoId") String sijoitteluajoId) {
        List<String> hakukohteet = new ArrayList<String>();
        hakukohteet.add(hakukohdeOid);
        final HakijaPaginationObject result =sijoitteluResource.hakemukset(hakuOid,
                SijoitteluResource.LATEST,
                true,
                null,
                null,
                hakukohteet,
                null,
                null);
        return result.getResults();
    }

}
