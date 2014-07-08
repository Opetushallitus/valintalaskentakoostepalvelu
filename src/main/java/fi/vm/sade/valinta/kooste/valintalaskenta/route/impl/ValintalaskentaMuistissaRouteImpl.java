package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import static org.apache.camel.LoggingLevel.ERROR;
import static org.apache.camel.LoggingLevel.INFO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;
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
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.HakemusTyoRest;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaTyoRest;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintaperusteetTyoRest;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Varoitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

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
public class ValintalaskentaMuistissaRouteImpl extends
		AbstractDokumenttiRouteBuilder {
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaMuistissaRouteImpl.class);
	private final static int UUDELLEEN_YRITYSTEN_MAARA = 3;
	private final static long UUDELLEEN_YRITYSTEN_ODOTUSAIKA = 10000L;

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
	private final HakuAppHakemus hakuAppHakemus;
	private final HakuAppHakemusOids hakuAppHakemusOids;
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
				.process(SecurityPreprocessor.SECURITY)
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
								.maximumRedeliveries(UUDELLEEN_YRITYSTEN_MAARA)
								//
								.redeliveryDelay(UUDELLEEN_YRITYSTEN_ODOTUSAIKA)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				// applications
				.process(SecurityPreprocessor.SECURITY)
				//
				.choice()
				//
				.when(hasNoPoikkeuksia())
				// hakukohteella hakemuksia?
				.process(hakemusOiditHakuApplta())
				//
				.to("direct:haeHakukohteidenHakemukset_splitter")
				//
				.otherwise()
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						// LOG.debug("Prosessin suoritus peruutettu. Tyhjennetään työjonoa!");
					}
				})
				//
				.end();
		// Collection< TYO >

		from("direct:haeHakukohteidenHakemukset_splitter")
		//
				.split(body())
				//
				.to("direct:haeHakukohteidenHakemukset_split_to_tyojono")
				//
				.end();
		from("direct:haeHakukohteidenHakemukset_split_to_tyojono")
		//
				.choice()
				//
				.when(body().isInstanceOf(ValintaperusteetTyoRest.class))
				//
				.to(valintaperusteetTyojono)
				//
				.when(body().isInstanceOf(HakemusTyoRest.class))
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						prosessi(exchange).getHakemukset()
								.inkrementoiKokonaismaaraa();
					}
				}).to(hakemusTyojono)
				//
				.when(body().isInstanceOf(ValintalaskentaTyoRest.class))
				//
				.to(valintalaskentaTyojono)
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
								.maximumRedeliveries(UUDELLEEN_YRITYSTEN_MAARA)
								//
								.redeliveryDelay(UUDELLEEN_YRITYSTEN_ODOTUSAIKA)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				// // /applications
				.process(SecurityPreprocessor.SECURITY)
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
								.maximumRedeliveries(UUDELLEEN_YRITYSTEN_MAARA)
								//
								.redeliveryDelay(UUDELLEEN_YRITYSTEN_ODOTUSAIKA)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				// // /applications
				.process(SecurityPreprocessor.SECURITY)
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
								.maximumRedeliveries(UUDELLEEN_YRITYSTEN_MAARA)
								//
								.redeliveryDelay(UUDELLEEN_YRITYSTEN_ODOTUSAIKA)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.choice()
				//
				.when(valintalaskenta())
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
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).getPoikkeukset().add(
								new Poikkeus(Poikkeus.HAKU,
										"Hakuun liittyvien hakemusten haku",
										Poikkeus.hakukohdeOid(exchange.getIn()
												.getBody(String.class))));
					}
				})
				//
				.to(fail);
		from(deadLetterChannelHaeHakemus)
		//
				.log(ERROR, "Hakukohteen hakemusta ei saatu haettua")
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange)
								.getPoikkeukset()
								.add(new Poikkeus(
										Poikkeus.HAKU,
										"Hakemuksen haku",
										Poikkeus.hakemusOid(oid(hakemus(exchange)))));
					}
				})
				//
				.to(fail);
		from(deadLetterChannelHaeValintaperusteet)
		//
				.log(ERROR,
						"Hakukohteen valintaperusteita ei onnistuttu haettua")
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange)
								.getPoikkeukset()
								.add(new Poikkeus(
										Poikkeus.VALINTAPERUSTEET,
										"Valintaperusteiden haku",
										Poikkeus.hakukohdeOid(oid(valintaperusteet(exchange)))));
					}
				})
				//
				.to(fail);
		from(deadLetterChannelTeeValintalaskenta)
		//
				.log(ERROR, "Valintalaskentaa ei voitu tehdä")
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange)
								.getPoikkeukset()
								.add(new Poikkeus(
										Poikkeus.VALINTALASKENTA,
										"Valintalaskenta epäonnistui",
										Poikkeus.hakukohdeOid(oid(valintalaskenta(exchange)))));
					}
				})
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
			Valintaperusteet valintaperusteet,
			HakuAppHakemus hakuAppHakemus,
			HakuAppHakemusOids hakuAppHakemusOids,
			Valintalaskenta valintalaskenta,
			@Value("bean:valintalaskentaMuistissaValvomo?method=start") String start,
			@Value("bean:valintalaskentaMuistissaValvomo?method=finish") String finish,
			@Value("bean:valintalaskentaMuistissaValvomo?method=fail") String fail,
			@Value("seda:valintalaskentaTyojono?" +
			//
			// "queueFactory=#defaultQueueFactory&" +
			//
					"purgeWhenStopping=true&waitForTaskToComplete=Never&" +
					//
					"concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.valintalaskenta.threadpoolsize:5}") String valintalaskentaTyojono,
			@Value("seda:haeHakemus?" +
			//
			// "queueFactory=#defaultQueueFactory&" +
			//
					"purgeWhenStopping=true&waitForTaskToComplete=Never&" +
					//
					"concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.hakemus.threadpoolsize:5}") String hakemusTyojono,
			@Value("seda:haeValintaperuste?" +
			//
			// "queueFactory=#defaultQueueFactory&" +
			//
					"purgeWhenStopping=true&waitForTaskToComplete=Never&"
					//
					+ "concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.valintaperusteet.threadpoolsize:5}") String valintaperusteetTyojono,
			@Value("seda:haeHakukohteidenHakemukset?" +
			//
			// "queueFactory=#defaultQueueFactory&" +
			//
					"purgeWhenStopping=true&waitForTaskToComplete=Never&" +
					//
					"concurrentConsumers=${valintalaskentakoostepalvelu.valintalaskentamuistissa.hakukohteidenhakemukset.threadpoolsize:5}") String hakukohteidenHakemuksetTyojono,
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

	private HakemusTyoRest<?> hakemus(Exchange exchange) {
		return exchange.getIn().getBody(HakemusTyoRest.class);
	}

	private Processor hakemusHakuApplta() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				HakemusTyoRest<?> hakemusTyo = hakemus(exchange);
				if (hakemusTyo == null) {
					throw new RuntimeException(
							"Yritetään tehdä null hakemusTyolla hakua haku-app:sta!");
				}
				long kesto = System.currentTimeMillis();
				try {
					HakemusDTO hakemusTyyppi = exchange
							.getContext()
							.getTypeConverter()
							.tryConvertTo(
                                    HakemusDTO.class,
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
							cache(exchange).esitietoHaettuRest(hakemusTyo.getOid(),
									hakemusTyyppi));
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					LOG.error(
							"Hakemuksen({}) haussa tuli virhe! Yritetään uudelleen! {} {}",
							hakemusTyo.getOid(), e.getMessage(), e.getCause());
					prosessi(exchange).getHakemukset().tyoEpaonnistui(kesto, e);
					throw new RuntimeException(e);
				}
			}
		};
	}

	private ValintaperusteetTyoRest<?> valintaperusteet(Exchange exchange) {
		return exchange.getIn().getBody(ValintaperusteetTyoRest.class);
	}

	private String oid(ValintalaskentaTyoRest v) {
		if (v == null) {
			return StringUtils.EMPTY;
		}
		return v.getHakukohdeOid();
	}

	private String oid(HakemusTyoRest<?> v) {
		if (v == null) {
			return StringUtils.EMPTY;
		}
		return v.getOid();
	}

	private String oid(ValintaperusteetTyoRest<?> v) {
		if (v == null) {
			return StringUtils.EMPTY;
		}
		return v.getOid();
	}

	private Processor valintaperusteet() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				ValintaperusteetTyoRest<?> valintaperusteetTyo = valintaperusteet(exchange);
				if (valintaperusteetTyo == null) {
					throw new RuntimeException(
							"ValintaperusteetTyoRest reitillä on null");
				}
				long kesto = System.currentTimeMillis();
				try {
					List<ValintaperusteetDTO> v = valintaperusteet
							.getValintaperusteetRest(valintaperusteetTyo.getOid(),
									exchange.getProperty("valinnanvaihe", null,
											Integer.class));
					kesto = System.currentTimeMillis() - kesto;
					if (v == null || v.isEmpty()) {
						prosessi(exchange)
								.getVaroitukset()
								.add(new Varoitus(valintaperusteetTyo.getOid(),
										"Hakukohteella ei ole valintaperusteita"));
						prosessi(exchange).getValintaperusteet().tyoOhitettu();
						exchange.getIn().setBody(
								cache(exchange).esitietoOhitettuRest(
										valintaperusteetTyo.getOid()));
					} else {
						prosessi(exchange).getValintaperusteet().tyoValmistui(
								kesto);
						exchange.getIn().setBody(
								cache(exchange).esitietoHaettuRest(
										valintaperusteetTyo.getOid(), v));
					}
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
                    e.printStackTrace();
					LOG.error(
							"Valintaperusteiden({}) haussa tuli virhe! Yritetään uudelleen! {} {}",
							valintaperusteetTyo.getOid(), e.getMessage(),
							e.getCause());
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
				String hakuOid = hakuOid(exchange); // .getIn().getBody(String.class);
				String hakukohdeOid = exchange.getIn().getBody(String.class);
				long kesto = System.currentTimeMillis();
				try {
					Collection<String> hakemusOids = hakuAppHakemusOids
							.getHakemusOids(hakuOid, hakukohdeOid);
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
												.hakukohteenEsitiedotOnSelvitettyJaSeuraavaksiEsitiedotTyojonoihinRest(
														hakukohdeOid,
														hakemusOids));

					}

				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					LOG.error(
							"Hakemusten haussa haulle({}) tuli virhe! Yritetään uudelleen! {} {}",
							hakuOid, e.getMessage(), e.getCause());
					prosessi(exchange).getHakukohteilleHakemukset()
							.tyoEpaonnistui(kesto, e);
					throw new RuntimeException(e);
				}
			}
		};

	}

	private ValintalaskentaTyoRest valintalaskenta(Exchange exchange) {
		return exchange.getIn().getBody(ValintalaskentaTyoRest.class);
	}

	private Predicate valintalaskenta() {
		return new Predicate() {
			@Override
			public boolean matches(Exchange exchange) {
				ValintalaskentaTyoRest valintalaskentaTyo = valintalaskenta(exchange);
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
					valintalaskenta.teeValintalaskentaRest(
							valintalaskentaTyo.getHakemustyypit(),
							valintalaskentaTyo.getValintaperusteet());
					kesto = System.currentTimeMillis() - kesto;
					prosessi(exchange).getValintalaskenta().tyoValmistui(kesto);
					return prosessi(exchange).getValintalaskenta().isValmis();
				} catch (Exception e) {
					kesto = System.currentTimeMillis() - kesto;
					LOG.error(
							"Valintalaskennassa tuli virhe hakukohteelle({})! Yritetään uudelleen! {} {}",
							valintalaskentaTyo.getHakukohdeOid(),
							e.getMessage(), e.getCause());
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

		Collection<String> getHakemusOids(String hakuOid, String hakukohdeOid)
				throws Exception;
	}

	public static interface HakuAppHakemus {

		Hakemus getHakemus(String hakemusOid) throws Exception;
	}

	public static interface Valintaperusteet {

		List<ValintaperusteetTyyppi> getValintaperusteet(String hakukohdeOid,
				Integer valinnanvaihe);

        List<ValintaperusteetDTO> getValintaperusteetRest(String hakukohdeOid,
                                                         Integer valinnanvaihe);
	}

	public static interface Valintalaskenta {

		void teeValintalaskenta(List<HakemusTyyppi> hakemukset,
				List<ValintaperusteetTyyppi> valintaperusteet);

        void teeValintalaskentaRest(List<HakemusDTO> hakemukset,
                                List<ValintaperusteetDTO> valintaperusteet);
	}

	protected String hakuOid(Exchange exchange) {
		return exchange.getProperty(OPH.HAKUOID, String.class);
	}
}
