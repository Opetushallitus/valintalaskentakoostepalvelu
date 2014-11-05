package fi.vm.sade.valinta.kooste.erillishaku.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunTuontiService;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunVientiService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Controller
@Path("erillishaku")
@PreAuthorize("isAuthenticated()")
@Api(value = "/erillishaku", description = "Resurssi erillishaun tietojen tuontiin ja vientiin")
public class ErillishakuResource {

	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiKomponentti;

	@Autowired
	private ErillishaunTuontiService tuontiService;
	
	@Autowired
	private ErillishaunVientiService vientiService;

	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
	@POST
	@Path("/vienti")
	@Consumes("application/json")
	@ApiOperation(consumes = "application/json", value = "Erillishaun hakukohteen vienti taulukkolaskentaan", response = ProsessiId.class)
	public ProsessiId vienti(@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("valintatapajonoOid") String valintatapajonoOid) {
		ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
		dokumenttiKomponentti.tuoUusiProsessi(prosessi);
		//
		vientiService.vie(prosessi, new ErillishakuDTO(hakuOid, hakukohdeOid,
				tarjoajaOid, valintatapajonoOid));
		return prosessi.toProsessiId();
	}

	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
	@POST
	@Path("/tuonti")
	@Consumes("application/octet-stream")
	@ApiOperation(consumes = "application/json", value = "Erillishaun hakukohteen tuonti taulukkolaskennalla", response = ProsessiId.class)
	public ProsessiId tuonti(@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("valintatapajonoOid") String valintatapajonoOid,
			InputStream file) throws IOException {
		ByteArrayOutputStream b;
		IOUtils.copy(file, b = new ByteArrayOutputStream());
		IOUtils.closeQuietly(file);
		ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
		dokumenttiKomponentti.tuoUusiProsessi(prosessi);
		tuontiService.tuo(prosessi, new ErillishakuDTO(hakuOid, hakukohdeOid,
				tarjoajaOid, valintatapajonoOid),
				new ByteArrayInputStream(b.toByteArray()));
		//
		return prosessi.toProsessiId();
	}
}
