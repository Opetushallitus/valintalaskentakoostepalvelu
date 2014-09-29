package fi.vm.sade.valinta.kooste.kela.resource;

import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHakuFiltteri;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

@Controller
@Path("kela")
@PreAuthorize("isAuthenticated()")
@Api(value = "/kela", description = "Kela-dokumentin luontiin ja FTP-siirtoon")
public class KelaResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(KelaResource.class);

	@Autowired
	private KoodiService koodiService;

	@Autowired
	private KelaRoute kelaRoute;

	@Autowired
	private KelaFtpRoute kelaFtpRoute;

	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	@POST
	@Path("/aktivoi")
	@Consumes("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Kela-reitin aktivointi", response = ProsessiId.class)
	public ProsessiId aktivoiKelaTiedostonluonti(KelaHakuFiltteri hakuTietue) {
		// tietoe ei ole viela saatavilla
		if (hakuTietue == null || hakuTietue.getHakuOids() == null
				|| hakuTietue.getHakuOids().isEmpty()) {
			throw new RuntimeException(
					"Vähintään yksi hakuOid on annettava Kela-dokumentin luontia varten.");
		}
		String aineistonNimi = hakuTietue.getAineisto();// "Toisen asteen vastaanottotiedot";
		String organisaationNimi = "OPH";
		KelaProsessi kelaProsessi = new KelaProsessi("Kela-dokumentin luonti",
				hakuTietue.getHakuOids());
		kelaRoute.aloitaKelaLuonti(kelaProsessi,
				new KelaLuonti(kelaProsessi.getId(), hakuTietue.getHakuOids(),
						aineistonNimi, organisaationNimi, new KelaCache(
								koodiService)));
		// SecurityContextHolder.getContext().getAuthentication()
		dokumenttiProsessiKomponentti.tuoUusiProsessi(kelaProsessi);
		return kelaProsessi.toProsessiId();
	}

	@PUT
	@Path("/laheta/{documentId}")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "FTP-siirto", response = Response.class)
	public Response laheta(@PathParam("documentId") String documentId) {
		LOG.warn("Kela-ftp siirto aloitettu {}", documentId);
		kelaFtpRoute.aloitaKelaSiirto(documentId);
		return Response.ok().build();
	}

}
