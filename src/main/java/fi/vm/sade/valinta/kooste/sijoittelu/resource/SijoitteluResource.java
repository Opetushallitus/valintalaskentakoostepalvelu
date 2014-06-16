package fi.vm.sade.valinta.kooste.sijoittelu.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.springframework.stereotype.Controller;

@Controller
@Path("sijoittele")
public interface SijoitteluResource {

	@GET
	@Path("{hakuOid}")
	public String sijoittele(@PathParam("hakuOid") String hakuOid);

}
