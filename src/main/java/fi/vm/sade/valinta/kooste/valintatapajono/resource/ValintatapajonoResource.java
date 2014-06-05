package fi.vm.sade.valinta.kooste.valintatapajono.resource;

import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Controller
@Path("valintatapajonolaskenta")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintatapajonolaskenta", description = "Valintatapajonon tuonti ja vienti taulukkolaskentaan")
public class ValintatapajonoResource {

	@Autowired
	private ValintatapajonoVientiRoute valintatapajonoVienti;
	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
	@POST
	@Path("/vienti")
	@Consumes("application/json")
	@ApiOperation(consumes = "application/json", value = "Valintatapajonon vienti taulukkolaskentaan", response = ProsessiId.class)
	public ProsessiId vienti(@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("valintatapajonoOid") String valintatapajonoOid) {
		DokumenttiProsessi prosessi = new DokumenttiProsessi("Valintatapajono",
				"vienti", hakuOid, Arrays.asList(hakukohdeOid));
		valintatapajonoVienti.vie(prosessi, hakuOid, hakukohdeOid,
				valintatapajonoOid);
		dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
		return prosessi.toProsessiId();
	}
}
