package fi.vm.sade.valinta.kooste.external.resource.valintaperusteet;

import java.util.List;
import java.util.concurrent.Future;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;

public interface ValintaperusteetAvaimetAsyncResource {
	// @GET
	// @Path("/avaimet/{oid}")
	// @Produces(MediaType.APPLICATION_JSON)
	// List<ValintaperusteDTO> findAvaimet(@PathParam("oid") String oid);
	Future<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid);

}
