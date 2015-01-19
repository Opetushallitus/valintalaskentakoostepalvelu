package fi.vm.sade.valinta.kooste.erillishaku.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

import fi.vm.sade.authentication.business.service.Authorizer;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuJson;
import fi.vm.sade.valinta.kooste.erillishaku.service.ErillishaunVientiService;
import fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiService;
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
	private static final Logger LOG = LoggerFactory.getLogger(ErillishakuResource.class);
	private static final String ROLE_APP_HAKEMUS_CRUD = "ROLE_APP_HAKEMUS_CRUD";

	public static final String POIKKEUS_TYHJA_DATAJOUKKO = "Syötteestä ei saatu poimittua yhtään hakijaa sijoitteluun tuotavaksi!";
	public static final String RIVIN_TUNNISTE_KAYTTOLIITTYMAAN = "Syöte"; // Datarivin tunniste käyttöliittymään
	public static final String POIKKEUS_VIALLINEN_DATAJOUKKO = "Syötteestä oli virheitä!";
	public static final String POIKKEUS_HENKILOPALVELUN_VIRHE = "Henkilöpalvelukutsu epäonnistui!";
	public static final String POIKKEUS_HAKEMUSPALVELUN_VIRHE = "Hakemuspalvelukutsu epäonnistui!";
	public static final String POIKKEUS_SIJOITTELUPALVELUN_VIRHE = "Sijoittelupalvelukutsu epäonnistui!";

	@Autowired
	private Authorizer authorizer;

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
	public ProsessiId vienti(
			@QueryParam("hakutyyppi") Hakutyyppi tyyppi,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@P("tarjoajaOid")
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("valintatapajonoOid") String valintatapajonoOid,
			@QueryParam("valintatapajononNimi") String valintatapajononNimi) {
		authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_APP_HAKEMUS_CRUD);
		ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
		dokumenttiKomponentti.tuoUusiProsessi(prosessi);
		//
		vientiService.vie(prosessi, new ErillishakuDTO(tyyppi,hakuOid, hakukohdeOid,
				tarjoajaOid, valintatapajonoOid, valintatapajononNimi));
		return prosessi.toProsessiId();
	}

	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
	@POST
	@Path("/tuonti")
	@Consumes("application/octet-stream")
	@ApiOperation(consumes = "application/json", value = "Erillishaun hakukohteen tuonti taulukkolaskennalla", response = ProsessiId.class)
	public ProsessiId tuonti(
			@QueryParam("hakutyyppi") Hakutyyppi tyyppi,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@P("tarjoajaOid")
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("valintatapajonoOid") String valintatapajonoOid,
			@QueryParam("valintatapajononNimi") String valintatapajononNimi,
			InputStream file) throws IOException {
		authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_APP_HAKEMUS_CRUD);
		ByteArrayOutputStream b;
		IOUtils.copy(file, b = new ByteArrayOutputStream());
		IOUtils.closeQuietly(file);
		ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
		dokumenttiKomponentti.tuoUusiProsessi(prosessi);
		tuontiService.tuoExcelistä(prosessi, new ErillishakuDTO(tyyppi, hakuOid, hakukohdeOid, tarjoajaOid, valintatapajonoOid, valintatapajononNimi), new ByteArrayInputStream(b.toByteArray()));
		//
		return prosessi.toProsessiId();
	}

	@PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
	@POST
	@Path("/tuonti/json")
	@Consumes("application/json")
	@ApiOperation(consumes = "application/json", value = "Erillishaun hakukohteen tuonti JSON-tietueella", response = ProsessiId.class)
	public ProsessiId tuontiJson(
			@ApiParam(allowableValues = "TOISEN_ASTEEN_OPPILAITOS,KORKEAKOULU")
			@QueryParam("hakutyyppi") Hakutyyppi tyyppi,
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@P("tarjoajaOid")
			@QueryParam("tarjoajaOid") String tarjoajaOid,
			@QueryParam("valintatapajonoOid") String valintatapajonoOid,
			@QueryParam("valintatapajononNimi") String valintatapajononNimi,
			@ApiParam("hakemuksenTila=[HYLATTY|VARALLA|PERUUNTUNUT|HYVAKSYTTY|VARASIJALTA_HYVAKSYTTY|HARKINNANVARAISESTI_HYVAKSYTTY|PERUNUT|PERUUTETTU]<br>" +
					"vastaanottoTila=[PERUNUT|KESKEN|EI_VASTAANOTTANUT_MAARA_AIKANA|VASTAANOTTANUT|VASTAANOTTANUT_SITOVASTI|PERUUTETTU]<br>" +
					"ilmoittautumisTila=[EI_TEHTY|LASNA_KOKO_LUKUVUOSI|POISSA_KOKO_LUKUVUOSI|EI_ILMOITTAUTUNUT|LASNA_SYKSY|POISSA_SYKSY|LASNA|POISSA]")
			// Body
			ErillishakuJson json) throws IOException {
		authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_APP_HAKEMUS_CRUD);
		ErillishakuProsessiDTO prosessi = new ErillishakuProsessiDTO(1);
		dokumenttiKomponentti.tuoUusiProsessi(prosessi);
		tuontiService.tuoJson(prosessi, new ErillishakuDTO(tyyppi, hakuOid, hakukohdeOid, tarjoajaOid, valintatapajonoOid, valintatapajononNimi), json.getRivit());
		//
		return prosessi.toProsessiId();
	}
}
