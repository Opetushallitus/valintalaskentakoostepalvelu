package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import fi.vm.sade.organisaatio.resource.dto.OrganisaatioRDTO;
import fi.vm.sade.valinta.kooste.tarjonta.OrganisaatioResource;
import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Wrappaa organisaatiokutsut erillisen reitin taakse niin etta
 */
@Component("organisaatioKomponentti")
public class OrganisaatioKomponentti {

    @Autowired
    // fi.vm.sade.valinta.kooste.tarjonta.
    private OrganisaatioResource organisaatioResource;

    public OrganisaatioRDTO haeOrganisaatio(@Property("tarjoajaOid") String tarjoajaOid) {
        // OrganisaatioResourceClient
        OrganisaatioRDTO o = organisaatioResource.getOrganisaatioByOID(tarjoajaOid);
        return o; // organisaatioService.findByOid(tarjoajaOid);
    }
}
