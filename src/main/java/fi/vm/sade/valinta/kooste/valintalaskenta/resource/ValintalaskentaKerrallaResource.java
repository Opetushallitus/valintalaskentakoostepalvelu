package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import fi.vm.sade.valinta.seuranta.resource.LaskentaSeurantaResource;

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
	private LaskentaSeurantaResource seurantaResource;
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
			@QueryParam("valinnanvaihe") Integer valinnanvaihe,
			@QueryParam("valintakoelaskenta") Boolean valintakoelaskenta,
			@PathParam("tyyppi") LaskentaTyyppi tyyppi,
			@PathParam("whitelist") boolean whitelist, List<String> maski) {
		return kaynnistaLaskenta(hakuOid, new Maski(whitelist, maski), ((hoid,
				haunHakukohteetOids) -> {
			try {
				if (valinnanvaihe != null && valintakoelaskenta != null) {
					return seurantaResource.luoLaskenta(hoid, tyyppi,
							valinnanvaihe, valintakoelaskenta,
							haunHakukohteetOids);
				} else {
					return seurantaResource.luoLaskenta(hoid, tyyppi,
							haunHakukohteetOids);
				}
			} catch (Exception e) {
				LOG.error("Laskennan luonti haulle {} epaonnistui! {}\r\n{}",
						hoid, e.getMessage(),
						Arrays.toString(e.getStackTrace()));
				throw e;
			}
		}), valinnanvaihe, valintakoelaskenta);
	}

	@GET
	@Path("/status/{uuid}")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
	public Laskenta status(@PathParam("uuid") String uuid) {
		return valintalaskentaValvomo.haeLaskenta(uuid);
	}

	@GET
	@Path("/status/{uuid}/xls")
	@Produces("application/vnd.ms-excel")
	@ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
	public Response statusXls(final @PathParam("uuid") String uuid) {
		byte[] bytes = null;
		try {
			LaskentaDto laskenta = new Gson().fromJson(
					seurantaResource.laskenta(uuid), LaskentaDto.class);
			Map<String, Object[][]> sheetAndGrid = Maps.newHashMap();
			{
				List<Object[]> grid = Lists.newArrayList();
				grid.add(new Object[] { "Suorittamattomat hakukohteet" });
				for (HakukohdeDto hakukohde : laskenta.getHakukohteet()
						.stream()
						.filter(h -> !HakukohdeTila.VALMIS.equals(h.getTila()))
						.collect(Collectors.toList())) {
					List<String> rivi = Lists.newArrayList();
					rivi.add(hakukohde.getHakukohdeOid());
					rivi.addAll(hakukohde.getIlmoitukset().stream()
							.map(i -> i.getOtsikko())
							.collect(Collectors.toList()));
					grid.add(rivi.toArray());
					sheetAndGrid.put("Kesken", grid.toArray(new Object[][] {}));
				}

			}
			{
				List<Object[]> grid = Lists.newArrayList();
				grid.add(new Object[] { "Valmistuneet hakukohteet" });
				for (HakukohdeDto hakukohde : laskenta.getHakukohteet()
						.stream()
						.filter(h -> HakukohdeTila.VALMIS.equals(h.getTila()))
						.collect(Collectors.toList())) {
					grid.add(new Object[] { hakukohde.getHakukohdeOid() });
					sheetAndGrid
							.put("Valmiit", grid.toArray(new Object[][] {}));
				}
			}
			bytes = ExcelExportUtil.exportGridSheetsAsXlsBytes(sheetAndGrid);// GridAsXlsBytes(grid
			// .toArray(new Object[][] {}));
		} catch (Exception e) {
			LOG.error(
					"Excelin muodostus laskennan yhteenvedolle epaonnistui! {}\r\n{}",
					e.getMessage(), Arrays.toString(e.getStackTrace()));
			bytes = new byte[] {};
		}
		return Response.ok()
				.entity(bytes)
				//
				.header("Content-Length", bytes.length)
				//
				.header("Content-Type", "application/vnd.ms-excel")
				//
				.header("Content-Disposition",
						"attachment; filename=\"yhteenveto.xls\"").build();
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
				((hoid, haunHakukohteetOids) -> laskenta.getUuid()),
				laskenta.getValinnanvaihe(), laskenta.getValintakoelaskenta());
	}

	private Vastaus kaynnistaLaskenta(String hakuOid, Maski maski,
			BiFunction<String, List<String>, String> seurantaTunnus,
			Integer valinnanvaihe, Boolean valintakoelaskenta) {
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
		List<String> haunHakukohteetOids = null;
		if (maski.isWhitelist()) { // whitelistilla ohitetaan haun hakukohteiden
									// resolvaus
			haunHakukohteetOids = Lists.newArrayList(maski
					.getHakukohdeOidsMask());
		} else {
			haunHakukohteetOids = haunHakukohteet(hakuOid);
			if (maski.isMask()) {
				haunHakukohteetOids = Lists.newArrayList(maski
						.maskaa(haunHakukohteetOids));
				if (haunHakukohteetOids.isEmpty()) {
					throw new RuntimeException(
							"Hakukohdemaskauksen jalkeen haulla ei ole hakukohteita! Ei voida aloittaa laskentaa hakukohteettomasti.");
				}
			}
		}
		String uuid = seurantaTunnus.apply(hakuOid, haunHakukohteetOids);
		AtomicBoolean lopetusehto = new AtomicBoolean(false);
		valintalaskentaRoute.suoritaValintalaskentaKerralla(new LaskentaJaHaku(
				new Laskenta(uuid, hakuOid, haunHakukohteetOids.size(),
						lopetusehto, maski.isMask(), valinnanvaihe,
						valintakoelaskenta), haunHakukohteetOids), lopetusehto);
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
