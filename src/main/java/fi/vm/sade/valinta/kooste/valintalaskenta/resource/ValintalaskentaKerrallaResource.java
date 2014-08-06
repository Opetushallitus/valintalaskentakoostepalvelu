package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;

import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
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
	@Autowired
	private ValintaperusteetResource valintaperusteetResource;

	/**
	 * Koko haun laskenta
	 * 
	 * @param hakuOid
	 * @return
	 */
	@POST
	@Path("/haku/{hakuOid}/whitelist/{whitelist}")
	@Consumes(APPLICATION_JSON)
	public Response valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid,
			@PathParam("whitelist") boolean whitelist, List<String> maski) {
		return kaynnistaLaskenta(hakuOid, new Maski(whitelist, maski));
	}

	/**
	 * Koko haun laskenta
	 * 
	 * @param hakuOid
	 * @return
	 */
	@POST
	@Path("/haku/{hakuOid}")
	@Consumes(APPLICATION_JSON)
	public Response valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid) {
		return kaynnistaLaskenta(hakuOid, new Maski());
	}

	private Response kaynnistaLaskenta(String hakuOid, Maski maski) {
		if (hakuOid == null) {
			return Response.serverError().entity("HakuOid on pakollinen")
					.build();
		}
		LOG.warn("Pyynto suorittaa valintalaskenta haulle {}", hakuOid);
		Collection<YhteenvetoDto> kaynnissaOlevatLaskennat = seurantaResource
				.haeKaynnissaOlevatLaskennat(hakuOid);

		if (kaynnissaOlevatLaskennat.isEmpty()) {
			List<String> haunHakukohteetOids = haunHakukohteet(hakuOid);
			if (maski.isMask()) {
				haunHakukohteetOids = Lists.newArrayList(maski
						.maskaa(haunHakukohteetOids));
			}
			String uuid = seurantaResource.luoLaskenta(hakuOid,
					haunHakukohteetOids);

			AtomicBoolean lopetusehto = new AtomicBoolean(false);
			valintalaskentaRoute.suoritaValintalaskentaKerralla(
					new LaskentaJaHaku(new Laskenta(uuid, hakuOid,
							haunHakukohteetOids.size(), lopetusehto),
							haunHakukohteetOids), lopetusehto);
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
	@Consumes(APPLICATION_JSON)
	public Response valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid,
			@PathParam("hakukohdeOid") String hakukohdeOid) {
		if (hakuOid == null || hakukohdeOid == null) {
			return Response.serverError()
					.entity("HakuOid ja hakukohdeOid on pakollinen").build();
		}
		LOG.warn("Pyynto suorittaa valintalaskenta haun {} hakukohteelle {}",
				hakuOid, hakukohdeOid);
		String uuid = UUID.randomUUID().toString();
		AtomicBoolean lopetusehto = new AtomicBoolean(false);
		valintalaskentaRoute
				.suoritaValintalaskentaKerralla(new LaskentaJaHaku(
						new Laskenta(uuid, hakuOid, 1, false, lopetusehto),
						Arrays.asList(hakukohdeOid)), lopetusehto);
		return Response.ok().entity(uuid).build();
	}

	private List<String> haunHakukohteet(String hakuOid) {
		return valintaperusteetResource
				.haunHakukohteet(hakuOid)
				.stream()
				.filter((h -> {
					boolean julkaistu = "JULKAISTU".equals(h.getTila());
					if (!julkaistu) {
						LOG.warn(
								"Ohitetaan hakukohde {} koska sen tila on {}.",
								h.getOid(), h.getTila());
					}
					return julkaistu;
				})).map(u -> u.getOid()).collect(Collectors.toList());
	}
}
