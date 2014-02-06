package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HakemusOidSplitter;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohteetTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.SplitHakukohteetKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.HaeValintaperusteetKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.SuoritaLaskentaKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HakukohteenValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoAdminService;

/**
 * @author Jussi Jartamo
 */
@Component
public class ValintalaskentaRouteImpl extends SpringRouteBuilder {

	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaRouteImpl.class);
	// private static final String ENSIMMAINEN_VIRHE =
	// "ensimmainen_virhe_reitilla";

	@Autowired
	private SecurityPreprocessor securityProcessor;

	@Autowired
	private SuoritaLaskentaKomponentti suoritaLaskentaKomponentti;

	@Autowired
	private HaeHakukohteetTarjonnaltaKomponentti haeHakukohteetTarjonnaltaKomponentti;

	@Autowired
	private HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti;

	@Autowired
	private HaeHakemusKomponentti haeHakemusKomponentti;

	@Autowired
	private HaeValintaperusteetKomponentti haeValintaperusteetKomponentti;

	@Override
	public void configure() throws Exception {
		/**
		 * Laskenta dead-letter-channel. Nyt ainoastaan paattaa prosessin.
		 * Jatkossa lisaa metadataa paatettyyn prosessiin yllapitajalle.
		 */

		from(suoritaValintalaskentaHaeValintaperusteetDeadLetterChannel())
		//
				.setHeader(
						"message",
						simple("[${property.authentication.name}] Valintaperusteiden haku ei toimi: Hakukohteelle ${property.hakukohdeOid}"))
				//
				.to(fail());

		from(suoritaValintalaskentaKomponenttiDeadLetterChannel())
		//
				.setHeader(
						"message",
						simple("[${property.authentication.name}] Valinta ei toimi: Hakukohteelle ${property.hakukohdeOid}"))
				//
				.to(fail());
		from(suoritaValintalaskentaDeadLetterChannel())
		//
				.setHeader(
						"message",
						simple("[${property.authentication.name}] Valintalaskennan suoritus ei toimi hakukohteelle ${property.hakukohdeOid} ja valinnanvaiheelle ${property.valinnanvaihe}"))
				//
				.to(fail());
		from(suoritaValintalaskentaHaeHakemusDeadLetterChannel())
		//
				.setHeader(
						"message",
						simple("[${property.authentication.name}] Haku-app ei toimi hakemukselle ${header.hakemusOid} hakukohteessa ${property.hakukohdeOid}"))
				//
				.to(fail());
		from(suoritaHakukohteelleValintalaskentaDeadLetterChannel())
		//
				.setHeader(
						"message",
						simple("[${property.authentication.name}] Valintaperusteiden haku ei toimi: Hakukohteelle ${property.hakukohdeOid} ja valinnanvaiheelle ${property.valinnanvaihe}"))
				//
				.to(fail());
		from(suoritaHaulleValintalaskentaDeadLetterChannel())
		//
				.setHeader(
						"message",
						simple("[${property.authentication.name}] Tarjonta ei toimi: Haulle ${property.hakuOid}"))
				//
				.to(fail());

		from("direct:suorita_haehakemus")
				.errorHandler(
						deadLetterChannel(
								suoritaValintalaskentaHaeHakemusDeadLetterChannel())
								.maximumRedeliveries(10)
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.setHeader(OPH.HAKEMUSOID, body())
				//
				.to("log:direct_suorita_haehakemus?level=INFO&showProperties=true")
				//
				.bean(securityProcessor)
				//
				.bean(haeHakemusKomponentti).convertBodyTo(HakemusTyyppi.class);

		from("direct:suorita_valintalaskenta_komponentti")
		//
				.errorHandler(
						deadLetterChannel(
								suoritaValintalaskentaKomponenttiDeadLetterChannel())
								//
								.maximumRedeliveries(3)
								//
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.bean(suoritaLaskentaKomponentti)
				//
				.bean(hakukohdeKasiteltyProsessille());

		from("direct:hae_hakukohteen_hakemusten_oidit")
		//
				.errorHandler(
						deadLetterChannel(
								suoritaValintalaskentaKomponenttiDeadLetterChannel())
								//
								.maximumRedeliveries(3)
								//
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.bean(haeHakukohteenHakemuksetKomponentti)
				//
				.bean(new HakemusOidSplitter())
				//
				.setHeader("hakemusOidit", body());

		from("direct:suorita_valintalaskenta_loppuun")
				//
				.errorHandler(
						deadLetterChannel(suoritaValintalaskentaDeadLetterChannel()))
				// hakee valintaperusteet hakukohdeOid:lla
				.to("direct:valintalaskenta_haeValintaperusteet")
				//
				.to("log:direct_suorita_valintalaskenta_pre_split_hakemukset?level=INFO")
				//
				.split(header("hakemusOidit"),
						new FlexibleAggregationStrategy<HakemusTyyppi>()
								.storeInHeader("hakemustyypit")
								.accumulateInCollection(ArrayList.class))
				//
				.shareUnitOfWork()
				//
				.parallelProcessing()
				//
				.stopOnException()
				//
				.bean(securityProcessor)
				//
				.to("direct:suorita_haehakemus").end()
				//
				.bean(securityProcessor)
				//
				.to("direct:suorita_valintalaskenta_komponentti");

		/**
		 * Alireitti yhden hakukohteen laskentaan
		 */
		from(suoritaValintalaskenta())
				// jos reitti epaonnistuu parent
				// failaa
				//
				.errorHandler(
						deadLetterChannel(suoritaValintalaskentaDeadLetterChannel()))
				//
				.to("log:direct_suorita_valintalaskenta?level=INFO")
				//
				.bean(securityProcessor)
				//
				.to("direct:hae_hakukohteen_hakemusten_oidit")
				//
				.choice()
				.when(simple("${in.header.hakemusOidit.isEmpty()}"))
				// VT-797
				.log(LoggingLevel.WARN,
						"Hakukohteessa ${property.hakukohdeOid} ei ole hakemuksia!")
				//
				.otherwise()
				//
				.to("direct:suorita_valintalaskenta_loppuun")
				//
				.end();

		from(haunValintalaskenta())
		//
				.errorHandler(
						deadLetterChannel(suoritaHaulleValintalaskentaDeadLetterChannel()))
				//
				.bean(securityProcessor)
				//
				// .setProperty(ENSIMMAINEN_VIRHE, constant(new
				// AtomicBoolean(true)))
				//
				.process(luoProsessiHaunValintalaskennalle())
				//
				.to(start())
				//
				.bean(haeHakukohteetTarjonnaltaKomponentti)
				//
				.process(paivitaHakukohteidenMaaraProsessille())
				// Collection<HakukohdeTyyppi>
				.bean(new SplitHakukohteetKomponentti())
				// Collection<String>
				.split(body())
				//
				.shareUnitOfWork()
				//
				.parallelProcessing()
				//
				.stopOnException()
				//
				.setProperty("hakukohdeOid", body())
				//
				.to(suoritaValintalaskenta())
				// end splitter
				.end()
				// route done
				.to(finish());
		from("direct:valintalaskenta_haeValintaperusteet")
		//
				.errorHandler(
						deadLetterChannel(
								suoritaValintalaskentaHaeValintaperusteetDeadLetterChannel())
								.maximumRedeliveries(10)
								.redeliveryDelay(300L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.bean(haeValintaperusteetKomponentti)
				//
				.setProperty("valintaperusteet", body());
		from(hakukohteenValintalaskenta())
		//
				.errorHandler(
						deadLetterChannel(suoritaHakukohteelleValintalaskentaDeadLetterChannel()))
				//
				.bean(securityProcessor)
				//
				.setHeader("hakukohteitaYhteensa", constant(1))
				//
				.process(luoProsessiHakukohteenValintalaskennalle())
				//
				.to(start())
				//
				.to(suoritaValintalaskenta())
				//
				.to(finish());

	}

	private String suoritaValintalaskenta() {
		return "direct:suorita_valintalaskenta";
	}

	private Processor paivitaHakukohteidenMaaraProsessille() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				ValintalaskentaProsessi valintalaskentaProsessi = exchange
						.getProperty(prosessi(), ValintalaskentaProsessi.class);
				try {
					valintalaskentaProsessi.setHakukohteitaYhteensa(exchange
							.getIn().getBody(List.class).size());
				} catch (Exception e) {
					LOG.error("Tarjonnasta palautui tyhjä joukko hakukohteita haulle!");
				}
			}
		};
	}

	private Processor hakukohdeKasiteltyProsessille() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				ValintalaskentaProsessi valintalaskentaProsessi = exchange
						.getProperty(prosessi(), ValintalaskentaProsessi.class);
				valintalaskentaProsessi.addHakukohde(exchange.getProperty(
						OPH.HAKUKOHDEOID, String.class));
			}
		};
	}

	private Processor luoProsessiHaunValintalaskennalle() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {

				exchange.setProperty(kuvaus(), "Valintalaskenta haulle");
				exchange.setProperty(
						prosessi(),
						new ValintalaskentaProsessi("Valintalaskenta",
								"Haulle", exchange.getProperty(OPH.HAKUOID,
										String.class), exchange.getIn()
										.getHeader("hakukohteitaYhteensa",
												Integer.class), exchange
										.getProperty(OPH.VALINNANVAIHE,
												Integer.class)));
			}
		};
	}

	private Processor luoProsessiHakukohteenValintalaskennalle() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				exchange.setProperty(kuvaus(), "Valintalaskenta hakukohteelle");
				exchange.setProperty(
						prosessi(),
						new ValintalaskentaProsessi("Valintalaskenta",
								"Hakukohteelle", exchange.getProperty(
										OPH.HAKUOID, String.class), exchange
										.getIn().getHeader(
												"hakukohteitaYhteensa",
												Integer.class), exchange
										.getProperty(OPH.VALINNANVAIHE,
												Integer.class)));
			}
		};
	}

	private static String kuvaus() {
		return ValvomoAdminService.PROPERTY_VALVOMO_PROSESSIKUVAUS;
	}

	private static String prosessi() {
		return ValvomoAdminService.PROPERTY_VALVOMO_PROSESSI;
	}

	private static String fail() {
		return "bean:valintalaskentaValvomo?method=fail";
	}

	private static String start() {
		return "bean:valintalaskentaValvomo?method=start";
	}

	private static String finish() {
		return "bean:valintalaskentaValvomo?method=finish";
	}

	private static String suoritaValintalaskentaHaeValintaperusteetDeadLetterChannel() {
		return "direct:suorita_valintalaskenta_haevalintaperusteet_deadletterchannel";
	}

	private static String suoritaValintalaskentaKomponenttiDeadLetterChannel() {
		return "direct:suorita_valintalaskenta_komponentti_deadletterchannel";
	}

	private static String suoritaHakukohteelleValintalaskentaDeadLetterChannel() {
		return "direct:suorita_hakukohteelle_valintalaskenta_deadletterchannel";
	}

	private static String suoritaHaulleValintalaskentaDeadLetterChannel() {
		return "direct:suorita_haulle_valintalaskenta_deadletterchannel";
	}

	private static String suoritaValintalaskentaDeadLetterChannel() {
		return "direct:suorita_valintalaskenta_deadletterchannel";
	}

	private static String suoritaValintalaskentaHaeHakemusDeadLetterChannel() {
		return "direct:suorita_laskenta_haehakemus_deadletterchannel";
	}

	private String hakukohteenValintalaskenta() {
		return HakukohteenValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAKUKOHTEELLE;
	}

	private String haunValintalaskenta() {
		return HaunValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAULLE;
	}
}
