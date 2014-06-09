package fi.vm.sade.valinta.kooste.sijoittelu.resource;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;

@Controller
@Path("tila")
@PreAuthorize("isAuthenticated()")
@Api(value = "/tila", description = "Valintatulokset")
public interface TilaResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{hakemusOid}")
	@ApiOperation(value = "Listaus hakemuksen valintatuloksista", response = List.class)
	public List<Valintatulos> hakemus(@PathParam("hakemusOid") String hakemusOid);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{hakemusOid}/{hakuOid}/{hakukohdeOid}/{valintatapajonoOid}/")
	@ApiOperation(value = "Yksittäinen valintatulos", response = Valintatulos.class)
	public Valintatulos hakemus(@PathParam("hakuOid") String hakuOid,
			@PathParam("hakukohdeOid") String hakukohdeOid,
			@PathParam("valintatapajonoOid") String valintatapajonoOid,
			@PathParam("hakemusOid") String hakemusOid);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/hakukohde/{hakukohdeOid}/{valintatapajonoOid}/")
	@ApiOperation(value = "Listaus valintatuloksista hakukohteelle", response = List.class)
	public List<Valintatulos> haku(
			@PathParam("hakukohdeOid") String hakukohdeOid,
			@PathParam("valintatapajonoOid") String valintatapajonoOid);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/hakukohde/{hakukohdeOid}")
	List<Valintatulos> hakukohteelle(
			@PathParam("hakukohdeOid") String hakukohdeOid);
}
