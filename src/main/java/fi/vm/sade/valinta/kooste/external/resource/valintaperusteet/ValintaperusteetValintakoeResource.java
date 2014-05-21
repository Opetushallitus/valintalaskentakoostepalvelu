package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Path("/valintaperusteet-service/resources/valintakoe")
public interface ValintaperusteetValintakoeResource {

	@GET
	@Path("/{oid}")
	@Produces(MediaType.APPLICATION_JSON)
	ValintakoeDTO readByOid(@PathParam("oid") String oid);
}
