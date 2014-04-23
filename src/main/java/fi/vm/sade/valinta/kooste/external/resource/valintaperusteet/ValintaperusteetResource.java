package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Path("/valintaperusteet-service/resources/hakukohde")
public interface ValintaperusteetResource {
	@GET
	@Path("/avaimet/{oid}")
	@Produces(MediaType.APPLICATION_JSON)
	List<ValintaperusteDTO> findAvaimet(@PathParam("oid") String oid);

}
