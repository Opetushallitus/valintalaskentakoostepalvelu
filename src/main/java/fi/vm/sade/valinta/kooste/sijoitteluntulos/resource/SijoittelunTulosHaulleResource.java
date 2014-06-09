package fi.vm.sade.valinta.kooste.sijoitteluntulos.resource;

import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosHyvaksymiskirjeetRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosOsoitetarratRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Controller
@Path("sijoitteluntuloshaulle")
@PreAuthorize("isAuthenticated()")
@Api(value = "/sijoitteluntuloshaulle", description = "Sijoitteluntulosten generointi koko haulle")
public class SijoittelunTulosHaulleResource {
	private static final Logger LOG = LoggerFactory
			.getLogger(SijoittelunTulosHaulleResource.class);

	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
	@Autowired
	private SijoittelunTulosTaulukkolaskentaRoute sijoittelunTulosTaulukkolaskentaRoute;
	@Autowired
	private SijoittelunTulosHyvaksymiskirjeetRoute sijoittelunTulosHyvaksymiskirjeetRoute;
	@Autowired
	private SijoittelunTulosOsoitetarratRoute sijoittelunTulosOsoitetarratRoute;

	@POST
	@Path("/osoitetarrat")
	@Consumes("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
	public ProsessiId osoitetarratKokoHaulle(
			@QueryParam("hakuOid") String hakuOid) {
		try {
			SijoittelunTulosProsessi prosessi = new SijoittelunTulosProsessi(
					"osoitetarrat", "Luo osoitetarrat haulle", null,
					Arrays.asList("osoitetarrat", "haulle"));
			sijoittelunTulosOsoitetarratRoute.osoitetarratHaulle(prosessi,
					hakuOid, SijoitteluResource.LATEST, SecurityContextHolder
							.getContext().getAuthentication());
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			return prosessi.toProsessiId();
		} catch (Exception e) {
			LOG.error("Osoitetarrojen luonnissa virhe! {}", e.getMessage());
			// Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
			// todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
			// Ylläpitäjä voi lukea logeista todellisen syyn!
			throw new RuntimeException("Osoitetarrojen luonnissa virhe!", e);

		}
	}

	@POST
	@Path("/hyvaksymiskirjeet")
	@Consumes("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
	public ProsessiId hyvaksymiskirjeetKokoHaulle(
			@QueryParam("hakuOid") String hakuOid) {
		try {
			SijoittelunTulosProsessi prosessi = new SijoittelunTulosProsessi(
					"hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", null,
					Arrays.asList("hyvaksymiskirjeet", "haulle"));
			sijoittelunTulosHyvaksymiskirjeetRoute.hyvaksymiskirjeetHaulle(
					prosessi, hakuOid, SijoitteluResource.LATEST,
					SecurityContextHolder.getContext().getAuthentication());
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			return prosessi.toProsessiId();
		} catch (Exception e) {
			LOG.error("Hyväksymiskirjeiden luonnissa virhe! {}", e.getMessage());
			// Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
			// todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
			// Ylläpitäjä voi lukea logeista todellisen syyn!
			throw new RuntimeException("Hyväksymiskirjeiden luonnissa virhe!",
					e);

		}
	}

	@POST
	@Path("/taulukkolaskennat")
	@Consumes("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
	public ProsessiId taulukkolaskennatKokoHaulle(
			@QueryParam("hakuOid") String hakuOid) {
		try {
			SijoittelunTulosProsessi prosessi = new SijoittelunTulosProsessi(
					"taulukkolaskennat", "Luo taulukkolaskennat haulle", null,
					Arrays.asList("taulukkolaskennat", "haulle"));
			sijoittelunTulosTaulukkolaskentaRoute.taulukkolaskennatHaulle(
					prosessi, hakuOid, SijoitteluResource.LATEST,
					SecurityContextHolder.getContext().getAuthentication());
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			return prosessi.toProsessiId();
		} catch (Exception e) {
			LOG.error("Taulukkolaskentojen luonnissa virhe! {}", e.getMessage());
			// Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
			// todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
			// Ylläpitäjä voi lukea logeista todellisen syyn!
			throw new RuntimeException("Taulukkolaskentojen luonnissa virhe!",
					e);

		}
	}
}
