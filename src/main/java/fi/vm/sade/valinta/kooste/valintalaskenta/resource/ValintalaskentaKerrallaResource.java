package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
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
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
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
	@Path("/haku/{hakuOid}/tyyppi/{tyyppi}/whitelist/{whitelist}")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	public Vastaus valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid,
			@PathParam("tyyppi") LaskentaTyyppi tyyppi,
			@PathParam("whitelist") boolean whitelist, List<String> maski) {
		return kaynnistaLaskenta(hakuOid, new Maski(whitelist, maski), ((hoid,
				haunHakukohteetOids) -> {
			try {
				return seurantaResource.luoLaskenta(hoid, tyyppi,
						haunHakukohteetOids);
			} catch (Exception e) {
				LOG.error("Laskennan luonti haulle {} epaonnistui! {}\r\n{}",
						hoid, e.getMessage(),
						Arrays.toString(e.getStackTrace()));
				throw e;
			}
		}));
	}

	@GET
	@Path("/status/{uuid}")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
	public Laskenta status(@PathParam("uuid") String uuid) {
		return valintalaskentaValvomo.haeLaskenta(uuid);
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
			try {
				seurantaResource.merkkaaLaskennanTila(uuid,
						LaskentaTila.PERUUTETTU);
			} catch (Exception e) {
				LOG.error("Laskennan {} peruutus epaonnistui! {}\r\n{}", uuid,
						e.getMessage(), Arrays.toString(e.getStackTrace()));
				return Response
						.serverError()
						.entity("Laskennan peruutus epaonnistui! "
								+ e.getMessage()).build();
			}
		}
		return Response.ok().build();
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
		final LaskentaDto laskenta;
		try {
			laskenta = new GsonBuilder().create().fromJson(
					seurantaResource.resetoiTilat(uuid), LaskentaDto.class);
		} catch (Exception e) {
			LOG.error("Laskennan {} resetointi epaonnistui! {}\r\n{}", uuid,
					e.getMessage(), Arrays.toString(e.getStackTrace()));
			throw e;
		}
		// valmistumattomien hakukohteiden maski
		List<String> maski = laskenta.getHakukohteet().stream()
				.filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
				.map(h -> h.getHakukohdeOid()).collect(Collectors.toList());
		return kaynnistaLaskenta(laskenta.getHakuOid(), new Maski(true, maski),
				((hoid, haunHakukohteetOids) -> laskenta.getUuid()));
	}

	private Vastaus kaynnistaLaskenta(String hakuOid, Maski maski,
			BiFunction<String, List<String>, String> seurantaTunnus) {
		if (StringUtils.isBlank(hakuOid)) {
			LOG.error("HakuOid on pakollinen");
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
			if (haunHakukohteetOids.isEmpty()) {
				throw new RuntimeException(
						"Hakukohdemaskauksen jalkeen haulla ei ole hakukohteita! Ei voida aloittaa laskentaa hakukohteettomasti.");
			}
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
		final String uuid;
		try {
			uuid = seurantaResource.luoLaskenta(hakuOid,
					LaskentaTyyppi.HAKUKOHDE, hakukohteet);
		} catch (Exception e) {
			LOG.error(
					"Laskennan luonti epaonnistui haulle {} ja hakukohteelle {}! {}\r\n{}",
					hakuOid, hakukohdeOid, e.getMessage(),
					Arrays.toString(e.getStackTrace()));
			throw e;
		}
		AtomicBoolean lopetusehto = new AtomicBoolean(false);
		valintalaskentaRoute.suoritaValintalaskentaKerralla(
				new LaskentaJaHaku(new Laskenta(uuid, hakuOid, 1, lopetusehto,
						true), hakukohteet), lopetusehto);
		return Vastaus.uudelleenOhjaus(uuid);
	}

	private List<String> haunHakukohteet(String hakuOid) {
		if (StringUtils.isBlank(hakuOid)) {
			LOG.error("Yritettiin hakea hakukohteita ilman hakuOidia!");
			throw new RuntimeException(
					"Yritettiin hakea hakukohteita ilman hakuOidia!");
		}
		List<String> haunHakukohdeOidit = valintaperusteetResource
				.haunHakukohteet(hakuOid.trim())
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
		if (haunHakukohdeOidit.isEmpty()) {
			LOG.error(
					"Haulla {} ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?",
					hakuOid);
			throw new RuntimeException(
					"Haulla "
							+ hakuOid
							+ " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?");
		}
		return haunHakukohdeOidit;
	}
}
