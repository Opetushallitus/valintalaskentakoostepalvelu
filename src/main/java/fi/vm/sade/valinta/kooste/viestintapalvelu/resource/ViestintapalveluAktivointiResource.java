package fi.vm.sade.valinta.kooste.viestintapalvelu.resource;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.google.common.collect.Sets;
import fi.vm.sade.valinta.kooste.viestintapalvelu.service.OsoitetarratService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumentinLisatiedot;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.HyvaksymiskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.JalkiohjauskirjeDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuDTO;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KoekutsuProsessiImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeetService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeetService;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Ei palauta PDF-tiedostoa vaan URI:n varsinaiseen resurssiin - koska
 *         AngularJS resurssin palauttaman datan konvertoiminen selaimen
 *         ladattavaksi tiedostoksi on ongelmallista (mutta ei mahdotonta - onko
 *         tarpeen?).
 */
@Controller("ViestintapalveluAktivointiResource")
@Path("viestintapalvelu")
@PreAuthorize("isAuthenticated()")
@Api(value = "/viestintapalvelu", description = "Osoitetarrojen, jälkiohjauskirjeiden ja hyväksymiskirjeiden tuottaminen")
public class ViestintapalveluAktivointiResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(ViestintapalveluAktivointiResource.class);

	@Autowired
	private OsoitetarratService osoitetarratService;
	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
	@Autowired
	private KoekutsukirjeetService koekutsukirjeetService;
	@Autowired
	private HyvaksymiskirjeetService hyvaksymiskirjeetService;
	@Autowired
	private JalkiohjauskirjeService jalkiohjauskirjeService;

	@POST
	@Path("/osoitetarrat/aktivoi")
	@Consumes("application/json")
	@Produces("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi osoitetarrojen luonnin hakukohteelle", response = Response.class)
	public ProsessiId aktivoiOsoitetarrojenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakuOid") String hakuOid,
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

			if(hakemuksillaRajaus.getHakemusOids() != null) {
				osoitetarratService.osoitetarratHakemuksille(osoiteProsessi, hakemuksillaRajaus.getHakemusOids());
			} else {
				osoitetarratService.osoitetarratValintakokeeseenOsallistujille(
						osoiteProsessi,
						hakuOid,
						hakukohdeOid, Sets.newHashSet(valintakoeOids));
			}
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
	@Produces("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
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
			if(hakemuksillaRajaus.getHakemusOids() != null) {
				osoitetarratService.osoitetarratHakemuksille(osoiteProsessi, hakemuksillaRajaus.getHakemusOids());
			} else {
				osoitetarratService.osoitetarratSijoittelussaHyvaksytyille(osoiteProsessi,hakuOid,hakukohdeOid);
			}
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
	@Produces("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
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
			osoitetarratService.osoitetarratHakemuksille(osoiteProsessi,
					hakemuksillaRajaus.getHakemusOids());

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
	@Produces("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi jälkiohjauskirjeiden luonnin valitsemattomille", response = Response.class)
	public ProsessiId aktivoiJalkiohjauskirjeidenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("templateName") String templateName,
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("tag") String tag) {
		try {
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			KoekutsuProsessiImpl prosessi = new KoekutsuProsessiImpl(2);
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			LOG.warn("Luodaan jälkiohjauskirjeet kielellä {}. Onko {} == {}",
					hakemuksillaRajaus.getLanguageCode(), KieliUtil.RUOTSI,
					KieliUtil.RUOTSI.equals(hakemuksillaRajaus
							.getLanguageCode()));
			JalkiohjauskirjeDTO jalkiohjauskirjeDTO = new JalkiohjauskirjeDTO(
					tarjoajaOid, hakemuksillaRajaus.getLetterBodyText(),
					templateName, tag, hakuOid,
					hakemuksillaRajaus.getLanguageCode());
			if (hakemuksillaRajaus.getHakemusOids() == null) {
				jalkiohjauskirjeService.jalkiohjauskirjeetHakukohteelle(
						prosessi, jalkiohjauskirjeDTO);
			} else {
				jalkiohjauskirjeService.jalkiohjauskirjeetHakemuksille(
						prosessi, jalkiohjauskirjeDTO,
						hakemuksillaRajaus.getHakemusOids());
			}
			return prosessi.toProsessiId();
		} catch (Exception e) {
			LOG.error("Jälkiohjauskirjeiden luonnissa virhe! {}",
					e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(
					"Jälkiohjauskirjeiden luonti epäonnistui!", e);
		}
	}
	@POST
	@Path("/hakukohteessahylatyt/aktivoi")
	@Consumes("application/json")
	@Produces("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi hakukohteessa hylatyille kirjeiden luonnin", response = Response.class)
	public ProsessiId aktivoiHakukohteessahylatyilleLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("templateName") String templateName,
			@QueryParam("palautusAika") String palautusAika,
			@QueryParam("palautusPvm") String palautusPvm,
			@QueryParam("tag") String tag,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
		try {
			if (templateName == null) {
				templateName = "jalkiohjauskirje";
			}
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			tag = hakemuksillaRajaus.getTag();
			KoekutsuProsessiImpl prosessi = new KoekutsuProsessiImpl(2);
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			HyvaksymiskirjeDTO hyvaksymiskirjeDTO = new HyvaksymiskirjeDTO(
					tarjoajaOid, hakemuksillaRajaus.getLetterBodyText(),
					templateName, tag, hakukohdeOid, hakuOid, sijoitteluajoId,palautusPvm,palautusAika);
			hyvaksymiskirjeetService.jalkiohjauskirjeHakukohteelle(prosessi, hyvaksymiskirjeDTO);
			return prosessi.toProsessiId();
		} catch (Exception e) {
			LOG.error("Hyväksymiskirjeiden luonnissa virhe! {}", e.getMessage());

			throw new RuntimeException(
					"Hyväksymiskirjeiden luonti epäonnistui! " + e.getMessage(),
					e);
		}
	}
	@POST
	@Path("/hyvaksymiskirjeet/aktivoi")
	@Consumes("application/json")
	@Produces("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi hyväksymiskirjeiden luonnin hakukohteelle haussa", response = Response.class)
	public ProsessiId aktivoiHyvaksymiskirjeidenLuonti(
	/* OPTIONAL */DokumentinLisatiedot hakemuksillaRajaus,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("palautusAika") String palautusAika,
			@QueryParam("palautusPvm") String palautusPvm,
			@QueryParam("templateName") String templateName,
			@QueryParam("tag") String tag,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("sijoitteluajoId") Long sijoitteluajoId) {
		try {
			if (templateName == null) {
				templateName = "hyvaksymiskirje";
			}
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			tag = hakemuksillaRajaus.getTag();
			KoekutsuProsessiImpl prosessi = new KoekutsuProsessiImpl(2);
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			HyvaksymiskirjeDTO hyvaksymiskirjeDTO = new HyvaksymiskirjeDTO(
					tarjoajaOid, hakemuksillaRajaus.getLetterBodyText(),
					templateName, tag, hakukohdeOid, hakuOid, sijoitteluajoId,palautusPvm,palautusAika);
			if (hakemuksillaRajaus.getHakemusOids() == null) {
				hyvaksymiskirjeetService.hyvaksymiskirjeetHakukohteelle(
						prosessi, hyvaksymiskirjeDTO);
			} else {
				hyvaksymiskirjeetService.hyvaksymiskirjeetHakemuksille(
						prosessi, hyvaksymiskirjeDTO,
						hakemuksillaRajaus.getHakemusOids());
			}
			return prosessi.toProsessiId();
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
	 * @return 200 OK
	 */
	@POST
	@Path("/koekutsukirjeet/aktivoi")
	@Consumes("application/json")
	@Produces("application/json")
	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_OPO')")
	@ApiOperation(value = "Aktivoi koekutsukirjeiden luonnin hakukohteelle haussa", response = Response.class)
	public ProsessiId aktivoiKoekutsukirjeidenLuonti(
			@QueryParam(OPH.HAKUOID) String hakuOid,
			@QueryParam(OPH.HAKUKOHDEOID) String hakukohdeOid,
			@QueryParam(OPH.TARJOAJAOID) String tarjoajaOid,
			@QueryParam("templateName") String templateName,
			@QueryParam("valintakoeOids") List<String> valintakoeOids,
			DokumentinLisatiedot hakemuksillaRajaus) {
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
		String tag = null;
		KoekutsuProsessiImpl prosessi = new KoekutsuProsessiImpl(2);
		try {
			if (templateName == null) {
				templateName = "koekutsukirje";
			}
			if (hakemuksillaRajaus == null) {
				hakemuksillaRajaus = new DokumentinLisatiedot();
			}
			tag = hakemuksillaRajaus.getTag();

			if (hakemuksillaRajaus.getHakemusOids() != null) {
				LOG.error(
						"Koekutsukirjeiden luonti aloitettu yksittaiselle hakemukselle {}",
						hakemuksillaRajaus.getHakemusOids());
				koekutsukirjeetService.koekutsukirjeetHakemuksille(prosessi,
						new KoekutsuDTO(hakemuksillaRajaus.getLetterBodyText(),
								tarjoajaOid, tag, hakukohdeOid, hakuOid,
								templateName), hakemuksillaRajaus
								.getHakemusOids());
			} else {
				LOG.error("Koekutsukirjeiden luonti aloitettu");
				koekutsukirjeetService.koekutsukirjeetOsallistujille(prosessi,
						new KoekutsuDTO(hakemuksillaRajaus.getLetterBodyText(),
								tarjoajaOid, tag, hakukohdeOid, hakuOid,
								templateName), valintakoeOids);
			}
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
		} catch (Exception e) {
			LOG.error("Koekutsukirjeiden luonti epäonnistui! {}",
					e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		return new ProsessiId(prosessi.getId());// Response.ok().build();
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
