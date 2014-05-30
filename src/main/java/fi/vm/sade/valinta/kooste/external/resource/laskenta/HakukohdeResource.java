package fi.vm.sade.valinta.kooste.external.resource.laskenta;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;

@Path("/valintalaskenta-laskenta-service/resources/hakukohde")
public interface HakukohdeResource {

	@GET
	@Path("{hakukohdeoid}/valinnanvaihe")
	@Produces(MediaType.APPLICATION_JSON)
	List<ValinnanvaiheDTO> hakukohde(
			@PathParam("hakukohdeoid") String hakukohdeoid);
}
