package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

/**
 * @author Jussi Jartamo
 */
@Controller
@Path("valintalaskentamuistissa")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentamuistissa", description = "Valintalaskenta muistinvaraisesti")
public class ValintalaskentaMuistissaResource {

	@Resource(name = "valintalaskentaMuistissaValvomo")
	private ValvomoService<ValintalaskentaMuistissaProsessi> valintalaskentaMuistissaValvomo;
	@Autowired
	private ValintalaskentaMuistissaRoute valintalaskentaMuistissa;

	@GET
	@Path("/status")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Muistinvaraisen valintalaskennan tila", response = Collection.class)
	public Collection<ProsessiJaStatus<ValintalaskentaMuistissaProsessi>> status() {
		return valintalaskentaMuistissaValvomo
				.getUusimmatProsessitJaStatukset();
	}

	@GET
	@Path("/exceptions")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Muistinvaraisen valintalaskennan poikkeukset", response = Collection.class)
	public Collection<Collection<Exception>> poikkeukset() {
		return Collections2
				.transform(
						valintalaskentaMuistissaValvomo.getUusimmatProsessit(),
						new Function<ValintalaskentaMuistissaProsessi, Collection<Exception>>() {
							public Collection<Exception> apply(
									ValintalaskentaMuistissaProsessi input) {
								return input.getKokonaistyo().getPoikkeukset();
							}
						});
	}

	@POST
	@Path("/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Valintalaskennan aktivointi haulle ilman annettuja hakukohteita", response = Response.class)
	public Response aktivoiHaunValintalaskentaIlmanAnnettujaHakukohteita(
			@QueryParam("hakuOid") String hakuOid, List<String> hakukohdeOids) {
		valintalaskentaMuistissa.aktivoiValintalaskentaAsync(
				ValintalaskentaCache.create(hakuOid, hakukohdeOids),
				hakukohdeOids, hakuOid, SecurityContextHolder.getContext()
						.getAuthentication());
		return Response.ok().build();
	}
}
