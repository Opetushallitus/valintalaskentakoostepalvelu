package fi.vm.sade.tarjonta.service.resources.v1;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeHakutulosV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakutuloksetV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Wrapper luokalle
 *         fi.vm.sade.tarjonta.service.resources.v1.HakukohdeV1Resource
 * 
 * @QueryParam("") ei ole laillinen client annotaatio
 * 
 */
@Path("/v1/hakukohde")
public interface HakukohdeV1ResourceWrapper {

	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
	public ResultV1RDTO<HakutuloksetV1RDTO<HakukohdeHakutulosV1RDTO>> search(
			@QueryParam("hakuOid") String hakuOid,
			// @ApiParam(value = "Lista organisaatioiden oid:tä", required =
			// true) @QueryParam("organisationOid") List<String>
			// organisationOids,
			@QueryParam("tila") List<String> hakukohdeTilas);
	// @ApiParam(value = "Alkamiskausi", required = true)
	// @QueryParam("alkamisKausi") String alkamisKausi,
	// @ApiParam(value = "Alkamisvuosi", required = true)
	// @QueryParam("alkamisVuosi") Integer alkamisVuosi,
	// @ApiParam(value = "Hakukohteen oid", required = true)
	// @QueryParam("hakukohdeOid") String hakukohdeOid,
	// @ApiParam(value = "Lista koulutusasteen tyyppejä", required = true)
	// @QueryParam("koulutusastetyyppi") List<KoulutusasteTyyppi>
	// koulutusastetyyppi);

}
