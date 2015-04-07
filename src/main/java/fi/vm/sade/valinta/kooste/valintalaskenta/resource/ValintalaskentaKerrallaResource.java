package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.dto.HakukohdeJaOrganisaatio;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.excel.LaskentaDtoAsExcel;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeDto;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Controller("ValintalaskentaKerrallaResource")
@Path("valintalaskentakerralla")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentakerralla", description = "Valintalaskenta kaikille valinnanvaiheille kerralla")
public class ValintalaskentaKerrallaResource {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaKerrallaResource.class);

	@Autowired
	private LaskentaSeurantaAsyncResource seurantaAsyncResource;
	@Autowired
	private ValintalaskentaKerrallaRoute valintalaskentaRoute;
	@Autowired
	private ValintaperusteetAsyncResource valintaperusteetAsyncResource;
	@Autowired
	private ValintalaskentaKerrallaRouteValvomo valintalaskentaValvomo;
	@Autowired
	private OhjausparametritAsyncResource ohjausparametritAsyncResource;

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
	public void valintalaskentaHaulle(@PathParam("hakuOid") String hakuOid,
			@QueryParam("erillishaku") Boolean erillishaku,
			@QueryParam("valinnanvaihe") Integer valinnanvaihe,
			@QueryParam("valintakoelaskenta") Boolean valintakoelaskenta,
			@PathParam("tyyppi") LaskentaTyyppi tyyppi,
			@PathParam("whitelist") boolean whitelist, List<String> maski,
			@Suspended AsyncResponse asyncResponse) {
		try {
			asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
			asyncResponse.setTimeoutHandler(new TimeoutHandler() {
				public void handleTimeout(AsyncResponse asyncResponse) {
					String hakukohdeOids = null;
					if (maski != null && !maski.isEmpty()) {
						try {
							Object[] hakukohdeOidArray = maski.toArray();
							StringBuilder sb = new StringBuilder();
							sb.append(Arrays.toString(Arrays.copyOfRange(
									hakukohdeOidArray, 0,
									Math.min(hakukohdeOidArray.length, 10))));
							if (hakukohdeOidArray.length > 10) {
								sb.append(
										" ensimmaiset 10 hakukohdetta maskissa jossa on yhteensa hakukohteita ")
										.append(hakukohdeOidArray.length);
							} else {
								sb.append(" maskin hakukohteet");
							}
							hakukohdeOids = sb.toString();
						} catch (Exception e) {
							hakukohdeOids = e.getMessage();
						}
					}
					LOG.error(
							"Laskennan kaynnistys timeuottasi kutsulle /haku/{}/tyyppi/{}/whitelist/{}?valinnanvaihe={}&valintakoelaskenta={}\r\n{}",
							hakuOid, tyyppi, whitelist, valinnanvaihe,
							valintakoelaskenta, hakukohdeOids, hakukohdeOids);
					asyncResponse.resume(Response.serverError()
							.entity("Uudelleen ajo laskennalle aikakatkaistu!")
							.build());
				}
			});
			kaynnistaLaskenta(
					tyyppi,
					hakuOid,
					new Maski(whitelist, maski),
					(hakukohdeOids, laskennanAloitus) -> {
						List<HakukohdeDto> hakukohdeDtos = hakukohdeOids
								.stream()
								.filter(hk -> {
									if (hk == null) {
										LOG.error("Null referenssi hakukohdeOidsien joukossa laskentaa luotaessa!");
										return false;
									}
									if (hk.getHakukohdeOid() == null) {
										LOG.error(
												"HakukohdeOid oli null laskentaa luotaessa! OrganisaatioOid == {}, joten hakukohde ohitetaan!",
												hk.getOrganisaatioOid());
										return false;
									}
									if (hk.getOrganisaatioOid() == null) {
										LOG.error(
												"OrganisaatioOid oli null laskentaa luotaessa! HakukohdeOid == {}, joten hakukohde ohitetaan!",
												hk.getHakukohdeOid());
										return false;
									}
									return true;
								})
								.map(hk -> new HakukohdeDto(hk
										.getHakukohdeOid(), hk
										.getOrganisaatioOid()))
								.collect(Collectors.toList());
						if (hakukohdeDtos.isEmpty()
								|| hakukohdeDtos.size() == 0) {
							LOG.error("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!");
							asyncResponse
									.resume(Response
											.serverError()
											.entity("Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!")
											.build());
							throw new RuntimeException(
									"Laskentaa ei voida aloittaa hakukohteille joilta puuttuu organisaatio!");
						} else {
							if(hakukohdeDtos.size() < hakukohdeOids.size()) {
								LOG.warn("Hakukohteita puuttuvien organisaatio-oidien vuoksi filtteroinnin jalkeen {}/{}!", hakukohdeDtos.size(), hakukohdeOids.size());
							}
							else {
								LOG.info("Hakukohteita filtteroinnin jalkeen {}/{}!", hakukohdeDtos.size(), hakukohdeOids.size());
							}
							seurantaAsyncResource.luoLaskenta(
									hakuOid,
									tyyppi,
									erillishaku,
									valinnanvaihe,
									valintakoelaskenta,
									hakukohdeDtos,
									uuid -> {
										if (uuid == null) {
											LOG.error("Laskentaa ei saatu luotua!");
											asyncResponse
													.resume(Response
															.serverError()
															.entity("Laskentaa ei saatu luotua!")
															.build());
											throw new RuntimeException(
													"Laskentaa ei saatu luotua!");
										}
										try {
											laskennanAloitus.accept(uuid);
										} catch (Throwable e) {
											LOG.error(
													"Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}",
													e.getMessage());
											asyncResponse
													.resume(Response
															.serverError()
															.entity("Odottamaton virhe laskennan kaynnistamisessa! "
																	+ e.getMessage())
															.build());
											throw e;
										}
									},
									poikkeus -> {
										LOG.error(
												"Seurannasta uuden laskennan haku paatyi virheeseen: {}",
												poikkeus.getMessage());
										asyncResponse.resume(Response
												.serverError()
												.entity(poikkeus.getMessage())
												.build());
									});
						}
					}, Boolean.TRUE.equals(erillishaku),
					LaskentaTyyppi.VALINTARYHMA.equals(tyyppi), valinnanvaihe,
					valintakoelaskenta, asyncResponse);
		} catch (Throwable e) {
			LOG.error(
					"Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}",
					e.getMessage());
			asyncResponse.resume(Response
					.serverError()
					.entity("Odottamaton virhe laskennan kaynnistamisessa! "
							+ e.getMessage()).build());
			throw e;
		}
	}

	/**
	 * Uudelleen aja vanha haku
	 *
	 * @return
	 */
	@POST
	@Path("/uudelleenyrita/{uuid}")
	@Consumes(APPLICATION_JSON)
	@Produces(APPLICATION_JSON)
	public void uudelleenajoLaskennalle(@PathParam("uuid") String uuid,
			@Suspended AsyncResponse asyncResponse) {
		try {
			asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
			asyncResponse.setTimeoutHandler(new TimeoutHandler() {
				public void handleTimeout(AsyncResponse asyncResponse) {
					LOG.error("Uudelleen ajo laskennalle({}) timeouttasi!",
							uuid);
					asyncResponse.resume(Response.serverError()
							.entity("Uudelleen ajo laskennalle timeouttasi!")
							.build());
				}
			});
			Laskenta l = valintalaskentaValvomo.haeLaskenta(uuid);
			if (l != null && !l.isValmis()) {
				LOG.warn(
						"Laskenta {} on viela ajossa, joten palautetaan linkki siihen.",
						uuid);
				asyncResponse.resume(Response.ok(Vastaus.uudelleenOhjaus(uuid))
						.build());
			}
			seurantaAsyncResource
					.resetoiTilat(
							uuid,
							laskenta -> {
								try {
									List<HakukohdeJaOrganisaatio> maski = laskenta
											.getHakukohteet()
											.stream()
											.filter(h -> !HakukohdeTila.VALMIS
													.equals(h.getTila()))
											.map(h -> new HakukohdeJaOrganisaatio(
													h.getHakukohdeOid(),
													h.getOrganisaatioOid()))
											.collect(Collectors.toList());
									kaynnistaLaskenta(
											laskenta.getTyyppi(),
											laskenta.getHakuOid(),
											new Maski(
													true,
													maski.stream()
															.map(hk -> hk
																	.getHakukohdeOid())
															.collect(
																	Collectors
																			.toList())),
											(hakuJaHakukohteet,
													laskennanAloitus) -> {
												laskennanAloitus
														.accept(laskenta
																.getUuid());
											}, Boolean.TRUE.equals(laskenta
													.isErillishaku()),
											LaskentaTyyppi.VALINTARYHMA
													.equals(laskenta
															.getTyyppi()),
											laskenta.getValinnanvaihe(),
											laskenta.getValintakoelaskenta(),
											asyncResponse);
								} catch (Throwable e) {
									LOG.error(
											"Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}",
											e.getMessage());
									asyncResponse
											.resume(Response
													.serverError()
													.entity("Odottamaton virhe laskennan kaynnistamisessa! "
															+ e.getMessage())
													.build());
									throw e;
								}
							},
							t -> {
								LOG.error(
										"Uudelleen ajo laskennalle heitti poikkeuksen {}:\r\n{}",
										t.getMessage(),
										Arrays.toString(t.getStackTrace()));
								asyncResponse
										.resume(Response
												.serverError()
												.entity("Uudelleen ajo laskennalle heitti poikkeuksen!")
												.build());
							});
		} catch (Throwable e) {
			LOG.error(
					"Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}",
					e.getMessage());
			asyncResponse.resume(Response
					.serverError()
					.entity("Odottamaton virhe laskennan kaynnistamisessa! "
							+ e.getMessage()).build());
			throw e;
		}
	}

	private void kaynnistaLaskenta(
			LaskentaTyyppi tyyppi,
			String hakuOid,
			Maski maski,
			BiConsumer<Collection<HakukohdeJaOrganisaatio>, Consumer<String>> seurantaTunnus,
			boolean erillishaku, boolean valintaryhmalaskenta,
			Integer valinnanvaihe, Boolean valintakoelaskenta,
			AsyncResponse asyncResponse) {
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
				asyncResponse.resume(Response.ok(Vastaus.uudelleenOhjaus(uuid))
						.build());
				return;
			}
		}
		LOG.info("Aloitetaan laskenta haulle {}", hakuOid);
		haunHakukohteet(
				hakuOid,
				haunHakukohteetOids -> {
					Collection<HakukohdeJaOrganisaatio> oids;
					if (maski.isMask()) {
						oids = maski.maskaa(haunHakukohteetOids);
						if (oids.isEmpty()) {
							throw new RuntimeException(
									"Hakukohdemaskauksen jalkeen haulla ei ole hakukohteita! Ei voida aloittaa laskentaa hakukohteettomasti.");
						}
					} else {
						oids = haunHakukohteetOids;
					}
					final Collection<HakukohdeJaOrganisaatio> finalOids = oids;
					ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid, parametrit -> {
								seurantaTunnus.accept(
										finalOids,
										uuid -> {
											valintalaskentaRoute
													.suoritaValintalaskentaKerralla(
															parametrit,
															new LaskentaAloitus(
															uuid, hakuOid, erillishaku,
															maski.isMask(),
															valintaryhmalaskenta,
															valinnanvaihe,
															valintakoelaskenta, finalOids,
															tyyppi));
											asyncResponse.resume(Response.ok(
													Vastaus.uudelleenOhjaus(uuid)).build());
										});
					},
							poikkeus -> {
								LOG.error("Ohjausparametrien luku epäonnistui: {} {}", poikkeus.getMessage(),
										Arrays.toString(poikkeus.getStackTrace()));
								asyncResponse.resume(Response.serverError()
										.entity(poikkeus.getMessage()).build());
							});
				}, poikkeus -> {
					asyncResponse.resume(Response.serverError()
							.entity(poikkeus.getMessage()).build());
				});
	}

	private void haunHakukohteet(String hakuOid,
			Consumer<List<HakukohdeJaOrganisaatio>> callback,
			Consumer<Throwable> failureCallback) {
		if (StringUtils.isBlank(hakuOid)) {
			LOG.error("Yritettiin hakea hakukohteita ilman hakuOidia!");
			throw new RuntimeException(
					"Yritettiin hakea hakukohteita ilman hakuOidia!");
		}
		valintaperusteetAsyncResource
				.haunHakukohteet(
						hakuOid,
						hakukohdeViitteet -> {
							LOG.info(
									"Tarkastellaan hakukohdeviitteita haulle {}",
									hakuOid);
							if (hakukohdeViitteet == null
									|| hakukohdeViitteet.isEmpty()) {
								LOG.error(
										"Valintaperusteet palautti tyhjat hakukohdeviitteet haulle {}!",
										hakuOid);
								throw new NullPointerException(
										"Valintaperusteet palautti tyhjat hakukohdeviitteet!");
							}
							List<HakukohdeJaOrganisaatio> haunHakukohdeOidit = hakukohdeViitteet
									.stream()
									.filter(Objects::nonNull)
									.filter(h -> {
										if (h == null) {
											LOG.error("nonNull filteri ei toimi!");
											return false;
										}
										if (h.getOid() == null) {
											LOG.error(
													"Hakukohdeviitteen oid oli null haussa {}",
													hakuOid);
											return false;
										}
										if (h.getTila() == null) {
											LOG.error(
													"Hakukohdeviitteen tila oli null hakukohteelle {}",
													h.getOid());
											return false;
										}
										boolean julkaistu = "JULKAISTU"
												.equals(h.getTila());
										if (!julkaistu) {
											LOG.warn(
													"Ohitetaan hakukohde {} koska sen tila on {}.",
													h.getOid(), h.getTila());
										}
										return julkaistu;
									})
									.map(u -> new HakukohdeJaOrganisaatio(u
											.getOid(), u.getTarjoajaOid()))
									.collect(Collectors.toList());
							if (haunHakukohdeOidit.isEmpty()) {
								LOG.error(
										"Haulla {} ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?",
										hakuOid);
								failureCallback
										.accept(new RuntimeException(
												"Haulla "
														+ hakuOid
														+ " ei saatu hakukohteita! Onko valinnat synkronoitu tarjonnan kanssa?"));
							} else {
								callback.accept(haunHakukohdeOidit);
							}
						}, poikkeus -> {
							failureCallback.accept(poikkeus);
						});
	}

	//
	//
	//
	//
	//
	//
	//
	//
	//
	//
	//
	//
	@GET
	@Path("/status")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
	public List<Laskenta> status() {
		return valintalaskentaValvomo.ajossaOlevatLaskennat();
	}

	@GET
	@Path("/status/{uuid}")
	@Produces(APPLICATION_JSON)
	@ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
	public Laskenta status(@PathParam("uuid") String uuid) {
		try {
			return valintalaskentaValvomo.haeLaskenta(uuid);
		} catch (Exception e) {
			LOG.error("Valintalaskennan statuksen luku heitti poikkeuksen! {}",
					e.getMessage());
			return null;
		}
	}

	private Response excelResponse(byte[] bytes, String tiedostonnimi) {
		return Response
				.ok()
				.entity(bytes)
				//
				.header("Content-Length", bytes.length)
				//
				.header("Content-Type", "application/vnd.ms-excel")
				//
				.header("Content-Disposition",
						"attachment; filename=\"" + tiedostonnimi + "\"")
				.build();
	}

	@GET
	@Path("/status/{uuid}/xls")
	@Produces("application/vnd.ms-excel")
	@ApiOperation(value = "Valintalaskennan tila", response = LaskentaAloitus.class)
	public void statusXls(final @PathParam("uuid") String uuid,
			@Suspended AsyncResponse asyncResponse) {
		asyncResponse.setTimeout(15L, TimeUnit.MINUTES);
		asyncResponse.setTimeoutHandler(new TimeoutHandler() {
			public void handleTimeout(AsyncResponse asyncResponse) {
				Map<String, Object[][]> sheetAndGrid = Maps.newHashMap();
				List<Object[]> grid = Lists.newArrayList();
				grid.add(new Object[] { "Kysely seuranapalveluun (kohteelle /laksenta/"
						+ uuid
						+ ") aikakatkaistiin. Palvelu saattaa olla ylikuormittunut!" });
				sheetAndGrid.put("Aikakatkaistu",
						grid.toArray(new Object[][] {}));
				byte[] bytes = ExcelExportUtil
						.exportGridSheetsAsXlsBytes(sheetAndGrid);
				asyncResponse.resume(excelResponse(bytes,
						"yhteenveto_aikakatkaistu.xls"));
				LOG.error(
						"Aikakatkaisu Excelin luonnille (kohde /laskenta/{})",
						uuid);
			}
		});
		seurantaAsyncResource
				.laskenta(
						uuid,
						laskenta -> {
							try {
								byte[] bytes = LaskentaDtoAsExcel
										.laskentaDtoAsExcel(laskenta);
								asyncResponse.resume(excelResponse(bytes,
										"yhteenveto.xls"));
							} catch (Throwable e) {
								LOG.error(
										"Excelin muodostuksessa(kohteelle /laskenta/{}) tapahtui virhe: {}",
										uuid, e.getMessage());
								Map<String, Object[][]> sheetAndGrid = Maps
										.newHashMap();
								List<Object[]> grid = Lists.newArrayList();
								grid.add(new Object[] { "Virhe Excelin muodostuksessa!" });
								grid.add(new Object[] { e.getMessage() });
								for (StackTraceElement se : e.getStackTrace()) {
									grid.add(new Object[] { se });
								}
								sheetAndGrid.put("Virhe",
										grid.toArray(new Object[][] {}));
								byte[] bytes = ExcelExportUtil
										.exportGridSheetsAsXlsBytes(sheetAndGrid);
								asyncResponse.resume(excelResponse(bytes,
										"yhteenveto_virhe.xls"));
								throw e;
							}
						},
						poikkeus -> {
							LOG.error(
									"Excelin tietojen haussa seurantapalvelusta(/laskenta/{}) tapahtui virhe: {}",
									uuid, poikkeus.getMessage());
							Map<String, Object[][]> sheetAndGrid = Maps
									.newHashMap();
							List<Object[]> grid = Lists.newArrayList();
							grid.add(new Object[] { "Virhe seurantapavelun kutsumisessa!" });
							grid.add(new Object[] { poikkeus.getMessage() });
							for (StackTraceElement se : poikkeus
									.getStackTrace()) {
								grid.add(new Object[] { se });
							}
							sheetAndGrid.put("Virhe",
									grid.toArray(new Object[][] {}));
							byte[] bytes = ExcelExportUtil
									.exportGridSheetsAsXlsBytes(sheetAndGrid);
							asyncResponse.resume(excelResponse(bytes,
									"yhteenveto_seurantavirhe.xls"));
						});
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
			l.lopeta();// getLopetusehto().set(true); // aktivoidaan
						// lopetuskasky
			seurantaAsyncResource.merkkaaLaskennanTila(uuid,
					LaskentaTila.PERUUTETTU);
		}
		return Response.ok().build();
	}

}
