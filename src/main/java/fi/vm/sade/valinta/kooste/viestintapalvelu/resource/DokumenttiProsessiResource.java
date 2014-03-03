package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Palauttaa prosessoidut dokumentit resursseina
 */
@Controller
@Path("dokumenttiprosessi")
@PreAuthorize("isAuthenticated()")
@Api(value = "/dokumenttiprosessi", description = "Dokumenttien luontiin liittyvää palautetta käyttäjälle")
public class DokumenttiProsessiResource {

	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	@GET
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Palauttaa dokumenttiprosessin id:lle jos sellainen on muistissa", response = Response.class)
	public DokumenttiProsessi hae(@PathParam("id") String id) {
		return dokumenttiProsessiKomponentti.haeProsessi(id);
	}

	@POST
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Ilmoittaa poikkeuksen prosessiin", response = Response.class)
	public Response poikkeus(@PathParam("id") String id,
			@QueryParam("poikkeus") String poikkeus) {
		DokumenttiProsessi d = dokumenttiProsessiKomponentti.haeProsessi(id);
		if (d != null) {
			d.getPoikkeukset().add(
					new Poikkeus("Dokumenttiprosessiresurssi",
							"Poikkeuksen ilmoitus", poikkeus));
		}
		return Response.ok().build();
	}
}
