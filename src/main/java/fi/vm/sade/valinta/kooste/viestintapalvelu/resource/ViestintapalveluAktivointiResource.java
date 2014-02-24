package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeHakemuksilleRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratHakemuksilleRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratSijoittelussaHyvaksytyilleRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Ei palauta PDF-tiedostoa vaan URI:n varsinaiseen resurssiin - koska
 *         AngularJS resurssin palauttaman datan konvertoiminen selaimen
 *         ladattavaksi tiedostoksi on ongelmallista (mutta ei mahdotonta - onko
 *         tarpeen?).
 */
@Controller
@Path("viestintapalvelu")
@PreAuthorize("isAuthenticated()")
@Api(value = "/viestintapalvelu", description = "Osoitetarrojen, jälkiohjauskirjeiden ja hyväksymiskirjeiden tuottaminen")
public class ViestintapalveluAktivointiResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(ViestintapalveluAktivointiResource.class);

	@Autowired
	private OsoitetarratRoute addressLabelBatchProxy;
	@Autowired
	private OsoitetarratHakemuksilleRoute osoitetarratHakemuksille;
	@Autowired
	private JalkiohjauskirjeRoute jalkiohjauskirjeBatchProxy;
	@Autowired
	private HyvaksymiskirjeRoute hyvaksymiskirjeBatchProxy;
	@Autowired
	private OsoitetarratSijoittelussaHyvaksytyilleRoute hyvaksyttyjenOsoitetarratProxy;
	@Autowired
	private KoekutsukirjeRoute koekutsukirjeRoute;
	@Autowired
	private KoekutsukirjeHakemuksilleRoute koekutsukirjeetHakemuksille;
	@Resource(name = "dokumenttipalveluRestClient")
	private DokumenttiResource dokumenttiResource;

	@POST
	@Path("/osoitetarrat/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin hakukohteelle", response = Response.class)
	public Response aktivoiOsoitetarrojenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("valintakoeOid") List<String> valintakoeOids) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			addressLabelBatchProxy.osoitetarratAktivointi(
					hakemuksillaRajaus.getHakemusOids(), hakukohdeOid,
					valintakoeOids);
			dokumenttiResource.viesti(new Message(
					"Osoitetarrojen luonti aloitettu", Arrays
							.asList("osoitetarra"), DateTime.now().plusDays(1)
							.toDate()));
			return Response.status(Status.OK).build();
		} catch (Exception e) {
			LOG.error("Osoitetarrojen luonnissa virhe! {}", e.getMessage());
			// Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
			// todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
			// Ylläpitäjä voi lukea logeista todellisen syyn!
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(Vastaus.virhe("Osoitetarrojen luonti epäonnistui! "
							+ e.getMessage())).build();
		}
	}

	@POST
	@Path("/osoitetarrat/hakemuksille/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
	public Response aktivoiOsoitetarrojenLuontiHakemuksille(
			DokumentinLisatiedot hakemuksillaRajaus) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			osoitetarratHakemuksille.osoitetarrotHakemuksilleAktivointiAsync(
					hakemuksillaRajaus.getHakemusOids(), SecurityContextHolder
							.getContext().getAuthentication());
			dokumenttiResource.viesti(new Message(
					"Osoitetarrojen luonti hakemuksille aloitettu", Arrays
							.asList("osoitetarrat"), DateTime.now().plusDays(1)
							.toDate()));
			return Response.status(Status.OK).build();
		} catch (Exception e) {
			LOG.error("Osoitetarrojen luonnissa virhe! {}", e.getMessage());
			// Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
			// todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
			// Ylläpitäjä voi lukea logeista todellisen syyn!
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(Vastaus.virhe("Osoitetarrojen luonti epäonnistui! "
							+ e.getMessage())).build();
		}
	}

	@POST
	@Path("/jalkiohjauskirjeet/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi jälkiohjauskirjeiden luonnin valitsemattomille", response = Response.class)
	public Response aktivoiJalkiohjauskirjeidenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakuOid") String hakuOid) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			jalkiohjauskirjeBatchProxy.jalkiohjauskirjeetAktivoiAsync(
					hakemuksillaRajaus.getHakemusOids(), hakuOid,
					//
					SecurityContextHolder.getContext().getAuthentication());
			try {
				// messageProxy.message("Jälkiohjauskirjeen luonti on aloitettu.");
			} catch (Exception e) {
				e.printStackTrace();
				LOG.error(
						"Viestintäpalvelun viestirajapinta ei ole käytettävissä! {}",
						e.getMessage());
			}
			return Response
					.status(Status.OK)
					.entity(Vastaus
							.viesti("Jälkiohjauskirjeen luonti on käynnistetty. Valmis kirje tulee näkyviin yhteisvalinnan hallinta välilehdelle."))
					.build();

		} catch (Exception e) {
			LOG.error("Jälkiohjauskirjeiden luonnissa virhe! {}",
					e.getMessage());
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(Vastaus
							.virhe("Jälkiohjauskirjeiden luonti epäonnistui! "
									+ e.getMessage())).build();
		}
	}

	@POST
	@Path("/hyvaksymiskirjeet/aktivoi")
	@ApiOperation(value = "Aktivoi hyväksymiskirjeiden luonnin hakukohteelle haussa", response = Response.class)
	public Response aktivoiHyvaksymiskirjeidenLuonti(
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
		try {
			hyvaksymiskirjeBatchProxy.hyvaksymiskirjeetAktivointi(hakukohdeOid,
					hakuOid, sijoitteluajoId);
			return Response.ok().build();
		} catch (Exception e) {
			LOG.error("Hyväksymiskirjeiden luonnissa virhe! {}", e.getMessage());
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(Vastaus
							.virhe("Hyväksymiskirjeiden luonti epäonnistui! "
									+ e.getMessage())).build();
		}
	}

	@POST
	@Path("/hyvaksyttyjenosoitetarrat/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi hyväksyttyjen osoitteiden luonnin hakukohteelle haussa", response = Response.class)
	public Response aktivoiHyvaksyttyjenOsoitetarrojenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			hyvaksyttyjenOsoitetarratProxy
					.hyvaksyttyjenOsoitetarrojenAktivointi(
							hakemuksillaRajaus.getHakemusOids(), hakukohdeOid,
							hakuOid, sijoitteluajoId);
			return Response.ok().build();
		} catch (Exception e) {
			LOG.error("Hyväksyttyjen osoitetarrojen luonnissa virhe! {}",
					e.getMessage());
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(Vastaus
							.virhe("Hyväksyttyjen osoitetarrojen luonti epäonnistui! "
									+ e.getMessage())).build();
		}
	}

	/**
	 * 
	 * @param hakukohdeOid
	 * @param hakuOid
	 * @param sijoitteluajoId
	 * @return 200 OK
	 */
	@POST
	@Path("/koekutsukirjeet/aktivoi")
	@Produces("application/json")
	@ApiOperation(value = "Aktivoi koekutsukirjeiden luonnin hakukohteelle haussa", response = Response.class)
	public Response aktivoiKoekutsukirjeidenLuonti(
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("valintakoeOids") List<String> valintakoeOids,
			DokumentinLisatiedot hakemuksillaRajaus) {
		if (hakukohdeOid == null || valintakoeOids == null
				|| valintakoeOids.isEmpty()) {
			LOG.error("Valintakoe ja hakukohde on pakollisia tietoja koekutsukirjeen luontiin!");
			return Response
					.serverError()
					.entity("Valintakoe ja hakukohde on pakollisia tietoja koekutsukirjeen luontiin!")
					.build();
		}
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			if (hakemuksillaRajaus.getHakemusOids() != null) {
				LOG.info(
						"Koekutsukirjeiden luonti aloitettu yksittaiselle hakemukselle {}",
						hakemuksillaRajaus.getHakemusOids());
			} else {
				LOG.info("Koekutsukirjeiden luonti aloitettu");
			}
			koekutsukirjeRoute.koekutsukirjeetAktivointiAsync(
					hakemuksillaRajaus.getHakemusOids(), hakukohdeOid,
					valintakoeOids, hakemuksillaRajaus.getLetterBodyText(),
					SecurityContextHolder.getContext().getAuthentication());
		} catch (Exception e) {
			LOG.error("Koekutsukirjeiden luonti epäonnistui! {}",
					e.getMessage());
			e.printStackTrace();
			return Response.serverError().entity(e.getMessage()).build();
		}
		return Response.ok().build();
	}

	/**
	 * 
	 * @param hakukohdeOid
	 * @param hakuOid
	 * @param sijoitteluajoId
	 * @return 200 OK
	 */
	@POST
	@Path("/koekutsukirjeet/hakemuksille/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi koekutsukirjeiden luonnin yksittaisille hakemuksille", response = Response.class)
	public Response aktivoiKoekutsukirjeidenLuontiHakemuksille(
			DokumentinLisatiedot koekutsukirjeHakemuksille,
			@QueryParam("hakukohdeOid") String hakukohdeOid) {
		koekutsukirjeetHakemuksille.koekutsukirjeetAktivointiHakemuksilleAsync(
				koekutsukirjeHakemuksille.getHakemusOids(), hakukohdeOid,
				koekutsukirjeHakemuksille.getLetterBodyText(),
				SecurityContextHolder.getContext().getAuthentication());
		return Response.ok().build();
	}
}
