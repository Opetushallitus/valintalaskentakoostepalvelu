package fi.vm.sade.valinta.kooste.valintakokeet.route.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHaunHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HakemusOidSplitter;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintakokeet.dto.ValintakoeCache;
import fi.vm.sade.valinta.kooste.valintakokeet.dto.ValintakoeProsessi;
import fi.vm.sade.valinta.kooste.valintakokeet.dto.ValintakoeTyo;
import fi.vm.sade.valinta.kooste.valintakokeet.route.ValintakoelaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.AbstraktiTyo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintaperusteetTyo;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class ValintakoelaskentaMuistissaRouteImpl extends
		AbstractDokumenttiRouteBuilder {
	private final static int UUDELLEEN_YRITYSTEN_MAARA = 10;
	private final static long UUDELLEEN_YRITYSTEN_ODOTUSAIKA = 5000L;

	private final static Logger LOG = LoggerFactory
			.getLogger(ValintakoelaskentaMuistissaRouteImpl.class);
	private final String valintakoelaskenta;
	private final String hakemusTyojono;
	private final String valintaperusteetTyojono;
	private final String valintakoelaskentaTyojono;
	private final HaeHaunHakemuksetKomponentti haeHaunHakemuksetKomponentti;
	private final HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti;
	private final ApplicationResource applicationResource;
	private final ValintaperusteService valintaperusteService;
	private final ValintalaskentaService valintalaskentaService;

	@Autowired
	public ValintakoelaskentaMuistissaRouteImpl(
			@Value("seda:valintakoelaskenta_hakemusTyojono?" +
			//
					"purgeWhenStopping=true&waitForTaskToComplete=Never&" +
					// Hakemustyojonon kasittelijoiden maara. Oletuksena 1
					"concurrentConsumers=${valintalaskentakoostepalvelu.valintakoelaskenta.hakemus.threadpoolsize:4}") String hakemusTyojono,
			@Value("seda:valintakoelaskenta_valintaperusteetTyojono?" +
			//
					"purgeWhenStopping=true&waitForTaskToComplete=Never&" +
					// Valintaperusteettyojonon kasittelijoiden maara.
					// Oletuksena 1
					"concurrentConsumers=${valintalaskentakoostepalvelu.valintakoelaskenta.valintaperusteet.threadpoolsize:4}") String valintaperusteetTyojono,
			@Value("seda:valintakoelaskenta_valintakoelaskentaTyojono?" +
			//
					"purgeWhenStopping=true&waitForTaskToComplete=Never&" +
					// Valintakoelaskentatyojonon kasittelijoiden maara.
					// Oletuksena 1
					"concurrentConsumers=${valintalaskentakoostepalvelu.valintakoelaskenta.threadpoolsize:5}") String valintakoelaskentaTyojono,
			@Value(ValintakoelaskentaMuistissaRoute.SEDA_VALINTAKOELASKENTA_MUISTISSA) String valintakoelaskenta,
			HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti,
			ValintaperusteService valintaperusteService,
			ValintalaskentaService valintalaskentaService,
			ApplicationResource applicationResource,
			HaeHaunHakemuksetKomponentti haeHaunHakemuksetKomponentti) {
		this.haeHakukohteenHakemuksetKomponentti = haeHakukohteenHakemuksetKomponentti;
		this.valintaperusteService = valintaperusteService;
		this.valintalaskentaService = valintalaskentaService;
		this.hakemusTyojono = hakemusTyojono;
		this.valintaperusteetTyojono = valintaperusteetTyojono;
		this.valintakoelaskentaTyojono = valintakoelaskentaTyojono;
		this.valintakoelaskenta = valintakoelaskenta;
		this.applicationResource = applicationResource;
		this.haeHaunHakemuksetKomponentti = haeHaunHakemuksetKomponentti;
	}

	protected ValintakoeProsessi prosessi(Exchange exchange) {
		return exchange.getProperty(
				ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI,
				ValintakoeProsessi.class);
	}

	@Override
	public void configure() throws Exception {
		//
		// Valintakoelaskenta muistissa reitti
		//
		from(valintakoelaskenta)
		//
				.process(SecurityPreprocessor.SECURITY)
				//
				.doTry()
				//
				// Collection<String> hakemusOids
				//
				.log(LoggingLevel.INFO,
						"Haetaan valintakoelaskentaa varten hakemukset")
				//
				.to("direct:valintakoelaskentamuistissa_haun_hakemukset")
				//
				.doCatch(Exception.class)
				//
				.process(
						kirjaaPoikkeus(new Poikkeus(Poikkeus.TARJONTA,
								"Tarjonnasta ei saatu haettua hakemuksia haulle")))
				//
				.log(LoggingLevel.ERROR,
						"Tarjonnasta ei saatu hakemuksia haulle!")
				//
				.stop()
				//
				.end()
				//
				.choice()
				//
				.when(isEmpty(body()))
				// .
				.log(LoggingLevel.ERROR,
						"Valintakoelaskentaa ei voida suorittaa haulle jossa ei ole hakemuksia!")
				//
				.stop()
				//
				.otherwise()
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						Collection<?> hakemukset = exchange.getIn().getBody(
								Collection.class);
						prosessi(exchange).getHakemukset().setKokonaismaara(
								hakemukset.size());
					}
				})
				//
				.split(body())
				//
				.to(hakemusTyojono);

		from(hakemusTyojono)
				//
				// when keskeytetty ohita
				.errorHandler(
						deadLetterChannel(
								"direct:valintakoelaskentamuistissa_hakemusTyojono_deadletterchannel")
								//
								.maximumRedeliveries(UUDELLEEN_YRITYSTEN_MAARA)
								//
								.redeliveryDelay(UUDELLEEN_YRITYSTEN_ODOTUSAIKA)
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))

				.choice()
				//
				.when(prosessiOnKeskeytetty())
				//
				.stop()
				//
				.otherwise()
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String hakemusOid = exchange.getIn().getBody(
								String.class);
						try {
							HakemusTyyppi hakemusTyyppi = exchange
									.getContext()
									.getTypeConverter()
									.tryConvertTo(
											HakemusTyyppi.class,
											applicationResource
													.getApplicationByOid(hakemusOid));
							prosessi(exchange).getHakemukset().tyoValmistui(0L);
							// Tallenna haettu hakemustyyppi osaksi
							// valintakoelaskentatyota prosessiin
							Collection<? extends AbstraktiTyo> tyot = cache(
									exchange)
									.hakukohteenEsitiedotOnSelvitettyJaSeuraavaksiEsitiedotTyojonoihin(
											hakemusTyyppi);
							exchange.getOut().setBody(tyot);
						} catch (Exception e) {
							LOG.error("Ei saatu hakemusta({}) {}:\r\n{}",
									hakemusOid, e.getMessage(),
									Arrays.toString(e.getStackTrace()));
							throw e;
						}

					}
				})
				//
				.split(body())
				//
				.choice()
				//
				.when(body().isInstanceOf(ValintaperusteetTyo.class))
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						prosessi(exchange).getValintaperusteet()
								.inkrementoiKokonaismaaraa();
					}
				})
				//
				.to(valintaperusteetTyojono)
				//
				.when(body().isInstanceOf(ValintakoeTyo.class))
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						prosessi(exchange).getValintakoelaskenta()
								.inkrementoiKokonaismaaraa();
					}
				})
				.to(valintakoelaskentaTyojono)
				//
				.otherwise()
				//
				.log(LoggingLevel.ERROR,
						"Tuntematon tyo palautettu valintakoetyon luovasta prosessista")
				//
				.stop()
				//
				.end();

		from(valintaperusteetTyojono)
		//
				.errorHandler(
						deadLetterChannel(
								"direct:valintakoelaskentamuistissa_valintaperusteetTyojono_deadletterchannel")
								//
								.maximumRedeliveries(UUDELLEEN_YRITYSTEN_MAARA)
								//
								.redeliveryDelay(UUDELLEEN_YRITYSTEN_ODOTUSAIKA)
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))

				// when keskeytetty ohita
				.choice()
				//
				.when(prosessiOnKeskeytetty())
				//
				.stop()
				//
				.otherwise()
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						try {
							ValintaperusteetTyo<ValintakoeTyo> valintaperusteetTyo = exchange
									.getIn().getBody(ValintaperusteetTyo.class);

							HakuparametritTyyppi params = new HakuparametritTyyppi();
							params.setHakukohdeOid(valintaperusteetTyo.getOid());
							params.setValinnanVaiheJarjestysluku(/*
																 * Haetaan
																 * kaikki
																 * hakukohteen
																 * valintaperusteet
																 */null);

							List<ValintaperusteetTyyppi> t = valintaperusteService
									.haeValintaperusteet(Arrays.asList(params));
							prosessi(exchange).getValintaperusteet()
									.tyoValmistui(0L);
							if (t == null || t.isEmpty()) {
								LOG.error(
										"Hakukohteelle {} ei saatu valintaperusteita!",
										valintaperusteetTyo.getOid());
								valintaperusteetTyo.setEsitietoOhitettu();
								exchange.getOut().setBody(
										Collections.emptyList());
							} else {
								Collection<ValintakoeTyo> v = valintaperusteetTyo
										.setEsitieto(t);
								if (v != null) {
									prosessi(exchange)
											.getValintakoelaskenta()
											.inkrementoiKokonaismaaraa(v.size());
									exchange.getOut().setBody(v);
								} else {
									exchange.getOut().setBody(
											Collections.emptyList());
								}
							}
						} catch (Exception e) {
							LOG.error(
									"Virhe valintaperusteiden haussa {}:\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));
							throw e;
						}
					}
				})
				//
				.end()
				//
				.split(body())
				//
				.to(valintakoelaskentaTyojono);

		from(valintakoelaskentaTyojono)
		//
				.errorHandler(
						deadLetterChannel(
								"direct:valintakoelaskentamuistissa_valintakoeTyojono_deadletterchannel")
								//
								.maximumRedeliveries(UUDELLEEN_YRITYSTEN_MAARA)
								//
								.redeliveryDelay(UUDELLEEN_YRITYSTEN_ODOTUSAIKA)
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				// when keskeytetty ohita
				.choice()
				//
				.when(prosessiOnKeskeytetty())
				//
				.stop()
				//
				.otherwise()
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						ValintakoeTyo valintakoeTyo = exchange.getIn().getBody(
								ValintakoeTyo.class);
						try {
							List<ValintaperusteetTyyppi> v = valintakoeTyo
									.getValintaperusteet();
							if (!v.isEmpty()) {
								valintalaskentaService.valintakokeet(
										valintakoeTyo.getHakemus(),
										valintakoeTyo.getValintaperusteet());
							} else {
								LOG.error(
										"Hakemuksen {} yhdellekään hakutoiveelle ei ollut valintaperusteita.",
										valintakoeTyo.getHakemus()
												.getHakemusOid());
							}
							prosessi(exchange).getValintakoelaskenta()
									.tyoValmistui(0L);
						} catch (Exception e) {
							LOG.error(
									"Valintakoelaskenta epäonnistui hakemukselle({}) {}:\r\n",
									valintakoeTyo.getHakemus().getHakemusOid(),
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));
							throw e;
						}
					}
				})
				//
				.end();
		//
		// Tarjonnasta Haun Hakemukset vaihtoehtoisesti jos "hakemusOids"
		// property on määritelty niin käytetään sitä
		//
		from("direct:valintakoelaskentamuistissa_haun_hakemukset")
		//
				.errorHandler(
						deadLetterChannel(
								"direct:valintakoelaskentamuistissa_hakemusTyojono_deadletterchannel")
								//
								.maximumRedeliveries(UUDELLEEN_YRITYSTEN_MAARA)
								//
								.redeliveryDelay(UUDELLEEN_YRITYSTEN_ODOTUSAIKA)
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.choice()
				//
				.when(property(OPH.HAKUKOHDEOID).isNull())
				//
				.bean(haeHaunHakemuksetKomponentti)
				//
				.bean(new HakemusOidSplitter())
				//
				.otherwise()
				//
				.bean(haeHakukohteenHakemuksetKomponentti)
				//
				.bean(new HakemusOidSplitter())
				//
				.end();

		//
		// Deadletterchannelit
		//

		from(
				"direct:valintakoelaskentamuistissa_valintaperusteetTyojono_deadletterchannel")
		//
				.process(
						kirjaaPoikkeus(new Poikkeus(Poikkeus.VALINTAPERUSTEET,
								"Valintaperusteiden haku epäonnistui.")));
		from(
				"direct:valintakoelaskentamuistissa_valintakoeTyojono_deadletterchannel")
		//
				.process(
						kirjaaPoikkeus(new Poikkeus(Poikkeus.VALINTALASKENTA,
								"Valintakoelaskenta epäonnistui.")));
		from(
				"direct:valintakoelaskentamuistissa_hakemusTyojono_deadletterchannel")
		//
				.process(
						kirjaaPoikkeus(new Poikkeus(Poikkeus.HAKU,
								"Hakemuksen haku kutsu epäonnistui.")));

	}

	private ValintakoeCache cache(Exchange exchange) {
		return exchange.getProperty("valintakoeCache", ValintakoeCache.class);
	}

}
