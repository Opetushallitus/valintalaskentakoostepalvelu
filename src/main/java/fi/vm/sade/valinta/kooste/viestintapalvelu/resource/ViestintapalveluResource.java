package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Koekutsukirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;

@Path("/api/v1")
public interface ViestintapalveluResource {

	@POST
	@Produces(APPLICATION_OCTET_STREAM)
	@Consumes(APPLICATION_JSON)
	@Path("/addresslabel/sync/pdf")
	InputStream haeOsoitetarratSync(Osoitteet osoitteet);

	@GET
	@Produces(APPLICATION_JSON)
	@Path("/template/getHistory")
	List<TemplateHistory> haeKirjepohja(@QueryParam("oid") String oid,
			@QueryParam("templateName") String templateName,
			@QueryParam("languageCode") String languageCode,
			@QueryParam("tag") String tag);

}
