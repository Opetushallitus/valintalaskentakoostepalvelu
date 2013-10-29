package fi.vm.sade.valinta.kooste.tarjonta.komponentti;

import org.apache.camel.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.organisaatio.api.model.OrganisaatioService;
import fi.vm.sade.organisaatio.api.model.types.OrganisaatioDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Wrappaa organisaatiokutsut erillisen reitin taakse niin etta
 */
@Component("organisaatioKomponentti")
public class OrganisaatioKomponentti {

    @Autowired
    private OrganisaatioService organisaatioService;

    public OrganisaatioDTO haeOrganisaatio(@Property("tarjoajaOid") String tarjoajaOid) {
        return organisaatioService.findByOid(tarjoajaOid);
    }
}
