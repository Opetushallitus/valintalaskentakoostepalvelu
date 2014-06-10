package fi.vm.sade.valinta.kooste.sijoitteluntulos.route.impl;

import static fi.vm.sade.valinta.kooste.security.SecurityPreprocessor.SECURITY;
import static org.apache.camel.LoggingLevel.ERROR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeNimiRDTO;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;
import fi.vm.sade.valinta.kooste.sijoittelu.exception.SijoittelultaEiSisaltoaPoikkeus;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.TilaResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.Tiedosto;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.Valmis;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosHyvaksymiskirjeetRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosOsoitetarratRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohteetTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateDetail;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.TemplateHistory;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.predicate.SijoittelussaHyvaksyttyHakijaPredicate;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class SijoittelunTulosRouteImpl extends AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(SijoittelunTulosRouteImpl.class);

	private final long getTimeToLive() {
		return DateTime.now().plusDays(7).toDate().getTime();
	}

	private final boolean pakkaaTiedostotTarriin;
	private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
	private final SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy;
	private final HaeHakukohdeNimiTarjonnaltaKomponentti nimiTarjonnalta;
	private final HaeHakukohteetTarjonnaltaKomponentti hakukohteetTarjonnalta;
	private final SijoittelunTulosExcelKomponentti sijoittelunTulosExcel;
	private final TilaResource tilaResource;
	private final DokumenttiResource dokumenttiResource;
	private final ViestintapalveluResource viestintapalveluResource;
	private final HaeOsoiteKomponentti osoiteKomponentti;
	private final ApplicationResource applicationResource;
	private final String hakukohteidenHaku;
	private final String luontiEpaonnistui;
	private final String taulukkolaskenta;
	private final String hyvaksymiskirjeet;
	private final String osoitetarrat;
	private final String dokumenttipalveluUrl;
	private final String muodostaDokumentit;

	@Autowired
	public SijoittelunTulosRouteImpl(
			@Value("${valintalaskentakoostepalvelu.sijoittelunTulosRouteImpl.pakkaaTiedostotTarriin:true}") boolean pakkaaTiedostotTarriin,
			@Value("${valintalaskentakoostepalvelu.dokumenttipalvelu.rest.url}/dokumentit/lataa/") String dokumenttipalveluUrl,
			@Value(SijoittelunTulosTaulukkolaskentaRoute.SEDA_SIJOITTELUNTULOS_TAULUKKOLASKENTA_HAULLE) String taulukkolaskenta,
			@Value(SijoittelunTulosHyvaksymiskirjeetRoute.SEDA_SIJOITTELUNTULOS_HYVAKSYMISKIRJEET_HAULLE) String hyvaksymiskirjeet,
			@Value(SijoittelunTulosOsoitetarratRoute.SEDA_SIJOITTELUNTULOS_OSOITETARRAT_HAULLE) String osoitetarrat,
			HaeHakukohteetTarjonnaltaKomponentti hakukohteetTarjonnalta,
			SijoittelunTulosExcelKomponentti sijoittelunTulosExcel,
			HaeHakukohdeNimiTarjonnaltaKomponentti nimiTarjonnalta,
			HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
			SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy,
			ViestintapalveluResource viestintapalveluResource,
			HaeOsoiteKomponentti osoiteKomponentti,
			ApplicationResource applicationResource, TilaResource tilaResource,
			DokumenttiResource dokumenttiResource) {
		this.tilaResource = tilaResource;
		this.pakkaaTiedostotTarriin = pakkaaTiedostotTarriin;
		this.applicationResource = applicationResource;
		this.osoiteKomponentti = osoiteKomponentti;
		this.osoitetarrat = osoitetarrat;
		this.viestintapalveluResource = viestintapalveluResource;
		this.sijoitteluProxy = sijoitteluProxy;
		this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
		this.nimiTarjonnalta = nimiTarjonnalta;
		this.dokumenttipalveluUrl = dokumenttipalveluUrl;
		this.muodostaDokumentit = "direct:sijoitteluntulos_muodosta_dokumentit";
		this.hakukohteidenHaku = "direct:sijoitteluntulos_hakukohteiden_haku";
		this.luontiEpaonnistui = "direct:sijoitteluntulos_koko_haulle_deadletterchannel";
		this.hakukohteetTarjonnalta = hakukohteetTarjonnalta;
		this.sijoittelunTulosExcel = sijoittelunTulosExcel;
		this.dokumenttiResource = dokumenttiResource;
		this.taulukkolaskenta = taulukkolaskenta;
		this.hyvaksymiskirjeet = hyvaksymiskirjeet;

	}

	public void configure() throws Exception {
		configureMuodostaDokumentit();
		configureDeadLetterChannel();
		configureHakukohteidenHaku();
		configureTaulukkolaskenta();
		configureHyvaksymiskirjeet();
		configureOsoitetarrat();
	}

	private void configureTaulukkolaskenta() {
		String yksittainenTaulukkoTyo = "seda:sijoitteluntulos_taulukkolaskenta_haulle_yksittainentulos?"
				+
				// jos palvelin sammuu niin ei suorita loppuun tyojonoa
				"purgeWhenStopping=true" +
				// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
				"&waitForTaskToComplete=Never" +
				// tyojonossa on yksi tyostaja
				"&concurrentConsumers=6";
		from(taulukkolaskenta)
				//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.log(ERROR,
						"Aloitetaan taulukkolaskentojen muodostus koko haulle!")

				.process(SECURITY)
				//
				.to(hakukohteidenHaku)
				//
				.split(body())
				//
				.stopOnException()
				//
				.shareUnitOfWork()
				//
				.to(yksittainenTaulukkoTyo)
				//
				.end();
		from(yksittainenTaulukkoTyo)
		//
				.routeId(
						"Sijoitteluntulokset koko haulle taulukkolaskentatyöjono")
				//
				.process(SECURITY)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						SijoittelunTulosProsessi prosessi = prosessi(exchange);
						HakukohdeTyyppi hakukohde = exchange.getIn().getBody(
								HakukohdeTyyppi.class);
						String hakukohdeOid = hakukohde.getOid();
						String hakuOid = hakuOid(exchange);
						String sijoitteluajoId = sijoitteluajoId(exchange);
						String tarjoajaOid = StringUtils.EMPTY;
						String hakukohdeNimi;
						String tarjoajaNimi;
						String preferoitukielikoodi = KieliUtil.SUOMI;
						try {
							HakukohdeNimiRDTO nimi = nimiTarjonnalta
									.haeHakukohdeNimi(hakukohdeOid);
							tarjoajaOid = nimi.getTarjoajaOid();
							Teksti hakukohdeTeksti = new Teksti(nimi
									.getHakukohdeNimi());
							preferoitukielikoodi = hakukohdeTeksti.getKieli();
							hakukohdeNimi = hakukohdeTeksti.getTeksti();
							tarjoajaNimi = new Teksti(nimi.getTarjoajaNimi())
									.getTeksti();

						} catch (Exception e) {
							hakukohdeNimi = new StringBuilder()
									.append("Nimetön hakukohde ")
									.append(hakukohdeOid).toString();
							tarjoajaNimi = new StringBuilder()
									.append("Nimetön tarjoaja ")
									.append(tarjoajaOid).toString();
							prosessi.getVaroitukset()
									.add(new Varoitus(hakukohdeOid,
											"Hakukohteelle ei saatu tarjoajaOidia!"));
						}
						List<Valintatulos> tilat = Collections.emptyList();
						try {
							tilat = tilaResource.hakukohteelle(hakukohdeOid);
						} catch (Exception e) {

						}
						try {
							if (pakkaaTiedostotTarriin) {
								Tiedosto tiedosto = new Tiedosto(
										"sijoitteluntulos_" + hakukohdeOid
												+ ".xls",
										IOUtils.toByteArray(sijoittelunTulosExcel
												.luoXls(tilat, sijoitteluajoId,
														preferoitukielikoodi,
														hakukohdeNimi,
														tarjoajaNimi,
														hakukohdeOid, hakuOid)));
								prosessi.getValmiit().add(
										new Valmis(tiedosto, hakukohdeOid,
												tarjoajaOid));
								return;
							} else {
								InputStream input = sijoittelunTulosExcel
										.luoXls(tilat, sijoitteluajoId,
												preferoitukielikoodi,
												hakukohdeNimi, tarjoajaNimi,
												hakukohdeOid, hakuOid);
								try {

									String id = generateId();
									dokumenttiResource.tallenna(id,
											"sijoitteluntulos_" + hakukohdeOid
													+ ".xls", getTimeToLive(),
											dokumenttiprosessi(exchange)
													.getTags(),
											"application/vnd.ms-excel", input);
									prosessi.getValmiit().add(
											new Valmis(hakukohdeOid,
													tarjoajaOid, id));
								} catch (Exception e) {
									LOG.error(
											"Dokumentin tallennus epäonnistui hakukohteelle {}: {}",
											hakukohdeOid, e.getMessage());
									prosessi.getVaroitukset()
											.add(new Varoitus(
													hakukohdeOid,
													"Ei saatu tallennettua dokumenttikantaan! "
															+ e.getMessage()
															+ "\r\n"
															+ Arrays.toString(e
																	.getStackTrace())));
									prosessi.getValmiit().add(
											new Valmis(hakukohdeOid,
													tarjoajaOid, null));
									return;
								}
							}
						} catch (SijoittelultaEiSisaltoaPoikkeus e) {
							prosessi.getValmiit().add(
									new Valmis(hakukohdeOid, tarjoajaOid, null,
											true));
						} catch (Exception e) {
							LOG.error(
									"Sijoitteluntulosexcelin luonti epäonnistui hakukohteelle {}: {}",
									hakukohdeOid, e.getMessage());
							prosessi.getVaroitukset().add(
									new Varoitus(hakukohdeOid,
											"Ei saatu sijoittelun tuloksia tai hakukohteita! "
													+ e.getMessage()
													+ "\r\n"
													+ Arrays.toString(e
															.getStackTrace())));
							prosessi.getValmiit()
									.add(new Valmis(hakukohdeOid, tarjoajaOid,
											null));
						}
					}
				})
				//
				.to(muodostaDokumentit);

	}

	private void configureOsoitetarrat() {
		String yksittainenOsoitetarraTyo = "seda:sijoitteluntulos_osoitetarrat_haulle_yksittainentulos?"
				+
				// jos palvelin sammuu niin ei suorita loppuun tyojonoa
				"purgeWhenStopping=true" +
				// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
				"&waitForTaskToComplete=Never" +
				// tyojonossa on yksi tyostaja
				"&concurrentConsumers=6";
		from(osoitetarrat)
		//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.log(ERROR, "Aloitetaan osoitetarrojen muodostus koko haulle!")

				.process(SECURITY)
				//
				.to(hakukohteidenHaku)
				//
				.split(body())
				//
				.stopOnException()
				//
				.shareUnitOfWork()
				//
				.to(yksittainenOsoitetarraTyo)
				//
				.end();
		from(yksittainenOsoitetarraTyo)
		//
				.routeId("Sijoitteluntulokset koko haulle osoitetarrattyöjono")
				//
				.process(SECURITY)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						SijoittelunTulosProsessi prosessi = prosessi(exchange);
						HakukohdeTyyppi hakukohde = exchange.getIn().getBody(
								HakukohdeTyyppi.class);
						String hakukohdeOid = hakukohde.getOid();
						String tarjoajaOid = StringUtils.EMPTY;
						try {
							HakukohdeNimiRDTO nimi = nimiTarjonnalta
									.haeHakukohdeNimi(hakukohdeOid);
							tarjoajaOid = nimi.getTarjoajaOid();
						} catch (Exception e) {
							prosessi.getVaroitukset()
									.add(new Varoitus(hakukohdeOid,
											"Hakukohteelle ei saatu tarjoajaOidia!"));
						}
						List<String> o;
						try {
							List<HakijaDTO> l = Lists.newArrayList();
							for (HakijaDTO hakija : sijoitteluProxy
									.koulutuspaikalliset(hakuOid(exchange),
											hakukohdeOid,
											SijoitteluResource.LATEST)) {
								l.add(hakija);
							}
							o = FluentIterable
									.from(l)
									.filter(new SijoittelussaHyvaksyttyHakijaPredicate(
											hakukohdeOid))
									.transform(
											new Function<HakijaDTO, String>() {
												@Override
												public String apply(
														HakijaDTO input) {
													return input
															.getHakemusOid();
												}
											}).toList();
							if (o.isEmpty()) {
								prosessi.getValmiit().add(
										new Valmis(hakukohdeOid, tarjoajaOid,
												null, true));
								return;
							}
							List<Hakemus> hakemukset = applicationResource
									.getApplicationsByOids(o);
							List<Osoite> addressLabels = Lists.newArrayList();

							for (Hakemus h : hakemukset) {
								addressLabels.add(osoiteKomponentti
										.haeOsoite(h));
							}
							Osoitteet osoitteet = new Osoitteet(addressLabels);
							if (pakkaaTiedostotTarriin) {
								Tiedosto tiedosto = new Tiedosto(
										"osoitetarrat_" + hakukohdeOid + ".pdf",
										IOUtils.toByteArray(viestintapalveluResource
												.haeOsoitetarratSync(osoitteet)));
								prosessi.getValmiit().add(
										new Valmis(tiedosto, hakukohdeOid,
												tarjoajaOid));
								return;
							} else {
								InputStream input = pipeInputStreams(viestintapalveluResource
										.haeOsoitetarratSync(osoitteet));

								String id = generateId();
								dokumenttiResource.tallenna(id, "osoitetarrat_"
										+ hakukohdeOid + ".pdf",
										getTimeToLive(),
										dokumenttiprosessi(exchange).getTags(),
										"application/pdf", input);
								prosessi.getValmiit().add(
										new Valmis(hakukohdeOid, tarjoajaOid,
												id));
							}
						} catch (Exception e) {
							LOG.error(
									"Sijoitteluntulosexcelin luonti epäonnistui hakukohteelle {}: {}",
									hakukohdeOid, e.getMessage());
							prosessi.getVaroitukset().add(
									new Varoitus(hakukohdeOid,
											"Ei saatu sijoittelun tuloksia tai hakukohteita! "
													+ e.getMessage()
													+ "\r\n"
													+ Arrays.toString(e
															.getStackTrace())));
							prosessi.getValmiit()
									.add(new Valmis(hakukohdeOid, tarjoajaOid,
											null));
						}

					}
				})
				//
				.to(muodostaDokumentit);
	}

	private void configureHyvaksymiskirjeet() {
		String yksittainenHyvaksymiskirjeTyo = "seda:sijoitteluntulos_hyvaksymiskirjeet_haulle_yksittainentulos?"
				+
				// jos palvelin sammuu niin ei suorita loppuun tyojonoa
				"purgeWhenStopping=true" +
				// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
				"&waitForTaskToComplete=Never" +
				// tyojonossa on yksi tyostaja
				"&concurrentConsumers=6";
		from(hyvaksymiskirjeet)
		//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(SECURITY)
				//
				.to(hakukohteidenHaku)
				//
				.split(body())
				//
				.stopOnException()
				//
				.shareUnitOfWork()
				//
				.to(yksittainenHyvaksymiskirjeTyo)
				//
				.end();
		from(yksittainenHyvaksymiskirjeTyo)
		//
				.routeId(
						"Sijoitteluntulokset koko haulle hyväksymiskirjeettyöjono")
				//
				.process(SECURITY)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						SijoittelunTulosProsessi prosessi = prosessi(exchange);
						HakukohdeTyyppi hakukohde = exchange.getIn().getBody(
								HakukohdeTyyppi.class);
						String hakukohdeOid = hakukohde.getOid();
						String hakuOid = hakuOid(exchange);
						String tarjoajaOid = StringUtils.EMPTY;
						String tag = StringUtils.EMPTY;
						try {
							HakukohdeNimiRDTO nimi = nimiTarjonnalta
									.haeHakukohdeNimi(hakukohdeOid);
							tarjoajaOid = nimi.getTarjoajaOid();
							tag = nimi.getHakukohdeNameUri().split("#")[0];
						} catch (Exception e) {
							prosessi.getVaroitukset()
									.add(new Varoitus(hakukohdeOid,
											"Hakukohteelle ei saatu tarjoajaOidia!"));
						}
						try {
							Collection<HakijaDTO> hakukohteenHakijat;

							hakukohteenHakijat = sijoitteluProxy
									.koulutuspaikalliset(hakuOid(exchange),
											hakukohdeOid,
											SijoitteluResource.LATEST);
							hakukohteenHakijat = Collections2.filter(
									hakukohteenHakijat,
									new SijoittelussaHyvaksyttyHakijaPredicate(
											hakukohdeOid));
							if (hakukohteenHakijat.isEmpty()) {
								prosessi.getValmiit().add(
										new Valmis(hakukohdeOid, tarjoajaOid,
												null, true));
								return;
							}
							Teksti hakukohdeNimi = new Teksti(hakukohde
									.getHakukohdeNimi());
							for (TemplateHistory history : viestintapalveluResource
									.haeKirjepohja(tarjoajaOid,
											"hyvaksymiskirje",
											hakukohdeNimi.getKieli(), tag)) {
								if ("default".equals(history.getName())) {
									for (TemplateDetail e : history
											.getTemplateReplacements()) {
										if ("sisalto".equals(e.getName())) {

											LetterBatch l = hyvaksymiskirjeetKomponentti
													.teeHyvaksymiskirjeet(
															hakukohteenHakijat,
															hakukohdeOid,
															hakuOid,
															tarjoajaOid,
															//
															e.getDefaultValue(),
															tag);
											if (pakkaaTiedostotTarriin) {
												Tiedosto tiedosto = new Tiedosto(
														"hyvaksymiskirje_"
																+ hakukohdeOid
																+ ".pdf",
														IOUtils.toByteArray(viestintapalveluResource
																.haeKirjeSync(new Gson()
																		.toJson(l))));
												prosessi.getValmiit().add(
														new Valmis(tiedosto,
																hakukohdeOid,
																tarjoajaOid));
												return;
											} else {
												InputStream input = pipeInputStreams(viestintapalveluResource
														.haeKirjeSync(new Gson()
																.toJson(l)));
												try {

													String id = generateId();
													dokumenttiResource
															.tallenna(
																	id,
																	"hyvaksymiskirje_"
																			+ hakukohdeOid
																			+ ".pdf",
																	getTimeToLive(),
																	dokumenttiprosessi(
																			exchange)
																			.getTags(),
																	"application/pdf",
																	input);
													prosessi.getValmiit()
															.add(new Valmis(
																	hakukohdeOid,
																	tarjoajaOid,
																	id));
												} catch (Exception ex) {
													LOG.error(
															"Dokumentin tallennus epäonnistui hakukohteelle {}: {}",
															hakukohdeOid,
															ex.getMessage());
													prosessi.getVaroitukset()
															.add(new Varoitus(
																	hakukohdeOid,
																	"Ei saatu tallennettua dokumenttikantaan! "
																			+ ex.getMessage()
																			+ "\r\n"
																			+ Arrays.toString(ex
																					.getStackTrace())));
													prosessi.getValmiit()
															.add(new Valmis(
																	hakukohdeOid,
																	tarjoajaOid,
																	null));
													return;
												}
											}
											return;
										}
									}
								}
							}
						} catch (Exception e) {
							LOG.error(
									"Sijoitteluntulosexcelin luonti epäonnistui hakukohteelle {}: {}",
									hakukohdeOid, e.getMessage());
							prosessi.getVaroitukset().add(
									new Varoitus(hakukohdeOid,
											"Ei saatu sijoittelun tuloksia tai hakukohteita! "
													+ e.getMessage()
													+ "\r\n"
													+ Arrays.toString(e
															.getStackTrace())));
							prosessi.getValmiit()
									.add(new Valmis(hakukohdeOid, tarjoajaOid,
											null));
						}

					}
				})
				//
				.to(muodostaDokumentit);

	}

	private void configureDeadLetterChannel() {
		from(luontiEpaonnistui)
		//
				.log(ERROR,
						"Sijoitteluntulosten taulukkolaskentaluonti epaonnistui: ${property.CamelExceptionCaught}")
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						if (dokumenttiprosessi(exchange).getPoikkeukset()
								.isEmpty()) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.KOOSTEPALVELU,
											"Sijoitteluntulosten vienti epäonnistui!"));
						}

					}
				})
				//
				.stop();
	}

	private void configureMuodostaDokumentit() {
		from(muodostaDokumentit)
		//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						SijoittelunTulosProsessi prosessi = prosessi(exchange);

						if (prosessi.inkrementoi() == 0) {

							try {
								InputStream tar = generoiYhteenvetoTar(prosessi
										.getValmiit());

								String id = generateId();
								dokumenttiResource.tallenna(id,
										"sijoitteluntuloksethaulle.tar",
										defaultExpirationDate().getTime(),
										dokumenttiprosessi(exchange).getTags(),
										"application/x-tar", tar);

								prosessi.setDokumenttiId(id);
							} catch (Exception e) {
								LOG.error(
										"Tulostietojen tallennus dokumenttipalveluun epäonnistui! {}:\r\n{}",
										e.getMessage(),
										Arrays.asList(e.getStackTrace()));
								prosessi.getPoikkeukset()
										.add(new Poikkeus(
												Poikkeus.DOKUMENTTIPALVELU,
												"Tulostietojen tallennus epäonnistui!"));

							}
						}
					}

				});
	}

	private void configureHakukohteidenHaku() {
		from(hakukohteidenHaku)
		//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						String hakuOid = hakuOid(exchange);
						try {
							dokumenttiprosessi(exchange)
									.getVaroitukset()
									.add(new Varoitus(
											hakuOid,
											"Haetaan tarjonnalta kaikki hakukohteet! Varoitus, pyyntö saattaa kestää pitkään!"));
							Collection<HakukohdeTyyppi> hakukohteet = hakukohteetTarjonnalta
									.haeHakukohteetTarjonnalta(hakuOid);
							if (hakukohteet == null || hakukohteet.isEmpty()) {
								throw kasittelePoikkeus(
										Poikkeus.TARJONTA,
										exchange,
										new RuntimeException(
												"Tarjonnalta ei saatu hakukohteita haulle"),
										Poikkeus.hakuOid(hakuOid));
							}
							exchange.getOut().setBody(hakukohteet);
							dokumenttiprosessi(exchange).setKokonaistyo(
									hakukohteet.size());
						} catch (Exception e) {
							LOG.error(
									"Hakukohteiden haku epäonnistui! {}\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));
							throw kasittelePoikkeus(Poikkeus.TARJONTA,
									exchange, e);
						}
					}
				});
	}

	protected SijoittelunTulosProsessi prosessi(Exchange exchange) {
		return exchange.getProperty(
				ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI,
				SijoittelunTulosProsessi.class);
	}

	private void writeLinesToTarFile(String fileName, byte[] data,
			TarArchiveOutputStream tarOutputStream) throws IOException {
		TarArchiveEntry archiveEntry = new TarArchiveEntry(fileName);
		archiveEntry.setSize(data.length);
		tarOutputStream.putArchiveEntry(archiveEntry);
		tarOutputStream.write(data);
		tarOutputStream.closeArchiveEntry();
	}

	private void writeLinesToTarFile(String fileName, Collection<String> lines,
			TarArchiveOutputStream tarOutputStream) throws IOException {
		TarArchiveEntry archiveEntry = new TarArchiveEntry(fileName);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		IOUtils.writeLines(lines, "\r\n", output);
		byte[] data = output.toByteArray();
		archiveEntry.setSize(data.length);
		tarOutputStream.putArchiveEntry(archiveEntry);
		tarOutputStream.write(data);
		tarOutputStream.closeArchiveEntry();
	}

	private InputStream generoiYhteenvetoTar(final Collection<Valmis> valmiit)
			throws IOException {
		int yhteensa = valmiit.size();

		Map<String, Collection<Valmis>> onnistuneetPerTarjoaja = Maps
				.newHashMap();
		Collection<Valmis> epaonnistuneet = Lists.newArrayList();
		int onnistuneita = 0;
		synchronized (valmiit) {
			for (Valmis v : valmiit) {
				if (v.isOnnistunut()) {
					++onnistuneita;
				} else {
					epaonnistuneet.add(v);
				}
				if (onnistuneetPerTarjoaja.containsKey(v.getTarjoajaOid())) {
					onnistuneetPerTarjoaja.get(v.getTarjoajaOid()).add(v);
				} else {
					onnistuneetPerTarjoaja.put(v.getTarjoajaOid(),
							Lists.newArrayList(v));
				}
			}
		}
		LOG.error(
				"Sijoitteluntulosexcel valmistui! {} työtä! Joista onnistuneita {} ja epäonnistuneita {}",
				yhteensa, onnistuneita, yhteensa - onnistuneita);
		ByteArrayOutputStream tarFileBytes = new ByteArrayOutputStream();
		TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(
				tarFileBytes);
		{
			Collection<String> rivit = Lists.newArrayList();
			rivit.add(new StringBuilder().append("Yhteensä ").append(yhteensa)
					.append(", joista onnistuneita ").append(onnistuneita)
					.append(" ja epäonnistuneita ")
					.append(yhteensa - onnistuneita).toString());

			for (Valmis epa : epaonnistuneet) {
				String otsikko;
				if (epa.isEiTuloksia()) {
					otsikko = "Tulokseton hakukohde ";
				} else {
					otsikko = "Epäonnistunut hakukohde ";
				}
				rivit.add(new StringBuilder().append(otsikko)
						.append(epa.getHakukohdeOid()).toString());
				rivit.add(new StringBuilder().append("-- Tarjoaja ")
						.append(epa.getTarjoajaOid()).toString());
			}
			writeLinesToTarFile("yhteenveto.txt", rivit, tarOutputStream);
		}
		for (Entry<String, Collection<Valmis>> perTarjoaja : onnistuneetPerTarjoaja
				.entrySet()) {
			if (perTarjoaja == null) {
				LOG.error("perTarjoaja null! Miten mahdollista!");
				continue;
			}
			String tarjoajaOid = StringUtils.trimToEmpty(perTarjoaja.getKey());
			String subFileName = new StringBuilder().append("tarjoajaOid_")
					.append(tarjoajaOid.replace(" ", "_")).append(".tar")
					.toString();

			ByteArrayOutputStream subTarFileBytes = new ByteArrayOutputStream();
			TarArchiveOutputStream subTarOutputStream = new TarArchiveOutputStream(
					subTarFileBytes);
			if (perTarjoaja.getValue() == null) {
				LOG.error("perTarjoaja value null! Miten mahdollista!");
				continue;
			}
			for (Valmis v : perTarjoaja.getValue()) {
				if (v.containsTiedosto()) {
					writeLinesToTarFile(v.getTiedosto().getTiedostonNimi(), v
							.getTiedosto().getData(), subTarOutputStream);
				} else if (v.isOnnistunut()) {
					String hakukohdeFileName = new StringBuilder()
							.append("hakukohdeOid_")
							.append(v.getHakukohdeOid()).append(".txt")
							.toString();
					String kokoUrl = new StringBuilder()
							.append(dokumenttipalveluUrl)
							.append(v.getTulosId()).toString();
					writeLinesToTarFile(hakukohdeFileName,
							Arrays.asList(v.getTulosId(), kokoUrl),
							subTarOutputStream);
				}

			}
			subTarOutputStream.close();
			writeLinesToTarFile(subFileName, subTarFileBytes.toByteArray(),
					tarOutputStream);
		}

		tarOutputStream.close();
		return new ByteArrayInputStream(tarFileBytes.toByteArray());
	}
}
