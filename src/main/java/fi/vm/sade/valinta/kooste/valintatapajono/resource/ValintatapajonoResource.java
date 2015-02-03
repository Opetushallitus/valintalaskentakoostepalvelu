package fi.vm.sade.valinta.kooste.valintatapajono.resource;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoTuontiRoute;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
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

	private final Logger LOG = LoggerFactory
			.getLogger(ValintatapajonoResource.class);
	@Autowired
	private ValintatapajonoVientiRoute valintatapajonoVienti;
	@Autowired
	private ValintatapajonoTuontiRoute valintatapajonoTuonti;
	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	@PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
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

	@PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
	@POST
	@Path("/tuonti")
	@Consumes("application/octet-stream")
	@ApiOperation(consumes = "application/json", value = "Valintatapajonon vienti taulukkolaskentaan", response = ProsessiId.class)
	public ProsessiId tuonti(@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid,
			@QueryParam("valintatapajonoOid") String valintatapajonoOid,
			InputStream file) throws IOException {
		DokumenttiProsessi prosessi = new DokumenttiProsessi("Valintatapajono",
				"tuonti", hakuOid, Arrays.asList(hakukohdeOid));
		try {
			ByteArrayOutputStream b;
			IOUtils.copy(file, b = new ByteArrayOutputStream());
			IOUtils.closeQuietly(file);
			LOG.info(
					"Käynnistetään tuonti! Hakukohde({}), haku({}), valintatapajono({})",
					hakukohdeOid, hakuOid, valintatapajonoOid);
			valintatapajonoTuonti.tuo(
					new ByteArrayInputStream(b.toByteArray()), prosessi,
					hakuOid, hakukohdeOid, valintatapajonoOid);
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			return prosessi.toProsessiId();
		} catch (Exception e) {
			LOG.error("Tuonnin käynnistys epäonnistui {}\r\n{}",
					e.getMessage(), Arrays.toString(e.getStackTrace()));
			prosessi.getPoikkeukset().add(
					new Poikkeus(Poikkeus.KOOSTEPALVELU, "Tuntematon virhe "
							+ e.getMessage()));
			return prosessi.toProsessiId();
		}
	}
}
