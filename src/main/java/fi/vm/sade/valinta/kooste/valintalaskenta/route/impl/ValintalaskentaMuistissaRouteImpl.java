package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import static java.lang.Boolean.TRUE;
import static org.apache.camel.LoggingLevel.DEBUG;
import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.LoggingLevel.WARN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache.HakukohdeKey;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Suorittaa valintalaskennan hakemalla kaiken tarvittavan tiedon ensin
 *         muistiin
 * 
 * 
 *         >> Tarjonnalta hakukohteet: List<String>
 * 
 *         >> filtterointi
 * 
 *         >> HakuApp:lta hakemusOidit hakukohteelle: List<String>
 * 
 *         >> ...
 * 
 */
@Component
public class ValintalaskentaMuistissaRouteImpl extends SpringRouteBuilder {
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaMuistissaRouteImpl.class);
	private final String fail;
	private final String start;
	private final String finish;
	private final String deadLetterChannelHaeHakukohteenHakemukset;
	private final String deadLetterChannelHaeHakemus;
	private final String deadLetterChannelHaeValintaperusteet;
	private final String deadLetterChannelTeeValintalaskenta;
	private final String valintalaskentaTyojonoon;
	private final String valintalaskentaCache;
	private final String valvomoKuvaus;
	private final String valvomoProsessi;
	private final String valintalaskentaMuistissa;
	private final String aloitaLaskenta;
	private final String haeHakukohteidenHakemukset;
	private final String haeHakemukset;
	private final String haeValintaperusteet;
	private final String haeHakemus;
	private final String haeHakemusYksittainen;
	private final String haeValintaperuste;
	private final String haeValintaperusteetYksittainen;
	private final SecurityPreprocessor security;
	private final HakuAppHakemus hakuAppHakemus;
	private final HakuAppHakemusOids hakuAppHakemusOids;
	private final TarjonnanHakukohdeOids tarjontaHakukohdeOids;
	private final Valintaperusteet valintaperusteet;
	private final Valintalaskenta valintalaskenta;

	// private final ExecutorService hakuAppExecutorService;
	// private final ExecutorService valintaperusteetExecutorService;

	private void configureValintalaskentaMuistissa() {

		/**
		 * Suoritetaan valintalaskenta muistinvaraisesti
		 */
		from(valintalaskentaMuistissa)
				//
				.process(security)
				//
				.setProperty(valvomoKuvaus,
						constant("Muistinvarainen valintalaskenta haulle"))
				//
				.choice()
				//
				.when(property(valvomoProsessi).isNull())
				//
				.setProperty(valvomoProsessi,
						constant(new ValintalaskentaMuistissaProsessi()))
				//
				.end()
				//
				.to(start)
				// 1. hae tarjonnalta haun hakukohteet
				.log(INFO,
						"Haetaan tarjonnalta haun ${property.hakuOid} hakukohteet")
				//
				.choice()
				//
				.when(simple("${property.valintalaskentaCache.hasTarjonnanHakukohteet()}"))
				//
				.log(WARN,
						"Valintalaskenta tehdään muistiin taltioiduille hakukohteille: ${property.valintalaskentaCache.getTarjonnanHakukohteet().size()} kpl")
				//
				.otherwise()
				//
				.process(tarjonnastaJulkaistutHakukohdeOidit())
				//
				.end()
				// List<String>
				.log(INFO, "Aloitetaan tiedon keräys muistiin")
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						Collection<String> c = cache(exchange)
								.getFilteredHakukohteet();
						exchange.getOut().setBody(c);
						// update work
						prosessi(exchange).getHakukohteilleHakemukset()
								.setKokonaismaara(c.size());
					}
				})
				//
				.split(body(),
						new FlexibleAggregationStrategy().storeInBody()
								.accumulateInCollection(ArrayList.class))
				//
				// .setExecutorService(executorService);
				//
				.stopOnException()
				//
				.shareUnitOfWork()
				//
				.parallelProcessing()
				//
				.to(haeHakukohteidenHakemukset)
				//
				.end()
				//
				.log(INFO, "Päivitetään työmäärät")
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						int tyomaarat = cache(exchange)
								.getFilteredHakukohteet().size();
						prosessi(exchange).getValintalaskenta()
								.setKokonaismaara(tyomaarat);
						prosessi(exchange).getValintaperusteet()
								.setKokonaismaara(tyomaarat);

						prosessi(exchange).getHakemukset().setKokonaismaara(
								cache(exchange)
										.getHakemattomienHakemustenMaara());
					}
				})
				// List<String>
				.multicast()
				//
				// No need for executor service as there's only two works here
				// haeHakemukset and haeValintaperusteet
				// (both works use internally executor services)
				//
				.parallelProcessing()
				//
				.to(haeHakemukset, haeValintaperusteet)
				//
				.end();
		// 3. aloita laskenta datan varaisesti
		// .to(aloitaLaskenta);

	}

	/**
	 * Hakukohteen hakemusoidit
	 */
	private void configureHaeHakukohteidenHakemukset() {
		// 2.1 hae jokaisen hakukohteen hakemusoidit
		from(haeHakukohteidenHakemukset)
		//
				.errorHandler(
						deadLetterChannel(
								deadLetterChannelHaeHakukohteenHakemukset)
								//
								.maximumRedeliveries(4)
								//
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				// applications
				.process(security)
				//
				.process(hakemusOiditHakuApplta())
		//
		;

	}

	/**
	 * Hakemukset
	 */
	private void configureHaeHakemukset() {
		// final String haeHakemus = haeHakemukset + "_yksittainen";
		final String haeHakemusOiditCachesta = haeHakemukset
				+ "_oidit_hakukohteesta";
		// 2.1.1 resolvaa yksilölliset hakemusoidit joka kohteesta
		from(haeHakemukset)
		// <- Null body
				.process(security)
				// update hakemus works
				.process(getHakukohteetInOrder())
				//
				// operaation kesto + ...
				// 123ms, haku-app/applications/...
				.split(body())
				//
				.stopOnException()
				//
				.shareUnitOfWork()
				//
				// .parallelProcessing()
				//
				.to(haeHakemusOiditCachesta)
				//
				.end();

		from(haeHakemusOiditCachesta)
		//
				.process(security)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						/**
						 * Thread takes hakemusOids from hakukohde that is not
						 * yet fetched
						 */
						exchange.getOut().setBody(
								cache(exchange).takeFromHakemattomatHakemukset(
										exchange.getIn()
												.getBody(HakukohdeKey.class)
												.getHakemusOids()));
					}
				})
				//
				.split(body())
				//
				// .executorService(hakuAppExecutorService)
				//
				.stopOnException()
				//
				.shareUnitOfWork()
				//
				// .parallelProcessing()
				//
				.to(haeHakemus)
				//
				.end();

		from(haeHakemus)
		//
				.choice()
				//
				.when(hasPoikkeuksia())
				//
				.log("Peruutetaan hakemuksen haku poikkeusten vuoksi!")
				//
				.otherwise()
				//
				.to(haeHakemusYksittainen)
				//
				.end();

		from(haeHakemusYksittainen)
				//
				.errorHandler(
						deadLetterChannel(deadLetterChannelHaeHakemus)
								//
								.maximumRedeliveries(4)
								//
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				// // /applications
				.process(security)
				//
				.process(hakemusHakuApplta())
				// Palauttaa valmistuneet tyot : List<HakukohdeKey>
				.choice()
				//
				.when(body().isNotNull())
				//
				.to(aloitaLaskenta)
				//
				.otherwise()
				//
				.log(DEBUG, "Yksikään työ ei valmistunut hakemuksen saapuessa!")
				//
				.end();
	}

	/**
	 * Valintaperusteet
	 */
	private void configureHaeValintaperusteet() {
		// 2.2 jokaisen hakukohteen valintaperusteet
		from(haeValintaperusteet)
		//
				.process(security)
				//
				.process(getHakukohteetInOrder())
				//
				.split(body())
				//
				// .executorService(valintaperusteetExecutorService)
				//
				.stopOnException()
				//
				.shareUnitOfWork()
				//
				// .parallelProcessing()
				//
				.to(haeValintaperuste)
				//
				.end();

		from(haeValintaperuste)
		// //

				//
				.choice()
				//
				.when(hasPoikkeuksia())
				//
				.log("Peruutetaan valintaperusteiden haku poikkeusten vuoksi!")
				//
				.otherwise()
				//
				.to(haeValintaperusteetYksittainen)
				//
				.end();

		from(haeValintaperusteetYksittainen)
		//
				.errorHandler(
						deadLetterChannel(deadLetterChannelHaeValintaperusteet)
								//
								.maximumRedeliveries(4)
								//
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				// // /applications
				.process(security)
				//
				.process(valintaperusteet())
				//
				// Palauttaa valmistuneet tyot : List<HakukohdeKey>
				.choice()
				//
				.when(body().isNotNull())
				//
				.to(aloitaLaskenta)
				//
				.otherwise()
				//
				.log(DEBUG, "Työ ei valmistunut valintaperusteen saapuessa!")
				//
				.end();
	}

	private Predicate hasPoikkeuksia() {
		return simple("${property." + valvomoProsessi + ".hasPoikkeuksia()}");
	}

	/**
	 * Valintalaskenta
	 */
	private void configureAloitaLaskenta() {
		final String aloitaLaskentaYksittainen = aloitaLaskenta
				+ "_yksittainen";

		from(aloitaLaskenta)
		// List<HakukohdeKey>
				.split(body())
				//
				.to(valintalaskentaTyojonoon)
				//
				.end();

		from(valintalaskentaTyojonoon)
		//

				//
				.choice()
				//
				.when(hasPoikkeuksia())
				//
				.log("Peruutetaan valintalaskenta poikkeusten vuoksi!")
				//
				.otherwise()
				//
				.to(aloitaLaskentaYksittainen)
				//
				.end();

		from(aloitaLaskentaYksittainen)
		//
				.errorHandler(
						deadLetterChannel(deadLetterChannelTeeValintalaskenta)
								//
								.maximumRedeliveries(4)
								//
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.process(security)
				//
				.process(valintalaskenta())
				//
				.choice()
				// Is it last job?
				.when(body().isEqualTo(TRUE))
				//
				.to(finish)
				//
				.end();

	}

	private void configureDeadLetterChannels() {
		from(deadLetterChannelHaeHakukohteenHakemukset)
		//
				.log(ERROR, "Hakukohteen hakemusten oideja ei voitu hakea")
				//
				.to(fail);
		from(deadLetterChannelHaeHakemus)
		//
				.log(ERROR, "Hakukohteen hakemusta ei saatu haettua")
				//
				.to(fail);
		from(deadLetterChannelHaeValintaperusteet)
		//
				.log(ERROR,
						"Hakukohteen valintaperusteita ei onnistuttu haettua")
				//
				.to(fail);
		from(deadLetterChannelTeeValintalaskenta)
		//
				.log(ERROR, "Valintalaskentaa ei voitu tehdä")
				//
				.to(fail);
	}

	@Override
	public void configure() throws Exception {
		configureValintalaskentaMuistissa();
		configureHaeHakemukset();
		configureHaeValintaperusteet();
		configureAloitaLaskenta();
		configureHaeHakukohteidenHakemukset();
		configureDeadLetterChannels();
	}

	private ValintalaskentaMuistissaProsessi prosessi(Exchange exchange) {
		return exchange.getProperty(valvomoProsessi,
				ValintalaskentaMuistissaProsessi.class);
	}

	private ValintalaskentaCache cache(Exchange exchange) {
		return exchange.getProperty(valintalaskentaCache,
				ValintalaskentaCache.class);
	}

	@Autowired
	public ValintalaskentaMuistissaRouteImpl(
			TarjonnanHakukohdeOids tarjontaHakukohdeOids,
			Valintaperusteet valintaperusteet,
			HakuAppHakemus hakuAppHakemus,
			HakuAppHakemusOids hakuAppHakemusOids,
			Valintalaskenta valintalaskenta,
			@Value("bean:valintalaskentaMuistissaValvomo?method=start") String start,
			@Value("bean:valintalaskentaMuistissaValvomo?method=finish") String finish,
			@Value("bean:valintalaskentaMuistissaValvomo?method=fail") String fail,
			@Value("seda:valintalaskentaTyojono?concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.valintalaskenta.threadpoolsize:4}") String seda_valintalaskenta_tyojonoon,
			@Value("seda:haeHakemus?concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.hakemus.threadpoolsize:4}") String haeHakemus,
			@Value("seda:haeValintaperuste?concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.valintaperusteet.threadpoolsize:4}") String haeValintaperuste,
			@Value("valintalaskentaCache") String valintalaskentaCache,
			@Value("direct:valintalaskenta_muistissa_hae_hakukohteiden_hakemukset") String direct_hae_hakukohteiden_hakemukset,
			@Value("direct:valintalaskenta_muistissa_aloita_laskenta") String direct_aloita_laskenta,
			@Value("direct:valintalaskenta_muistissa_hae_muistiin") String direct_hae_hakemukset,
			@Value("direct:valintalaskenta_muistissa_hae_valintaperusteet") String direct_hae_valintaperusteet,
			@Value("direct:valintalaskenta_muistissa_hae_valintaperusteet_yksittainen") String direct_hae_valintaperusteet_yksittainen,
			@Value("direct:valintalaskenta_muistissa_hae_hakemus_yksittainen") String haeHakemusYksittainen,
			@Value("direct:valintalaskenta_muistissa") String direct_valintalaskenta_muistissa,
			@Value("direct:valintalaskenta_muistissa_deadletterchannel_hae_hakukohteiden_hakemukset") String deadLetterChannelHaeHakukohteenHakemukset,
			@Value("direct:valintalaskenta_muistissa_deadletterchannel_hae_hakemus") String deadLetterChannelHaeHakemus,
			@Value("direct:valintalaskenta_muistissa_deadletterchannel_hae_valintaperusteet") String deadLetterChannelHaeValintaperusteet,
			@Value("direct:valintalaskenta_muistissa_deadletterchannel_tee_valintalaskenta") String deadLetterChannelTeeValintalaskenta) {
		this.deadLetterChannelHaeHakemus = deadLetterChannelHaeHakemus;
		this.deadLetterChannelHaeValintaperusteet = deadLetterChannelHaeValintaperusteet;
		this.deadLetterChannelTeeValintalaskenta = deadLetterChannelTeeValintalaskenta;
		this.haeValintaperuste = haeValintaperuste;
		this.haeHakemus = haeHakemus;
		this.finish = finish;
		this.start = start;
		this.fail = fail;
		this.deadLetterChannelHaeHakukohteenHakemukset = deadLetterChannelHaeHakukohteenHakemukset;
		this.valintalaskenta = valintalaskenta;
		this.valintalaskentaTyojonoon = seda_valintalaskenta_tyojonoon;
		this.valintaperusteet = valintaperusteet;
		this.hakuAppHakemusOids = hakuAppHakemusOids;
		this.valintalaskentaCache = valintalaskentaCache;
		this.haeHakukohteidenHakemukset = direct_hae_hakukohteiden_hakemukset;
		this.tarjontaHakukohdeOids = tarjontaHakukohdeOids;
		this.hakuAppHakemus = hakuAppHakemus;
		this.security = new SecurityPreprocessor();
		this.valvomoKuvaus = ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS;
		this.valvomoProsessi = ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;
		this.aloitaLaskenta = direct_aloita_laskenta;
		this.haeHakemukset = direct_hae_hakemukset;
		this.haeValintaperusteet = direct_hae_valintaperusteet;
		this.haeHakemusYksittainen = haeHakemusYksittainen;
		this.haeValintaperusteetYksittainen = direct_hae_valintaperusteet_yksittainen;
		this.valintalaskentaMuistissa = direct_valintalaskenta_muistissa;
	}

	private Processor hakemusHakuApplta() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				String hakemusOid = exchange.getIn().getBody(String.class);
				long kesto = System.currentTimeMillis();
				try {
					Hakemus hakemus = hakuAppHakemus.getHakemus(hakemusOid);
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getHakemukset().tyoValmistui(kesto);

					exchange.getOut().setBody(
							cache(exchange).putHakemus(
									exchange.getContext()
											.getTypeConverter()
											.tryConvertTo(HakemusTyyppi.class,
													hakemus)));

				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getHakemukset().tyoEpaonnistui(kesto, e);
					throw e;
				}
			}
		};
	}

	private Processor valintaperusteet() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				String hakukohdeOid = exchange.getIn().getBody(String.class);
				if (hakukohdeOid == null) {
					throw new RuntimeException(
							"HakukohdeOid valintaperusteet reitillä on null");
				}
				long kesto = System.currentTimeMillis();
				try {
					List<ValintaperusteetTyyppi> v = valintaperusteet
							.getValintaperusteet(hakukohdeOid);
					kesto = System.currentTimeMillis() - kesto;
					if (v == null) {
						throw new RuntimeException(
								"ValintaperusteetService palautti null valintaperusteet listan");
					}
					if (v.isEmpty()) {
						throw new RuntimeException("Hakukohteella ("
								+ hakukohdeOid + ") ei ole valintaperusteita!");
					}
					prosessi(exchange).getValintaperusteet()
							.tyoValmistui(kesto);
					exchange.getOut().setBody(
							cache(exchange)
									.putValintaperusteet(hakukohdeOid, v));
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getValintaperusteet().tyoEpaonnistui(
							kesto, e);
					throw e;
				}
			}
		};
	}

	private Processor hakemusOiditHakuApplta() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				String hakukohdeOid = exchange.getIn().getBody(String.class);
				long kesto = System.currentTimeMillis();
				try {
					Collection<String> hakemusOids = hakuAppHakemusOids
							.getHakemusOids(hakukohdeOid);
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getHakukohteilleHakemukset()
							.tyoValmistui(kesto);
					if (hakemusOids.isEmpty()) {
						cache(exchange).getHakukohdeEmpty().add(hakukohdeOid);
						prosessi(exchange).getValintaperusteet().tyoOhitettu();
						prosessi(exchange).getValintalaskenta().tyoOhitettu();
					} else {
						cache(exchange).putHakemusOids(hakukohdeOid,
								hakemusOids);
					}
					// TODO: hakemusOids
					exchange.getOut().setBody(new Object());
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getHakukohteilleHakemukset()
							.tyoEpaonnistui(kesto, e);
					throw e;
				}

			}
		};
	}

	private Processor tarjonnastaJulkaistutHakukohdeOidit() {
		return new Processor() {

			public void process(Exchange exchange) throws Exception {
				String hakuOid = exchange
						.getProperty(OPH.HAKUOID, String.class);
				Collection<String> hakukohdeOids;
				long kesto = System.currentTimeMillis();
				try {
					hakukohdeOids = tarjontaHakukohdeOids
							.getHakukohdeOids(hakuOid);
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getTarjonnastaHakukohteet()
							.tyoValmistui(kesto);
					cache(exchange).getTarjonnanHakukohteet().addAll(
							hakukohdeOids);
					exchange.getOut().setBody(hakukohdeOids);
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getTarjonnastaHakukohteet()
							.tyoEpaonnistui(kesto, e);
					e.printStackTrace();
					throw e;
				}

			}
		};
	}

	private Processor valintalaskenta() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				HakukohdeKey hakukohdeKey = exchange.getIn().getBody(
						HakukohdeKey.class);
				long kesto = System.currentTimeMillis();
				try {
					valintalaskenta.teeValintalaskenta(
							hakukohdeKey.getHakemukset(),
							hakukohdeKey.getValintaperusteet());
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getValintalaskenta().tyoValmistui(kesto);
					if (prosessi(exchange).getValintalaskenta().isValmis()) {
						exchange.getOut().setBody(TRUE);
					} else {
						exchange.getOut().setBody(null);
					}
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getValintalaskenta().tyoEpaonnistui(
							kesto, e);
					e.printStackTrace();
					throw e;
				}
			}
		};
	}

	private Processor getHakukohteetInOrder() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				/**
				 * HakukohdeOid with least hakijoita first
				 * 
				 * Ordering will speed up the calculation as valintaperusteet is
				 * fetched in same order and calculation starts as soon as first
				 * valintaperusteet+hakemukset pair exists for some hakukohde!
				 */
				List<HakukohdeKey> hakukohteet = cache(exchange)
						.getKasiteltavatHakukohteet();
				Collections.sort(hakukohteet);
				exchange.getOut().setBody(hakukohteet);

			}
		};
	}

	/**
	 * 
	 * @author Jussi Jartamo
	 * 
	 *         Makes unit testing lot easier
	 */
	public static interface TarjonnanHakukohdeOids {

		Collection<String> getHakukohdeOids(String hakuOid) throws Exception;
	}

	public static interface HakuAppHakemusOids {

		Collection<String> getHakemusOids(String hakukohdeOid) throws Exception;
	}

	public static interface HakuAppHakemus {

		Hakemus getHakemus(String hakemusOid) throws Exception;
	}

	public static interface Valintaperusteet {

		List<ValintaperusteetTyyppi> getValintaperusteet(String hakukohdeOid);
	}

	public static interface Valintalaskenta {

		void teeValintalaskenta(List<HakemusTyyppi> hakemukset,
				List<ValintaperusteetTyyppi> valintaperusteet);
	}
}
