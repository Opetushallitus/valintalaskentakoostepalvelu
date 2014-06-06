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
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
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

	@POST
	@Path("/hyvaksymiskirjeet")
	@Consumes("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
	public ProsessiId hyvaksymiskirjeetKokoHaulle(
			@QueryParam("hakuOid") String hakuOid) {
		try {
			DokumenttiProsessi prosessi = new DokumenttiProsessi(
					"hyvaksymiskirjeet", "Luo hyvaksymiskirjeet haulle", null,
					Arrays.asList("hyvaksymiskirjeet", "haulle"));
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			return new ProsessiId(prosessi.getId());
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
			DokumenttiProsessi prosessi = new DokumenttiProsessi(
					"taulukkolaskennat", "Luo taulukkolaskennat haulle", null,
					Arrays.asList("taulukkolaskennat", "haulle"));
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			return new ProsessiId(prosessi.getId());
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
