package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;

import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaMaski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.seuranta.resource.SeurantaResource;

/**
 * @author Jussi Jartamo
 */
@Controller
@Path("valintalaskentakerralla")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentakerralla", description = "Valintalaskenta kaikille valinnanvaiheille kerralla")
public class ValintalaskentaKerrallaResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaKerrallaResource.class);

	@Autowired
	private SeurantaResource seurantaResource;
	@Autowired
	private ValintalaskentaKerrallaRoute valintalaskentaRoute;

	@POST
	@Path("/haku/{hakuOid}")
	public Response valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid) {
		LOG.error("Pyynto suorittaa valintalaskenta haulle {}", hakuOid);
		Laskenta laskenta = new Laskenta("uuid", hakuOid);
		valintalaskentaRoute
				.suoritaValintalaskentaKerralla(new LaskentaJaMaski(laskenta));
		return Response.ok().build();
	}
}
