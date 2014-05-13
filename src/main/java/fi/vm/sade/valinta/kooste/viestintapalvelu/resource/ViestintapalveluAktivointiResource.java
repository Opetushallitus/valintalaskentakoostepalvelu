package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.DokumenttiTyyppi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratRoute;

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
	private OsoitetarratRoute osoitetarratRoute;
	@Autowired
	private JalkiohjauskirjeRoute jalkiohjauskirjeBatchProxy;
	@Autowired
	private HyvaksymiskirjeRoute hyvaksymiskirjeetRoute;
	@Autowired
	private KoekutsukirjeRoute koekutsukirjeRoute;
	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	@POST
	@Path("/osoitetarrat/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin hakukohteelle", response = Response.class)
	public ProsessiId aktivoiOsoitetarrojenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("valintakoeOid") List<String> valintakoeOids) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			DokumenttiProsessi osoiteProsessi = new DokumenttiProsessi(
					"Osoitetarrat", "Luo osoitetarrat", null, tags(
							"osoitetarrat", hakemuksillaRajaus.getTag()));
			dokumenttiProsessiKomponentti.tuoUusiProsessi(osoiteProsessi);
			osoitetarratRoute.osoitetarratAktivointi(
					DokumenttiTyyppi.VALINTAKOKEESEEN_OSALLISTUJAT,
					osoiteProsessi, hakemuksillaRajaus.getHakemusOids(),
					hakukohdeOid, valintakoeOids, SecurityContextHolder
							.getContext().getAuthentication());
			return new ProsessiId(osoiteProsessi.getId());
		} catch (Exception e) {
			LOG.error("Osoitetarrojen luonnissa virhe! {}", e.getMessage());
			// Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
			// todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
			// Ylläpitäjä voi lukea logeista todellisen syyn!
			e.printStackTrace();
			throw new RuntimeException("Osoitetarrojen luonti epäonnistui! "
					+ e.getMessage(), e);
		}
	}

	/**
	 * @param hakemuksillaRajaus
	 *            https://test-virkailija.oph.ware.fi/
	 *            valintalaskentakoostepalvelu/resources/
	 *            viestintapalvelu/osoitetarrat
	 *            /sijoittelussahyvaksytyille/aktivoi
	 *            ?hakuOid=1.2.246.562.5.2013080813081926341927
	 *            &hakukohdeOid=1.2.246.562.5.85532589612
	 *            &sijoitteluajoId=1392302745967
	 */
	@POST
	@Path("/osoitetarrat/sijoittelussahyvaksytyille/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi hyväksyttyjen osoitteiden luonnin hakukohteelle haussa", response = Response.class)
	public ProsessiId aktivoiHyvaksyttyjenOsoitetarrojenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			DokumenttiProsessi osoiteProsessi = new DokumenttiProsessi(
					"Osoitetarrat", "Sijoittelussa hyväksytyille", hakuOid,
					tags("osoitetarrat", hakemuksillaRajaus.getTag()));
			dokumenttiProsessiKomponentti.tuoUusiProsessi(osoiteProsessi);
			osoitetarratRoute.osoitetarratAktivointi(
					DokumenttiTyyppi.SIJOITTELUSSA_HYVAKSYTYT, osoiteProsessi,
					hakemuksillaRajaus.getHakemusOids(), hakukohdeOid, hakuOid,
					sijoitteluajoId,
					//
					SecurityContextHolder.getContext().getAuthentication());
			return osoiteProsessi.toProsessiId();
		} catch (Exception e) {
			LOG.error("Hyväksyttyjen osoitetarrojen luonnissa virhe! {}",
					e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(
					"Hyväksyttyjen osoitetarrojen luonnissa virhe!", e);

		}
	}

	/**
	 * 
	 * @Deprecated Tehdaan eri luontivariaatiot reitin alustusmuuttujilla. Ei
	 *             enää monta resurssia per toiminto.
	 * 
	 * @param hakemuksillaRajaus
	 * @return
	 */
	@POST
	@Path("/osoitetarrat/hakemuksille/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin annetuille hakemuksille", response = Response.class)
	public ProsessiId aktivoiOsoitetarrojenLuontiHakemuksille(
			DokumentinLisatiedot hakemuksillaRajaus) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			DokumenttiProsessi osoiteProsessi = new DokumenttiProsessi(
					"Osoitetarrat", "Luo osoitetarrat", null, tags(
							"osoitetarrat", hakemuksillaRajaus.getTag()));
			dokumenttiProsessiKomponentti.tuoUusiProsessi(osoiteProsessi);
			osoitetarratRoute.osoitetarratAktivointi(
					DokumenttiTyyppi.HAKEMUKSILLE, osoiteProsessi,
					hakemuksillaRajaus.getHakemusOids(),
					//
					SecurityContextHolder.getContext().getAuthentication());

			return new ProsessiId(osoiteProsessi.getId());
			/*
			 * dokumenttiResource.viesti(new Message(
			 * "Osoitetarrojen luonti hakemuksille aloitettu", Arrays
			 * .asList("osoitetarrat"), DateTime.now().plusDays(1) .toDate()));
			 */
		} catch (Exception e) {
			LOG.error("Osoitetarrojen luonnissa virhe! {}", e.getMessage());
			// Ei oikeastaan väliä loppukäyttäjälle miksi palvelu pettää!
			// todennäköisin syy on hakemuspalvelun ylikuormittumisessa!
			// Ylläpitäjä voi lukea logeista todellisen syyn!
			throw new RuntimeException("Osoitetarrojen luonnissa virhe!", e);

		}
	}

	@POST
	@Path("/jalkiohjauskirjeet/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi jälkiohjauskirjeiden luonnin valitsemattomille", response = Response.class)
	public ProsessiId aktivoiJalkiohjauskirjeidenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakuOid") String hakuOid) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			DokumenttiProsessi jalkiohjauskirjeetProsessi = new DokumenttiProsessi(
					"Jälkiohjauskirjeet", "Luo jälkiohjauskirjeet", hakuOid,
					tags("jalkiohjauskirjeet", hakemuksillaRajaus.getTag()));
			dokumenttiProsessiKomponentti
					.tuoUusiProsessi(jalkiohjauskirjeetProsessi);
			jalkiohjauskirjeBatchProxy.jalkiohjauskirjeetAktivoi(
					jalkiohjauskirjeetProsessi,
					hakemuksillaRajaus.getHakemusOids(), hakuOid,
					//
					SecurityContextHolder.getContext().getAuthentication());
			return jalkiohjauskirjeetProsessi.toProsessiId();
		} catch (Exception e) {
			LOG.error("Jälkiohjauskirjeiden luonnissa virhe! {}",
					e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(
					"Jälkiohjauskirjeiden luonti epäonnistui!", e);
		}
	}

	@POST
	@Path("/hyvaksymiskirjeet/aktivoi")
	@Consumes("application/json")
	@ApiOperation(value = "Aktivoi hyväksymiskirjeiden luonnin hakukohteelle haussa", response = Response.class)
	public ProsessiId aktivoiHyvaksymiskirjeidenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("sisalto") String sisalto,
			@QueryParam("templateName") String templateName,
			@QueryParam("tag") String tag,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			DokumenttiProsessi hyvaksymiskirjeetProsessi = new DokumenttiProsessi(
					"Hyväksymiskirjeet", "Hyväksymiskirjeet", hakuOid, tags(
							"hyvaksymiskirjeet", hakemuksillaRajaus.getTag()));
			dokumenttiProsessiKomponentti
					.tuoUusiProsessi(hyvaksymiskirjeetProsessi);
			hyvaksymiskirjeetRoute.hyvaksymiskirjeetAktivointi(
					hyvaksymiskirjeetProsessi,
					//
					tarjoajaOid, sisalto, templateName, tag,
					//
					hakukohdeOid, hakemuksillaRajaus.getHakemusOids(), hakuOid,
					sijoitteluajoId, SecurityContextHolder.getContext()
							.getAuthentication());
			return new ProsessiId(hyvaksymiskirjeetProsessi.getId());
		} catch (Exception e) {
			LOG.error("Hyväksymiskirjeiden luonnissa virhe! {}", e.getMessage());

			throw new RuntimeException(
					"Hyväksymiskirjeiden luonti epäonnistui! " + e.getMessage(),
					e);
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
	public ProsessiId aktivoiKoekutsukirjeidenLuonti(
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("valintakoeOids") List<String> valintakoeOids,
			DokumentinLisatiedot hakemuksillaRajaus) {
		DokumenttiProsessi kirjeProsessi = null;
		if (hakemuksillaRajaus != null
				&& hakemuksillaRajaus.getHakemusOids() != null
				&& !hakemuksillaRajaus.getHakemusOids().isEmpty()) {
			// luodaan koekutsukirjeet rajauksella
		} else {
			if (hakukohdeOid == null || valintakoeOids == null
					|| valintakoeOids.isEmpty()) {
				LOG.error("Valintakoe ja hakukohde on pakollisia tietoja koekutsukirjeen luontiin!");
				throw new RuntimeException(
						"Valintakoe ja hakukohde on pakollisia tietoja koekutsukirjeen luontiin!");
			}
		}
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			if (hakemuksillaRajaus.getHakemusOids() != null) {
				LOG.info(
						"Koekutsukirjeiden luonti aloitettu yksittaiselle hakemukselle {}",
						hakemuksillaRajaus.getHakemusOids());
				kirjeProsessi = new DokumenttiProsessi("Koekirjeet",
						"Koekirjeet valituille hakemuksille", null,
						Lists.newArrayList("koekutsukirjeet", "hakemuksille"));
			} else {
				LOG.info("Koekutsukirjeiden luonti aloitettu");
				kirjeProsessi = new DokumenttiProsessi(
						"Koekirjeet",
						"Koekirjeet valintakokeisiin osallistujille",
						null,
						Lists.newArrayList("koekutsukirjeet", "valintakokeelle"));
			}
			dokumenttiProsessiKomponentti.tuoUusiProsessi(kirjeProsessi); //
			koekutsukirjeRoute.koekutsukirjeetAktivointi(kirjeProsessi,
					hakemuksillaRajaus.getHakemusOids(), hakukohdeOid,
					valintakoeOids, hakemuksillaRajaus.getLetterBodyText(),
					SecurityContextHolder.getContext().getAuthentication());
		} catch (Exception e) {
			LOG.error("Koekutsukirjeiden luonti epäonnistui! {}",
					e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return new ProsessiId(kirjeProsessi.getId());// Response.ok().build();
	}

	private List<String> tags(String... tag) {
		List<String> l = Lists.newArrayList();
		for (String t : tag) {
			if (t != null) {
				l.add(t);
			}
		}
		return l;
	}
}
