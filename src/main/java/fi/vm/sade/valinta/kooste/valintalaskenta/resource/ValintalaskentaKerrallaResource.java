package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.wordnik.swagger.annotations.Api;

import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.resource.SeurantaResource;

/**
 * 
 * @author Jussi Jartamo
 * 
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
	@Autowired
	private ValintalaskentaKerrallaRouteValvomo valintalaskentaValvomo;

	/**
	 * Koko haun laskenta
	 * 
	 * @param hakuOid
	 * @return
	 */
	@POST
	@Path("/haku/{hakuOid}/whitelist/{whitelist}")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	public Vastaus valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid,
			@PathParam("whitelist") boolean whitelist, List<String> maski) {
		return kaynnistaLaskenta(hakuOid, new Maski(whitelist, maski), ((hoid,
				haunHakukohteetOids) -> seurantaResource.luoLaskenta(hoid,
				haunHakukohteetOids)));
	}

	/**
	 * Sammutta laskennan uuid:lla jos laskenta on kaynnissa
	 * 
	 * @param uuid
	 * @return 200 OK
	 */
	@DELETE
	@Path("/haku/{uuid}")
	public Response lopetaLaskenta(@PathParam("uuid") String uuid) {
		if (uuid == null) {
			return Response.serverError().entity("Uuid on pakollinen").build();
		}
		Laskenta l = valintalaskentaValvomo.haeLaskenta(uuid);
		if (l != null) {
			l.getLopetusehto().set(true); // aktivoidaan lopetuskasky
			seurantaResource
					.merkkaaLaskennanTila(uuid, LaskentaTila.PERUUTETTU);
		}
		return Response.ok().build();
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
	@Produces(APPLICATION_JSON)
	public Vastaus valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid) {
		return kaynnistaLaskenta(hakuOid, new Maski(), ((hoid,
				haunHakukohteetOids) -> seurantaResource.luoLaskenta(hoid,
				haunHakukohteetOids)));
	}

	/**
	 * Uudelleen aja vanha haku
	 * 
	 * @param hakuOid
	 * @return
	 */
	@POST
	@Path("/uudelleenyrita/{uuid}")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	public Vastaus uudelleenajoLaskennalle(@PathParam("uuid") String uuid) {
		final LaskentaDto laskenta = // seurantaResource.laskenta(uuid);
		seurantaResource.resetoiTilat(uuid);
		// valmistumattomien hakukohteiden maski
		List<String> maski = laskenta.getHakukohteet().stream()
				.filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
				.map(h -> h.getHakukohdeOid()).collect(Collectors.toList());
		return kaynnistaLaskenta(uuid, new Maski(true, maski), ((hoid,
				haunHakukohteetOids) -> laskenta.getUuid()));
	}

	private Vastaus kaynnistaLaskenta(String hakuOid, Maski maski,
			BiFunction<String, List<String>, String> seurantaTunnus) {
		if (hakuOid == null) {
			throw new RuntimeException("HakuOid on pakollinen");
		}
		// maskilla kaynnistettaessa luodaan aina uusi laskenta
		if (!maski.isMask()) { // muuten tarkistetaan onko laskenta jo olemassa
			// Kaynnissa oleva laskenta koko haulle
			Optional<Laskenta> ajossaOlevaLaskentaHaulle = valintalaskentaValvomo
					.ajossaOlevatLaskennat().stream()
					// Tama haku ...
					.filter(l -> hakuOid.equals(l.getHakuOid())
					// .. ja koko haun laskennasta on kyse
							&& !l.isOsittainenLaskenta()).findFirst();
			if (ajossaOlevaLaskentaHaulle.isPresent()) {
				// palautetaan seurattavaksi ajossa olevan hakukohteen
				// seurantatunnus
				String uuid = ajossaOlevaLaskentaHaulle.get().getUuid();
				LOG.warn(
						"Laskenta on jo kaynnissa haulle {} joten palautetaan seurantatunnus({}) ajossa olevaan hakuun",
						hakuOid, uuid);
				return Vastaus.uudelleenOhjaus(uuid);
			}
		}
		LOG.info("Aloitetaan laskenta haulle {}", hakuOid);
		List<String> haunHakukohteetOids = haunHakukohteet(hakuOid);
		if (maski.isMask()) {
			haunHakukohteetOids = Lists.newArrayList(maski
					.maskaa(haunHakukohteetOids));
		}
		String uuid = seurantaTunnus.apply(hakuOid, haunHakukohteetOids);
		AtomicBoolean lopetusehto = new AtomicBoolean(false);
		valintalaskentaRoute.suoritaValintalaskentaKerralla(new LaskentaJaHaku(
				new Laskenta(uuid, hakuOid, haunHakukohteetOids.size(),
						lopetusehto, maski.isMask()), haunHakukohteetOids),
				lopetusehto);
		return Vastaus.uudelleenOhjaus(uuid);
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
	@Produces(APPLICATION_JSON)
	public Vastaus valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid,
			@PathParam("hakukohdeOid") String hakukohdeOid) {
		if (hakuOid == null || hakukohdeOid == null) {
			throw new RuntimeException("HakuOid ja hakukohdeOid on pakollinen");
		}
		LOG.info("Pyynto suorittaa valintalaskenta haun {} hakukohteelle {}",
				hakuOid, hakukohdeOid);
		List<String> hakukohteet = Arrays.asList(hakukohdeOid);
		String uuid = seurantaResource.luoLaskenta(hakuOid, hakukohteet);
		AtomicBoolean lopetusehto = new AtomicBoolean(false);
		valintalaskentaRoute.suoritaValintalaskentaKerralla(
				new LaskentaJaHaku(new Laskenta(uuid, hakuOid, 1, lopetusehto,
						true), hakukohteet), lopetusehto);
		return Vastaus.uudelleenOhjaus(uuid);
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
