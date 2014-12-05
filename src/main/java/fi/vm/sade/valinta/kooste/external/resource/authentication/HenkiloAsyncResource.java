package fi.vm.sade.valinta.kooste.external.resource.authentication;

import java.util.List;
import java.util.concurrent.Future;

import fi.vm.sade.authentication.model.Henkilo;
import fi.vm.sade.valinta.kooste.external.resource.authentication.dto.HenkiloCreateDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public interface HenkiloAsyncResource {
    /*
	@Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @JsonView(JsonViews.Basic.class)
    @Secured(CRUD)
    @POST
    public Henkilo createHenkilo(Henkilo henkilo) {
        henkilo.setOidHenkilo(null);
        henkilo.setOppijanumero(null);
        return userManagementBusinessService.addHenkilo(henkilo);
    }
    */
	Future<List<Henkilo>> haeHenkilot(List<HenkiloCreateDTO> henkiloPrototyypit);
}
