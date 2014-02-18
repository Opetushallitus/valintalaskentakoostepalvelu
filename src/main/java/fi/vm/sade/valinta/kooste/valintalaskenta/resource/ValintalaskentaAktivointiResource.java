package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HakukohteenValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

/**
 * @author Jussi Jartamo
 */
@Controller
@Path("valintalaskenta")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskenta", description = "Valintalaskennan aktivointi")
public class ValintalaskentaAktivointiResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaAktivointiResource.class);

	@Autowired
	private HakukohteenValintalaskentaRoute hakukohteenValintalaskentaAktivointiProxy;

	@Autowired
	private HaunValintalaskentaRoute haunValintalaskentaAktivointiProxy;

	@Autowired
	private ParametriService parametriService;

	@Value("${valintalaskentakoostepalvelu.valintaperusteService.url}")
	String valintaperusteServiceUrl;

	@Value("${valintalaskentakoostepalvelu.valintalaskentaService.url}")
	String valintalaskentaServiceUrl;

	@Resource(name = "valintalaskentaValvomo")
	private ValvomoService<ValintalaskentaProsessi> valintalaskentaValvomo;

	@GET
	@Path("/status")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Valintalaskenta-reitin tila", response = Collection.class)
	public Collection<ProsessiJaStatus<ValintalaskentaProsessi>> status() {
		return valintalaskentaValvomo.getUusimmatProsessitJaStatukset();
	}

	@GET
	@Path("/aktivoi")
	@ApiOperation(value = "Valintalaskennan aktivointi hakukohteelle", response = Response.class)
	public Response aktivoiHakukohteenValintalaskenta(
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("valinnanvaihe") Integer valinnanvaihe) {
		try {
			if (StringUtils.isBlank(hakukohdeOid) || valinnanvaihe == null) {
				return Response
						.status(Response.Status.OK)
						.entity("get parameter 'hakukohdeOid' and 'valinnanvaihe' required")
						.build();
			} else {
				LOG.info("Valintalaskenta kohteelle {}", hakukohdeOid);
				hakukohteenValintalaskentaAktivointiProxy
						.aktivoiValintalaskenta(hakukohdeOid, valinnanvaihe);
				return Response.status(Response.Status.OK)
						.entity("in progress").build();
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Error aktivoi: {}", e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(e.getMessage()).build();
		}
	}

	@GET
	@Path("/aktivoiHaunValintalaskenta")
	@ApiOperation(value = "Valintalaskennan aktivointi haulle", response = Response.class)
	public Response aktivoiHaunValintalaskenta(
			@QueryParam("hakuOid") String hakuOid) {
		if (!parametriService.valintalaskentaEnabled(hakuOid)) {
			return Response.status(Response.Status.FORBIDDEN).build();
		}

		if (StringUtils.isBlank(hakuOid)) {
			return Response.status(Response.Status.OK)
					.entity("get parameter 'hakuoid' required").build();
		} else {
			ProsessiJaStatus<?> ajossaOlevaLaskenta = valintalaskentaValvomo
					.getAjossaOlevaProsessiJaStatus();
			if (ajossaOlevaLaskenta != null) {
				LOG.error("Valintalaskenta on jo ajossa!\r\n {}",
						new Gson().toJson(ajossaOlevaLaskenta));
				return Response.serverError().entity(ajossaOlevaLaskenta)
						.build();
			}

			LOG.info("Suoritetaan valintalaskenta haulle {}", hakuOid);
			haunValintalaskentaAktivointiProxy.aktivoiValintalaskentaAsync(
					null, hakuOid, SecurityContextHolder.getContext()
							.getAuthentication());
			return Response.status(Response.Status.OK).entity("in progress")
					.build();
		}
	}

	@POST
	@Path("/aktivoiHaunValintalaskenta")
	@Consumes("application/json")
	@ApiOperation(value = "Valintalaskennan aktivointi haulle ilman annettuja hakukohteita", response = Response.class)
	public Response aktivoiHaunValintalaskentaIlmanAnnettujaHakukohteita(
			@QueryParam("hakuOid") String hakuOid, List<String> hakukohdeOids) {
		if (!parametriService.valintalaskentaEnabled(hakuOid)) {
			return Response.status(Response.Status.FORBIDDEN).build();
		}

		if (StringUtils.isBlank(hakuOid)) {
			return Response.status(Response.Status.OK)
					.entity("get parameter 'hakuoid' required").build();
		} else {

			ProsessiJaStatus<?> ajossaOlevaLaskenta = valintalaskentaValvomo
					.getAjossaOlevaProsessiJaStatus();
			if (ajossaOlevaLaskenta != null) {
				LOG.error("Valintalaskenta on jo ajossa!\r\n {}",
						new Gson().toJson(ajossaOlevaLaskenta));
				return Response.serverError().entity(ajossaOlevaLaskenta)
						.build();
			}
			LOG.info(
					"Suoritetaan valintalaskenta haulle {}: Ilman seuraavia hakukohteita {}",
					hakuOid, Arrays.toString(hakukohdeOids.toArray()));

			haunValintalaskentaAktivointiProxy.aktivoiValintalaskentaAsync(
					hakukohdeOids, hakuOid, SecurityContextHolder.getContext()
							.getAuthentication());
			return Response.status(Response.Status.OK).entity("in progress")
					.build();
		}
	}
}
