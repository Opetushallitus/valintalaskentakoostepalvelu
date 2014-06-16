package fi.vm.sade.valinta.kooste.sijoittelu.resource;

import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import fi.vm.sade.valinta.kooste.sijoittelu.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

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
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	@POST
	@Path("/aktivoi")
	@ApiOperation(value = "Sijoittelun aktivointi", response = String.class)
	public ProsessiId aktivoiSijoittelu(@QueryParam("hakuOid") String hakuOid) {
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
			DokumenttiProsessi prosessi = new DokumenttiProsessi("Sijoittelu",
					"aktivointi", hakuOid, Arrays.asList("sijoittelu"));
			LOG.info("aktivoiSijoittelu haulle {}", hakuOid);
			sijoitteluAktivointiProxy.aktivoiSijoittelu(prosessi, hakuOid,
					SecurityContextHolder.getContext().getAuthentication());
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			return prosessi.toProsessiId();
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
			// TODO: käyttöoikeus hakuun ja tarkastus samalla onko hakukohdetta,
			// jos vaihdetaan pois OPH_CRUDISTA
			Sijoittelu sijoittelu = JatkuvaSijoittelu.SIJOITTELU_HAUT
					.get(hakuOid);
			if (sijoittelu == null) {
				sijoittelu = new Sijoittelu();
			}
			sijoittelu.setHakuOid(hakuOid);
			sijoittelu.setAjossa(true);
			JatkuvaSijoittelu.SIJOITTELU_HAUT.put(hakuOid, sijoittelu);
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
			Sijoittelu sijoittelu = JatkuvaSijoittelu.SIJOITTELU_HAUT
					.get(hakuOid);
			if (sijoittelu == null) {
				sijoittelu = new Sijoittelu();
			}
			sijoittelu.setHakuOid(hakuOid);
			sijoittelu.setAjossa(false);
			JatkuvaSijoittelu.SIJOITTELU_HAUT.put(hakuOid, sijoittelu);
			return "poistettu";
		}
	}

	@GET
	@Path("/jatkuva/kaikki")
	@Produces(MediaType.APPLICATION_JSON)
	@PreAuthorize(OPH_CRUD)
	@ApiOperation(value = "Kaikki aktiiviset sijoittelut", response = Map.class)
	public Map<String, Sijoittelu> aktiivisetSijoittelut() {
		return JatkuvaSijoittelu.SIJOITTELU_HAUT;
	}

	@GET
	@Path("/jatkuva")
	@Produces(MediaType.APPLICATION_JSON)
	@PreAuthorize(OPH_CRUD)
	@ApiOperation(value = "Haun aktiiviset sijoittelut", response = Sijoittelu.class)
	public Sijoittelu jatkuvaTila(@QueryParam("hakuOid") String hakuOid) {
		if (StringUtils.isBlank(hakuOid)) {
			return null;
		} else {
			return JatkuvaSijoittelu.SIJOITTELU_HAUT.get(hakuOid);
		}
	}
}
