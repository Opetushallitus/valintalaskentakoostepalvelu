package fi.vm.sade.valinta.kooste.sijoittelu.komponentti;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaPaginationObject;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component("sijoitteluKoulutuspaikkallisetKomponentti")
public class SijoitteluKoulutuspaikkallisetKomponentti {

    private SijoitteluResource sijoitteluResource;
    private String sijoitteluResourceUrl;

    @Autowired
    public SijoitteluKoulutuspaikkallisetKomponentti(SijoitteluResource sijoitteluResource,
            @Value("${valintalaskentakoostepalvelu.sijoittelu.rest.url}") String sijoitteluResourceUrl) {
        this.sijoitteluResource = sijoitteluResource;
        this.sijoitteluResourceUrl = sijoitteluResourceUrl;
    }

    public Collection<HakijaDTO> koulutuspaikalliset(@Property("hakuOid") String hakuOid,
            @Property("hakukohdeOid") String hakukohdeOid, @Property("sijoitteluajoId") String sijoitteluajoId) {
        List<String> hakukohteet = new ArrayList<String>();
        hakukohteet.add(hakukohdeOid);
        final HakijaPaginationObject result = sijoitteluResource.hakemukset(hakuOid, SijoitteluResource.LATEST, true,
                null, null, hakukohteet, null, null);
        return result.getResults();
    }

}
