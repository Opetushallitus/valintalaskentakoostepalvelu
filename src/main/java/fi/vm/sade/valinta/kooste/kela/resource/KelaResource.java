package fi.vm.sade.valinta.kooste.kela.resource;

import static fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil.toSearchCriteria;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.annotation.Resource;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.tarjonta.route.TarjontaHakuRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
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
	private TarjontaHakuRoute hakuProxy;

	@Autowired
	private KoodiService koodiService;

	@Autowired
	private KelaRoute kelaRoute;

	@Resource(name = "kelaValvomo")
	private ValvomoService<KelaProsessi> kelaValvomo;

	@Resource(name = "dokumenttipalveluRestClient")
	private DokumenttiResource dokumenttiResource;
	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	@GET
	@Path("/status")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Kela-reitin tila", response = Collection.class)
	public Collection<ProsessiJaStatus<KelaProsessi>> status() {
		return kelaValvomo.getUusimmatProsessitJaStatukset();
	}

	@POST
	@Path("/aktivoi")
	@ApiOperation(value = "Kela-reitin aktivointi", response = ProsessiId.class)
	public ProsessiId aktivoiKelaTiedostonluonti(
			@QueryParam("hakuOid") String hakuOid) {
		// tietoe ei ole viela saatavilla
		if (hakuOid == null) {
			throw new RuntimeException("Haku-parametri on pakollinen");
		}
		String aineistonNimi = "Toisen asteen vastaanottotiedot";
		String organisaationNimi = "OPH";
		int lukuvuosi = 2014;
		int kuukausi = 1;
		try { // REFAKTOROI OSAKSI REITTIA
			HakuDTO hakuDTO = hakuProxy.haeHaku(hakuOid);
			lukuvuosi = hakuDTO.getKoulutuksenAlkamisVuosi();
			// kausi_k
			for (KoodiType koodi : koodiService
					.searchKoodis(toSearchCriteria(hakuDTO
							.getKoulutuksenAlkamiskausiUri()))) {
				if ("S".equals(StringUtils.upperCase(koodi.getKoodiArvo()))) { // syksy
					kuukausi = 8;
				} else if ("K".equals(StringUtils.upperCase(koodi
						.getKoodiArvo()))) { // kevat
					kuukausi = 1;
				} else {
					LOG.error(
							"Viallinen arvo {}, koodilla {} ",
							new Object[] { koodi.getKoodiArvo(),
									hakuDTO.getKoulutuksenAlkamiskausiUri() });
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOG.error("Ei voitu hakea lukuvuotta tarjonnalta syystä {}",
					e.getMessage());
		}
		DokumenttiProsessi kelaProsessi = new DokumenttiProsessi("Kela",
				"Kela-dokumentin luonti", hakuOid, Arrays.asList("kela"));

		kelaRoute.aloitaKelaLuonti(kelaProsessi, hakuOid, new DateTime(
				lukuvuosi, kuukausi, 1, 1, 1).toDate(), new Date(),
				aineistonNimi, organisaationNimi, SecurityContextHolder
						.getContext().getAuthentication());
		dokumenttiProsessiKomponentti.tuoUusiProsessi(kelaProsessi);
		return kelaProsessi.toProsessiId();
	}

	@PUT
	@Path("/laheta/{documentId}")
	@ApiOperation(value = "FTP-siirto", response = Response.class)
	public Response laheta(@PathParam("documentId") String input) {
		// KelaCacheDocument document = kelaCache.getDocument(input);
		// if (document == null) {
		// return Response.status(Status.BAD_REQUEST).build();
		// }
		// try {
		// kelaFtpProxy.lahetaTiedosto(document.getHeader(), new
		// ByteArrayInputStream(document.getData()));
		// } catch (Exception e) {
		// kelaCache.addDocument(KelaCacheDocument.createErrorMessage("FTP-lähetys epäonnistui!"));
		// return Response.serverError().build();
		// }
		// kelaCache.addDocument(KelaCacheDocument.createInfoMessage("Dokumentti "
		// + document.getHeader()
		// + " lähetetty Kelan FTP-palvelimelle"));
		return Response.ok().build();
	}

}
