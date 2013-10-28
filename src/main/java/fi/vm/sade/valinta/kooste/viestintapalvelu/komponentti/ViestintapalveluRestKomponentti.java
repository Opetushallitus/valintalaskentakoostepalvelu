package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.viestintapalvelu.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Valttamaton epasuoruus viestintapalveluresurssin kutsuun koska Camel
 *         ei osaa kayttaa rest rajapintaa bean-endpointtina
 */
@Component("viestintapalveluRestKomponentti")
public class ViestintapalveluRestKomponentti {

    @Autowired
    private ViestintapalveluResource viestintapalveluResource;

    public Response haeOsoitetarrat(Osoitteet osoitteet) {
        return viestintapalveluResource.haeOsoitetarrat(osoitteet);
    }

    public Response haeJalkiohjauskirjeet(Kirjeet kirjeet) {
        return viestintapalveluResource.haeJalkiohjauskirjeet(kirjeet);
    }

    public Response haeHyvaksymiskirjeet(Kirjeet kirjeet) {
        return viestintapalveluResource.haeHyvaksymiskirjeet(kirjeet);
    }
}
