package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
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
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.HakemusTyo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaTyo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintaperusteetTyo;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
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
	private final String valintalaskentaTyojono;
	private final String valintalaskentaCache;
	private final String valvomoKuvaus;
	private final String valvomoProsessi;
	private final String valintalaskentaMuistissa;
	private final String aloitaLaskenta;
	private final String hakukohteidenHakemuksetTyojono;
	// private final String haeValintaperusteet;
	private final String hakemusTyojono;
	private final String haeHakemusYksittainen;
	private final String valintaperusteetTyojono;
	private final String haeValintaperusteetYksittainen;
	private final SecurityPreprocessor security;
	private final HakuAppHakemus hakuAppHakemus;
	private final HakuAppHakemusOids hakuAppHakemusOids;
	private final Valintaperusteet valintaperusteet;
	private final Valintalaskenta valintalaskenta;

	private ValintalaskentaTila valintalaskentaTila;

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
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						exchange.setProperty(
								valvomoProsessi,
								new ValintalaskentaMuistissaProsessi(exchange
										.getProperty(OPH.HAKUOID, String.class)));
					}
				})
				//
				.end()
				//
				.to(start)
				// 1. hae tarjonnalta haun hakukohteet
				// List<String>
				.log(INFO, "Aloitetaan tiedon keräys muistiin")
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						Collection<String> c = cache(exchange).getHakukohteet();
						exchange.getOut().setBody(c);
						// update work
						prosessi(exchange).getHakukohteilleHakemukset()
								.setKokonaismaara(c.size());
						prosessi(exchange).getValintalaskenta()
								.setKokonaismaara(c.size());
						prosessi(exchange).getValintaperusteet()
								.setKokonaismaara(c.size());
					}
				})
				//
				.split(body())
				//
				// .setExecutorService(executorService);
				//
				.stopOnException()
				//
				.shareUnitOfWork()
				//
				// .parallelProcessing()
				//
				.to(hakukohteidenHakemuksetTyojono)
				//
				.end();
	}

	/**
	 * Hakukohteen hakemusoidit
	 */
	private void configureHaeHakukohteidenHakemukset() {
		// 2.1 hae jokaisen hakukohteen hakemusoidit
		from(hakukohteidenHakemuksetTyojono)
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
				// hakukohteella hakemuksia?
				.process(hakemusOiditHakuApplta())
				// Collection< TYO >
				.split(body())
				//
				.to("direct:split_to_tyojono")
				//
				.end();

		from("direct:split_to_tyojono")
		//
				.choice()
				//
				.when(body().isInstanceOf(ValintaperusteetTyo.class))
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						// prosessi(exchange).getValintaperusteet().inkrementoiKokonaismaaraa();
					}
				}).to(valintaperusteetTyojono)
				//
				.when(body().isInstanceOf(HakemusTyo.class))
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						prosessi(exchange).getHakemukset()
								.inkrementoiKokonaismaaraa();
					}
				}).to(hakemusTyojono)
				//
				.when(body().isInstanceOf(ValintalaskentaTyo.class))
				//
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						// prosessi(exchange).getValintalaskenta().inkrementoiKokonaismaaraa();
					}
				}).to(valintalaskentaTyojono)
				//
				.otherwise()
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						throw new RuntimeException("Ei työtä! "
								+ exchange.getIn().getBody());
					}
				})
				//
				.end();
		//

	}

	/**
	 * Hakemukset
	 */
	private void configureHaeHakemukset() {
		// 2.1.1 resolvaa yksilölliset hakemusoidit joka kohteesta
		// => (hakukohdeKey)

		from(hakemusTyojono)
		//
				.choice()
				//
				.when(hasNoPoikkeuksia())
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
				// => hakemusOid
				.process(hakemusHakuApplta())
				//
				.split(body())
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						// prosessi(exchange).getValintalaskenta().inkrementoiKokonaismaaraa();
					}
				}).to(valintalaskentaTyojono)
				//
				.end();
	}

	/**
	 * Valintaperusteet
	 */
	private void configureHaeValintaperusteet() {
		from(valintaperusteetTyojono)
		//
				.choice()
				//
				.when(hasNoPoikkeuksia())
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
				.split(body())
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						// prosessi(exchange).getValintalaskenta().inkrementoiKokonaismaaraa();
					}
				}).to(valintalaskentaTyojono)
				//
				.end();
	}

	private Predicate hasNoPoikkeuksia() {
		// simple("${property." + valvomoProsessi + ".hasPoikkeuksia()}");
		return new Predicate() {
			public boolean matches(Exchange exchange) {
				return prosessi(exchange).getExceptions().isEmpty();
			}
		};
	}

	/**
	 * Valintalaskenta
	 */
	private void configureAloitaLaskenta() {
		final String aloitaLaskentaYksittainen = aloitaLaskenta
				+ "_yksittainen";
		/**
		 * @Body Collection<HakukohdeKey>
		 */
		from(aloitaLaskenta)
		// List<HakukohdeKey>
				.split(body())
				//
				.to(valintalaskentaTyojono)
				//
				.end();

		from(valintalaskentaTyojono)
		//

				//
				.choice()
				//
				.when(hasNoPoikkeuksia())
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
				.choice()
				//
				.when(valintalaskenta())
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						valintalaskentaTila.getKaynnissaOlevaValintalaskenta()
								.set(null);
					}
				}).to(finish)
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
			ValintalaskentaTila valintalaskentaTila,
			Valintaperusteet valintaperusteet,
			HakuAppHakemus hakuAppHakemus,
			HakuAppHakemusOids hakuAppHakemusOids,
			Valintalaskenta valintalaskenta,
			@Value("bean:valintalaskentaMuistissaValvomo?method=start") String start,
			@Value("bean:valintalaskentaMuistissaValvomo?method=finish") String finish,
			@Value("bean:valintalaskentaMuistissaValvomo?method=fail") String fail,
			@Value("seda:valintalaskentaTyojono?purgeWhenStopping=true&waitForTaskToComplete=Never&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.valintalaskenta.threadpoolsize:4}") String valintalaskentaTyojono,
			@Value("seda:haeHakemus?purgeWhenStopping=true&waitForTaskToComplete=Never&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.hakemus.threadpoolsize:4}") String hakemusTyojono,
			@Value("seda:haeValintaperuste?purgeWhenStopping=true&waitForTaskToComplete=Never&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.valintaperusteet.threadpoolsize:4}") String valintaperusteetTyojono,
			@Value("seda:haeHakukohteidenHakemukset?purgeWhenStopping=true&waitForTaskToComplete=Never&concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.hakukohteidenhakemukset.threadpoolsize:4}") String hakukohteidenHakemuksetTyojono,
			@Value("valintalaskentaCache") String valintalaskentaCache,
			@Value("direct:valintalaskenta_muistissa_aloita_laskenta") String direct_aloita_laskenta,
			@Value("direct:valintalaskenta_muistissa_hae_valintaperusteet_yksittainen") String direct_hae_valintaperusteet_yksittainen,
			@Value("direct:valintalaskenta_muistissa_hae_hakemus_yksittainen") String haeHakemusYksittainen,
			@Value(ValintalaskentaMuistissaRoute.SEDA_VALINTALASKENTA_MUISTISSA) String valintalaskentaMuistissa,
			@Value("direct:valintalaskenta_muistissa_deadletterchannel_hae_hakukohteiden_hakemukset") String deadLetterChannelHaeHakukohteenHakemukset,
			@Value("direct:valintalaskenta_muistissa_deadletterchannel_hae_hakemus") String deadLetterChannelHaeHakemus,
			@Value("direct:valintalaskenta_muistissa_deadletterchannel_hae_valintaperusteet") String deadLetterChannelHaeValintaperusteet,
			@Value("direct:valintalaskenta_muistissa_deadletterchannel_tee_valintalaskenta") String deadLetterChannelTeeValintalaskenta) {
		// deadletterchannelit
		this.valintalaskentaTila = valintalaskentaTila;
		this.deadLetterChannelHaeHakemus = deadLetterChannelHaeHakemus;
		this.deadLetterChannelHaeValintaperusteet = deadLetterChannelHaeValintaperusteet;
		this.deadLetterChannelTeeValintalaskenta = deadLetterChannelTeeValintalaskenta;
		this.finish = finish;
		this.start = start;
		this.fail = fail;
		this.deadLetterChannelHaeHakukohteenHakemukset = deadLetterChannelHaeHakukohteenHakemukset;
		this.valintalaskenta = valintalaskenta;
		this.valintaperusteet = valintaperusteet;
		this.hakuAppHakemusOids = hakuAppHakemusOids;
		this.valintalaskentaCache = valintalaskentaCache;
		this.hakuAppHakemus = hakuAppHakemus;
		this.security = new SecurityPreprocessor();
		this.valvomoKuvaus = ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS;
		this.valvomoProsessi = ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;
		this.aloitaLaskenta = direct_aloita_laskenta;
		this.haeHakemusYksittainen = haeHakemusYksittainen;
		this.haeValintaperusteetYksittainen = direct_hae_valintaperusteet_yksittainen;

		// tyojonot
		this.valintalaskentaMuistissa = valintalaskentaMuistissa;
		this.valintalaskentaTyojono = valintalaskentaTyojono;
		this.hakukohteidenHakemuksetTyojono = hakukohteidenHakemuksetTyojono;
		this.valintaperusteetTyojono = valintaperusteetTyojono;
		this.hakemusTyojono = hakemusTyojono;
	}

	private Processor hakemusHakuApplta() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				HakemusTyo hakemusTyo = exchange.getIn().getBody(
						HakemusTyo.class);
				if (hakemusTyo == null) {
					throw new RuntimeException(
							"Yritetään tehdä null hakemusTyolla hakua haku-app:sta!");
				}
				long kesto = System.currentTimeMillis();
				try {
					HakemusTyyppi hakemusTyyppi = exchange
							.getContext()
							.getTypeConverter()
							.tryConvertTo(
									HakemusTyyppi.class,
									hakuAppHakemus.getHakemus(hakemusTyo
											.getOid()));
					kesto = System.currentTimeMillis() - kesto;
					if (hakemusTyyppi == null) {
						throw new RuntimeException(
								"Haku-App palautti null hakemuksen oidille "
										+ hakemusTyo.getOid());
					}
					prosessi(exchange).getHakemukset().tyoValmistui(kesto);
					/**
					 * Valintalaskentatyojonoon!
					 */
					exchange.getIn().setBody(
							cache(exchange).esitietoHaettu(hakemusTyo.getOid(),
									hakemusTyyppi));
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getHakemukset().tyoEpaonnistui(kesto, e);
					throw new RuntimeException(e);
				}
			}
		};
	}

	private Processor valintaperusteet() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				ValintaperusteetTyo valintaperusteetTyo = exchange.getIn()
						.getBody(ValintaperusteetTyo.class);
				if (valintaperusteetTyo == null) {
					throw new RuntimeException(
							"ValintaperusteetTyo reitillä on null");
				}
				long kesto = System.currentTimeMillis();
				try {
					List<ValintaperusteetTyyppi> v = valintaperusteet
							.getValintaperusteet(valintaperusteetTyo.getOid());
					kesto = System.currentTimeMillis() - kesto;
					if (v == null || v.isEmpty()) {
						prosessi(exchange)
								.getVaroitukset()
								.add(new Varoitus(valintaperusteetTyo.getOid(),
										"Hakukohteella ei ole valintaperusteita"));
						prosessi(exchange).getValintaperusteet().tyoOhitettu();
						exchange.getIn().setBody(
								cache(exchange).esitietoOhitettu(
										valintaperusteetTyo.getOid()));
					} else {
						prosessi(exchange).getValintaperusteet().tyoValmistui(
								kesto);
						exchange.getIn().setBody(
								cache(exchange).esitietoHaettu(
										valintaperusteetTyo.getOid(), v));
					}
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getValintaperusteet().tyoEpaonnistui(
							kesto, e);
					throw new RuntimeException(e);
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
						prosessi(exchange).getValintaperusteet().tyoOhitettu();
						prosessi(exchange).getValintalaskenta().tyoOhitettu();
						exchange.getIn().setBody(Collections.emptyList());
					} else {
						exchange.getIn()
								.setBody(
										cache(exchange)
												.hakukohteenEsitiedotOnSelvitettyJaSeuraavaksiEsitiedotTyojonoihin(
														hakukohdeOid,
														hakemusOids));

					}

				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getHakukohteilleHakemukset()
							.tyoEpaonnistui(kesto, e);
					throw new RuntimeException(e);
				}
			}
		};

	}

	/*
	 * private Processor hakemusOiditHakuApplta() {
	 * 
	 * return new Processor() { public void process(Exchange exchange) throws
	 * Exception {
	 * 
	 * 
	 * } }; }
	 */

	private Predicate valintalaskenta() {
		return new Predicate() {
			@Override
			public boolean matches(Exchange exchange) {

				ValintalaskentaTyo valintalaskentaTyo = exchange.getIn()
						.getBody(ValintalaskentaTyo.class);
				if (valintalaskentaTyo.isOhitettu()) {
					prosessi(exchange)
							.getVaroitukset()
							.add(new Varoitus(valintalaskentaTyo
									.getHakukohdeOid(),
									"Valintalaskentaa ei tehty hakukohteelle puuttuvien tietojen vuoksi."));
					prosessi(exchange).getValintalaskenta().tyoOhitettu();
					return prosessi(exchange).getValintalaskenta().isValmis();
				}
				long kesto = System.currentTimeMillis();
				try {
					valintalaskenta.teeValintalaskenta(
							valintalaskentaTyo.getHakemustyypit(),
							valintalaskentaTyo.getValintaperusteet());
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getValintalaskenta().tyoValmistui(kesto);
					return prosessi(exchange).getValintalaskenta().isValmis();
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getValintalaskenta().tyoEpaonnistui(
							kesto, e);
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		};
	}

	/**
	 * 
	 * @author Jussi Jartamo
	 * 
	 *         Makes unit testing lot easier
	 */
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
