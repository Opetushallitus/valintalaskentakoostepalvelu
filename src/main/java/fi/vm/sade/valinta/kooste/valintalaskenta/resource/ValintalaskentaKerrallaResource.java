package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.wordnik.swagger.annotations.Api;

import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaMaski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.seuranta.dto.YhteenvetoDto;
import fi.vm.sade.valinta.seuranta.resource.SeurantaResource;

/**
 * @author Jussi Jartamo
 */
@Controller
@Path("valintalaskentakerralla")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentakerralla", description = "Valintalaskenta kaikille valinnanvaiheille kerralla")
public class ValintalaskentaKerrallaResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaKerrallaResource.class);

	@Autowired
	private SeurantaResource seurantaResource;
	@Autowired
	private ValintalaskentaKerrallaRoute valintalaskentaRoute;

	/**
	 * Koko haun laskenta
	 * 
	 * @param hakuOid
	 * @return
	 */
	@POST
	@Path("/haku/{hakuOid}")
	public Response valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid) {
		if (hakuOid == null) {
			return Response.serverError().entity("HakuOid on pakollinen")
					.build();
		}
		LOG.warn("Pyynto suorittaa valintalaskenta haulle {}", hakuOid);
		Collection<YhteenvetoDto> kaynnissaOlevatLaskennat = seurantaResource
				.haeKaynnissaOlevatLaskennat(hakuOid);

		if (kaynnissaOlevatLaskennat.isEmpty()) {
			String uuid = (String) seurantaResource.luoLaskenta(hakuOid,
					Collections.emptyList()).getEntity();
			valintalaskentaRoute
					.suoritaValintalaskentaKerralla(new LaskentaJaMaski(
							new Laskenta(uuid, hakuOid)));
			return Response.ok().entity(uuid).build();
		} else {
			String uuid = kaynnissaOlevatLaskennat.iterator().next().getUuid();
			return Response.ok().entity(uuid).build();
		}
	}

	/**
	 * Yksittaisen hakukohteen laskenta. Ei merkata seurantaan.
	 * 
	 * @param hakuOid
	 * @param hakukohdeOid
	 * @return laskennan uuid
	 */
	@POST
	@Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
	public Response valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid,
			@PathParam("hakukohdeOid") String hakukohdeOid) {
		if (hakuOid == null || hakukohdeOid == null) {
			return Response.serverError()
					.entity("HakuOid ja hakukohdeOid on pakollinen").build();
		}
		LOG.warn("Pyynto suorittaa valintalaskenta haun {} hakukohteelle {}",
				hakuOid, hakukohdeOid);
		String uuid = UUID.randomUUID().toString();
		valintalaskentaRoute
				.suoritaValintalaskentaKerralla(new LaskentaJaMaski(
						new Laskenta(uuid, hakuOid, false)));
		return Response.ok().entity(uuid).build();
	}
}
