package fi.vm.sade.valinta.kooste.hakuimport.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakukohdeImportRoute;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

/**
 * User: wuoti Date: 20.5.2013 Time: 10.34
 */
@Controller("HakuImportResource")
@Path("hakuimport")
@PreAuthorize("isAuthenticated()")
@Api(value = "/hakuimport", description = "Haun tuontiin tarjonnalta")
public class HakuImportResource {
	private static final Logger LOG = LoggerFactory
			.getLogger(HakuImportResource.class);

	@Autowired(required = false)
	private HakuImportRoute hakuImportAktivointiRoute;
	@Autowired(required = false)
	private HakukohdeImportRoute hakukohdeImportRoute;
	@Autowired(required = false)
	private ParametriService parametriService;

	@Autowired(required = false)
	@Qualifier("hakuImportValvomo")
	private ValvomoService<HakuImportProsessi> hakuImportValvomo;

	@GET
	@Path("/status")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Hauntuontireitin tila", response = Collection.class)
	public Collection<ProsessiJaStatus<HakuImportProsessi>> status() {
		return hakuImportValvomo.getUusimmatProsessitJaStatukset();
	}

	@GET
	@Path("/aktivoi")
	@ApiOperation(value = "Haun tuonnin aktivointi", response = String.class)
	public String aktivoiHakuImport(@QueryParam("hakuOid") String hakuOid) {
		if (!parametriService.valinnanhallintaEnabled(hakuOid)) {
			return "no privileges.";
		}

		if (StringUtils.isBlank(hakuOid)) {
			return "get parameter 'hakuOid' required";
		} else {
			LOG.info("Haku import haulle {}", hakuOid);
			hakuImportAktivointiRoute.asyncAktivoiHakuImport(hakuOid,
					SecurityContextHolder.getContext().getAuthentication());
			return "in progress";
		}
	}

	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@GET
	@Path("/hakukohde")
	@ApiOperation(value = "Haun tuonnin aktivointi", response = String.class)
	public String aktivoiHakukohdeImport(
			@QueryParam("hakukohdeOid") String hakukohdeOid) {

		if (StringUtils.isBlank(hakukohdeOid)) {
			return "get parameter 'hakukohde' required";
		} else {
			LOG.info("Hakukohde import hakukohteelle {}", hakukohdeOid);
			hakukohdeImportRoute.asyncAktivoiHakukohdeImport(hakukohdeOid,
					new HakuImportProsessi("Hakukohde", "Hakukhode"),
					SecurityContextHolder.getContext().getAuthentication());
			return "in progress";
		}
	}
}
