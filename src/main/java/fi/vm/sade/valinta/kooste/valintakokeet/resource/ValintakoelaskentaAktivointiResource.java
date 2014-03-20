package fi.vm.sade.valinta.kooste.valintakokeet.resource;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.valintakokeet.dto.ValintakoeCache;
import fi.vm.sade.valinta.kooste.valintakokeet.dto.ValintakoeProsessi;
import fi.vm.sade.valinta.kooste.valintakokeet.route.ValintakoelaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

/**
 * User: wuoti Date: 2.9.2013 Time: 12.28
 */
@Controller
@Path("valintakoelaskenta")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintakoelaskenta", description = "Valintakoelaskennan aktivointi")
public class ValintakoelaskentaAktivointiResource {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintakoelaskentaAktivointiResource.class);

	@Autowired
	private ValintakoelaskentaMuistissaRoute valintakoelaskentaRoute;
	@Autowired
	private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;

	/**
	 * Jos hakukohdeOid on annettu niin ainoastaan annetulle hakukohteelle
	 * tehdään valintakoelaskenta
	 * 
	 * @param hakuOid
	 *            pakollinen
	 * @param hakukohdeOid
	 *            valinnainen
	 * @return
	 */
	@POST
	@Path("/aktivoiValintakoelaskenta")
	@ApiOperation(value = "Aktivoi valintakoelaskenta koko haulle", response = ProsessiId.class)
	public ProsessiId aktivoiValintakoelaskenta(
			@QueryParam("hakuOid") String hakuOid,
			@QueryParam("hakukohdeOid") String hakukohdeOid) {
		if (hakuOid == null) {
			throw new RuntimeException("HakuOid-parametri on pakollinen");
		} else {
			LOG.info("Valintakoelaskenta for haku {}", hakuOid);
			ValintakoeProsessi prosessi = new ValintakoeProsessi(hakuOid);
			valintakoelaskentaRoute.aktivoiValintakoelaskenta(
					new ValintakoeCache(), prosessi, hakuOid, hakukohdeOid,
					SecurityContextHolder.getContext().getAuthentication());
			dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
			return prosessi.toProsessiId(); // "in progress";
		}
	}

}
