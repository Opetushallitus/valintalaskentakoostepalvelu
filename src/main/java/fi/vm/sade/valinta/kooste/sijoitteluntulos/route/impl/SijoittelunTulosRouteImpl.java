package fi.vm.sade.valinta.kooste.sijoitteluntulos.route.impl;

import static fi.vm.sade.valinta.kooste.security.SecurityPreprocessor.SECURITY;
import static org.apache.camel.LoggingLevel.ERROR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.KoulutusKoosteTyyppi;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.dto.SijoittelunTulosProsessi;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosHyvaksymiskirjeetRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohteetTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.komponentti.SijoittelunTulosExcelKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
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

	private final HaeHakukohteetTarjonnaltaKomponentti hakukohteetTarjonnalta;
	private final SijoittelunTulosExcelKomponentti sijoittelunTulosExcel;
	private final DokumenttiResource dokumenttiResource;
	private final Endpoint hakukohteidenHaku;
	private final Endpoint luontiEpaonnistui;
	private final String taulukkolaskenta;
	private final String hyvaksymiskirjeet;

	@Autowired
	public SijoittelunTulosRouteImpl(
			@Value(SijoittelunTulosTaulukkolaskentaRoute.SEDA_SIJOITTELUNTULOS_TAULUKKOLASKENTA_HAULLE) String taulukkolaskenta,
			@Value(SijoittelunTulosHyvaksymiskirjeetRoute.SEDA_SIJOITTELUNTULOS_HYVAKSYMISKIRJEET_HAULLE) String hyvaksymiskirjeet,
			HaeHakukohteetTarjonnaltaKomponentti hakukohteetTarjonnalta,
			SijoittelunTulosExcelKomponentti sijoittelunTulosExcel,
			DokumenttiResource dokumenttiResource) {
		this.hakukohteidenHaku = endpoint("direct:sijoitteluntulos_hakukohteiden_haku");
		this.luontiEpaonnistui = endpoint("direct:sijoitteluntulos_koko_haulle_deadletterchannel");
		this.hakukohteetTarjonnalta = hakukohteetTarjonnalta;
		this.sijoittelunTulosExcel = sijoittelunTulosExcel;
		this.dokumenttiResource = dokumenttiResource;
		this.taulukkolaskenta = taulukkolaskenta;
		this.hyvaksymiskirjeet = hyvaksymiskirjeet;

	}

	public void configure() throws Exception {
		configureDeadLetterChannel();
		configureHakukohteidenHaku();
		configureTaulukkolaskenta();
		configureHyvaksymiskirjeet();
	}

	private void configureTaulukkolaskenta() {
		Endpoint yksittainenTaulukkoTyo = endpoint("seda:sijoitteluntulos_taulukkolaskenta_haulle_yksittainentulos?"
				+
				// jos palvelin sammuu niin ei suorita loppuun tyojonoa
				"purgeWhenStopping=true" +
				// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
				"&waitForTaskToComplete=Never" +
				// tyojonossa on yksi tyostaja
				"&concurrentConsumers=10");
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
						try {
							HakukohdeTyyppi hakukohde = exchange.getIn()
									.getBody(HakukohdeTyyppi.class);
							StringBuilder s = new StringBuilder();
							for (KoulutusKoosteTyyppi k : hakukohde
									.getHakukohdeKoulutukses()) {
								s.append(k.getTarjoaja()).append(" ");

							}

							String tarjoajaOid = s.toString().trim();
							String hakukohdeOid = hakukohde.getOid();
							String hakuOid = hakuOid(exchange);
							String sijoitteluajoId = sijoitteluajoId(exchange);

							if (hakukohdeOid == null || hakuOid == null
									|| sijoitteluajoId == null
									|| prosessi == null) {
								LOG.error("Arvot ei välity oikein sijoitteluntulostyöjonoon. Tarkista hakukohdeOid,hakuOid,sijoitteluajoId ja prosessi!");
								prosessi.getVaroitukset()
										.add(new Varoitus(hakukohdeOid,
												"Arvot ei välity työjonoon oikein!"));
								prosessi.getOhitetutOids().add(hakukohdeOid);
								prosessi.getOhitetutTarjoajaOids().add(
										tarjoajaOid);
								return;
							}
							InputStream xls;
							try {
								xls = sijoittelunTulosExcel.luoXls(
										sijoitteluajoId, hakukohdeOid, hakuOid);
							} catch (Exception e) {
								LOG.error(
										"Sijoitteluntulosexcelin luonti epäonnistui hakukohteelle {}: {}",
										hakukohdeOid, e.getMessage());
								prosessi.getVaroitukset()
										.add(new Varoitus(
												hakukohdeOid,
												"Ei saatu sijoittelun tuloksia tai hakukohteita! "
														+ e.getMessage()
														+ "\r\n"
														+ Arrays.toString(e
																.getStackTrace())));
								prosessi.getOhitetutOids().add(hakukohdeOid);
								prosessi.getOhitetutTarjoajaOids().add(
										tarjoajaOid);
								return;
							}

							try {

								String id = generateId();
								dokumenttiResource.tallenna(id,
										"sijoitteluntulos_" + hakukohdeOid
												+ ".xls",
										defaultExpirationDate().getTime(),
										dokumenttiprosessi(exchange).getTags(),
										"application/vnd.ms-excel", xls);

								prosessi.getTulosIds().add(id);
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
								prosessi.getOhitetutOids().add(hakukohdeOid);
								prosessi.getOhitetutTarjoajaOids().add(
										tarjoajaOid);
								return;
							}
							prosessi.getOnnistuneetOids().add(hakukohdeOid);
							prosessi.getOnnistuneetTarjoajaOids().add(
									tarjoajaOid);
						} finally {
							if (prosessi.inkrementoi() == 0) {
								LOG.error(
										"Sijoitteluntulosexcel valmistui! {} ohitettua työtä, {} oikein valmistunutta!",
										prosessi.getOhitetutOids().size(),
										prosessi.getTulosIds().size());
								try {
									Collection<String> lines = Lists
											.newArrayList();

									for (String tulosIds : prosessi
											.getTulosIds()) {
										lines.add(tulosIds);
									}

									ByteArrayOutputStream b = new ByteArrayOutputStream();
									IOUtils.writeLines(lines, "\r\n", b);

									String id = generateId();
									dokumenttiResource.tallenna(
											id,
											"sijoitteluntulosexcel.txt",
											defaultExpirationDate().getTime(),
											dokumenttiprosessi(exchange)
													.getTags(),
											"text/plain",
											new ByteArrayInputStream(b
													.toByteArray()));

									prosessi.setDokumenttiId(id);
								} catch (Exception e) {
									LOG.error("Tulostietojen tallennus dokumenttipalveluun epäonnistui!");
									prosessi.getPoikkeukset()
											.add(new Poikkeus(
													Poikkeus.DOKUMENTTIPALVELU,
													"Tulostietojen tallennus epäonnistui!"));

								}
							}
						}
					}
				});

	}

	private void configureHyvaksymiskirjeet() {
		Endpoint yksittainenHyvaksymiskirjeTyo = endpoint("seda:sijoitteluntulos_hyvaksymiskirjeet_haulle_yksittainentulos?"
				+
				// jos palvelin sammuu niin ei suorita loppuun tyojonoa
				"purgeWhenStopping=true" +
				// reitin kutsuja ei jaa koskaan odottamaan paluuarvoa
				"&waitForTaskToComplete=Never" +
				// tyojonossa on yksi tyostaja
				"&concurrentConsumers=10");
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

					}
				});
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
				.to(yksittainenHyvaksymiskirjeTyo)
				//
				.end();

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
}
