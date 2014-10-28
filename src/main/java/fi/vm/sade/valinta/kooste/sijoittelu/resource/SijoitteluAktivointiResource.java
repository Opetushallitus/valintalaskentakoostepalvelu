package fi.vm.sade.valinta.kooste.sijoittelu.resource;

import java.util.Collection;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.gson.Gson;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoittelunValvonta;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;

/**
 *
 */
@Controller
@Path("koostesijoittelu")
@PreAuthorize("isAuthenticated()")
@Api(value = "/koostesijoittelu", description = "Ohjausparametrit palveluiden aktiviteettipäivämäärille")
public class SijoitteluAktivointiResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(SijoitteluAktivointiResource.class);
	public static final String OPH_CRUD = "hasAnyRole('ROLE_APP_SIJOITTELU_CRUD_1.2.246.562.10.00000000001')";

	@Autowired
	private SijoitteluAktivointiRoute sijoitteluAktivointiProxy;

	@Autowired
	private ParametriService parametriService;

	@Autowired
	private SijoittelunSeurantaResource sijoittelunSeurantaResource;

	@Autowired
	private SijoittelunValvonta sijoittelunValvonta;

	@Autowired
	private JatkuvaSijoittelu jatkuvaSijoittelu;

	@GET
	@Path("/status/{hakuoid}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Sijoittelun status", response = String.class)
	public Sijoittelu status(@PathParam("hakuoid") String hakuOid) {
		return sijoittelunValvonta.haeAktiivinenSijoitteluHaulle(hakuOid);
	}

	@GET
	@Path("/status")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Jatkuvan sijoittelun jonossa olevat sijoittelut", response = String.class)
	public Collection<DelayedSijoittelu> status() {
		return jatkuvaSijoittelu.haeJonossaOlevatSijoittelut();
	}

	@POST
	@Path("/aktivoi")
	@PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_CRUD')")
	@ApiOperation(value = "Sijoittelun aktivointi", response = String.class)
	public void aktivoiSijoittelu(@QueryParam("hakuOid") String hakuOid) {
		if (!parametriService.valinnanhallintaEnabled(hakuOid)) {
			LOG.error(
					"Sijoittelua yritettiin käynnistää haulle({}) ilman käyttöoikeuksia!",
					hakuOid);
			throw new RuntimeException("Ei käyttöoikeuksia!");
		}

		if (StringUtils.isBlank(hakuOid)) {
			LOG.error("Sijoittelua yritettiin käynnistää ilman hakuOidia!");
			throw new RuntimeException("Parametri hakuOid on pakollinen!");
		} else {
			sijoitteluAktivointiProxy
					.aktivoiSijoittelu(new Sijoittelu(hakuOid));
		}
	}

	@GET
	@Path("/jatkuva/aktivoi")
	@PreAuthorize(OPH_CRUD)
	@ApiOperation(value = "Ajastetun sijoittelun aktivointi", response = String.class)
	public String aktivoiJatkuvassaSijoittelussa(
			@QueryParam("hakuOid") String hakuOid) {
		if (!parametriService.valinnanhallintaEnabled(hakuOid)) {
			return "no privileges.";
		}

		if (StringUtils.isBlank(hakuOid)) {
			return "get parameter 'hakuOid' required";
		} else {
			LOG.info("jatkuva sijoittelu aktivoitu haulle {}", hakuOid);
			sijoittelunSeurantaResource.merkkaaSijoittelunAjossaTila(hakuOid,
					true);
			return "aktivoitu";
		}
	}

	@GET
	@Path("/jatkuva/poista")
	@PreAuthorize(OPH_CRUD)
	@ApiOperation(value = "Ajastetun sijoittelun deaktivointi", response = String.class)
	public String poistaJatkuvastaSijoittelusta(
			@QueryParam("hakuOid") String hakuOid) {
		if (!parametriService.valinnanhallintaEnabled(hakuOid)) {
			return "no privileges.";
		}

		if (StringUtils.isBlank(hakuOid)) {
			return "get parameter 'hakuOid' required";
		} else {
			LOG.info("jatkuva sijoittelu poistettu haulta {}", hakuOid);
			sijoittelunSeurantaResource.poistaSijoittelu(hakuOid);
			return "poistettu";
		}
	}

	@GET
	@Path("/jatkuva/kaikki")
	@Produces(MediaType.APPLICATION_JSON)
	@PreAuthorize(OPH_CRUD)
	@ApiOperation(value = "Kaikki aktiiviset sijoittelut", response = Map.class)
	public Collection<SijoitteluDto> aktiivisetSijoittelut() {
		return sijoittelunSeurantaResource.hae();
	}

	@GET
	@Path("/jatkuva")
	@Produces(MediaType.APPLICATION_JSON)
	@PreAuthorize(OPH_CRUD)
	@ApiOperation(value = "Haun aktiiviset sijoittelut", response = SijoitteluDto.class)
	public String jatkuvaTila(@QueryParam("hakuOid") String hakuOid) {
		if (StringUtils.isBlank(hakuOid)) {
			return null;
		} else {
			SijoitteluDto sijoitteluDto = sijoittelunSeurantaResource
					.hae(hakuOid);
			return new Gson().toJson(sijoitteluDto);
		}
	}

	@GET
	@Path("/jatkuva/paivita")
	@PreAuthorize(OPH_CRUD)
	@ApiOperation(value = "Ajastetun sijoittelun aloituksen päivitys", response = String.class)
	public String paivitaJatkuvanSijoittelunAloitus(
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("aloitusajankohta") Long aloitusajankohta,
			@QueryParam("ajotiheys") Integer ajotiheys) {
		if (!parametriService.valinnanhallintaEnabled(hakuOid)) {
			return "no privileges.";
		}

		if (StringUtils.isBlank(hakuOid)) {
			return "get parameter 'hakuOid' required";
		} else {
			sijoittelunSeurantaResource.paivitaSijoittelunAloitusajankohta(
					hakuOid, aloitusajankohta, ajotiheys);
			return "paivitetty";
		}
	}
}
