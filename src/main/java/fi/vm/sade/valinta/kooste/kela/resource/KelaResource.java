package fi.vm.sade.valinta.kooste.kela.resource;

import static fi.vm.sade.valinta.kooste.util.TarjontaUriToKoodistoUtil.toSearchCriteria;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Resource;
import javax.ws.rs.GET;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.koodisto.service.types.common.KoodiType;
import fi.vm.sade.tarjonta.service.resources.HakuResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.MetaData;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.tarjonta.route.TarjontaHakuRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

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

	@GET
	@Path("/status")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Kela-reitin tila", response = Collection.class)
	public Collection<ProsessiJaStatus<KelaProsessi>> status() {
		return kelaValvomo.getUusimmatProsessitJaStatukset();
	}

	/**
	 * CAS TEST
	 */
	@Value("${valintalaskentakoostepalvelu.tarjonta.rest.url}")
	String baseAddress;
	@Autowired
	HakuResource hakuResource;

	@GET
	@Path("/test")
	@ApiOperation(value = "CAS:n performanssi testaukseen. Varmistetaan etta kaikki palvelut vapauttaa tiketit.", response = Response.class)
	public Response performanceTest(@QueryParam("threads") Integer threads,
			@QueryParam("works") Integer works,
			final @QueryParam("hakuOid") String hakuOid) {
		LOG.info("Number of threads {} and number of works {} hakuOid {}",
				new Object[] { threads, works, hakuOid });
		if (works == null || threads == null || hakuOid == null) {
			LOG.error("Threads and works query parameter is mandatory!");
			return Response.serverError().build();
		}

		ExecutorService e = Executors.newFixedThreadPool(threads);
		final Runnable r = new Runnable() {

			@Override
			public void run() {

				hakuResource.getByOID(hakuOid);
			}
		};
		for (int i = 0; i < works; ++i) {
			e.submit(r);
		}
		LOG.info("Tyot submitattu!");
		return Response.ok().build();
	}

	/**
	 * CAS TEST
	 */

	@GET
	@Path("/aktivoi")
	@ApiOperation(value = "Kela-reitin aktivointi", response = Response.class)
	public Response aktivoiKelaTiedostonluonti(
			@QueryParam("hakuOid") String hakuOid) {
		// tietoe ei ole viela saatavilla
		if (hakuOid == null) {
			return Response.serverError().entity("HakuOid on pakollinen!")
					.build();
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
		kelaRoute
				.aloitaKelaLuonti(hakuOid, new DateTime(lukuvuosi, kuukausi, 1,
						1, 1).toDate(), new Date(), aineistonNimi,
						organisaationNimi);
		return Response.ok().build();
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

	@GET
	@Path("/listaus")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Listaus Kela-dokumenteista", response = Collection.class)
	public Collection<MetaData> listaus() {
		return dokumenttiResource.hae(Arrays.asList("kela"));
	}
}
