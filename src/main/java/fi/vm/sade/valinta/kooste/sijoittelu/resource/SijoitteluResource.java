package fi.vm.sade.valinta.kooste.sijoittelu.resource;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.service.valintatiedot.schema.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Controller
@Path("sijoittele")
@PreAuthorize("isAuthenticated()")
@Api(value = "/sijoittele", description = "Resurssi sijoitteluun")
public interface SijoitteluResource {


	@GET
	@Path("{hakuOid}")
	@ApiOperation(value = "Hakemuksen valintatulosten haku")
	public HakuTyyppi sijoittele(@PathParam("hakuOid") String hakuOid);


}
