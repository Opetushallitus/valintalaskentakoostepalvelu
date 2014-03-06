package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static org.apache.camel.builder.PredicateBuilder.not;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Koekutsukirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class KoekutsukirjeRouteImpl extends AbstractDokumenttiRoute {
	private static final Logger LOG = LoggerFactory
			.getLogger(KoekutsukirjeRouteImpl.class);
	private final ViestintapalveluResource viestintapalveluResource;
	private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
	private final ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti;
	private final ApplicationResource applicationResource;
	private final DokumenttiResource dokumenttiResource;
	private final SecurityPreprocessor security = new SecurityPreprocessor();
	// private final ValintakoeResource valintakoeResource;
	private final String koekutsukirjeet;
	private final String hakemuksilleKoekutsukirjeet;
	private final String hakemusOids;

	@Autowired
	public KoekutsukirjeRouteImpl(
			@Value(KoekutsukirjeRoute.SEDA_KOEKUTSUKIRJEET) String koekutsukirjeet,
			@Value("direct:koekutsukirjeet_hakemuksilleKoekutsukirjeet") String hakemuksilleKoekutsukirjeet,
			@Value("hakemusOids") String hakemusOids,
			@Qualifier("dokumenttipalveluRestClient") DokumenttiResource dokumenttiResource,
			// ValintakoeResource valintakoeResource,
			ViestintapalveluResource viestintapalveluResource,
			KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
			ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti,
			ApplicationResource applicationResource) {
		super();
		this.hakemusOids = hakemusOids;
		this.koekutsukirjeet = koekutsukirjeet;
		this.hakemuksilleKoekutsukirjeet = hakemuksilleKoekutsukirjeet;
		// this.valintakoeResource = valintakoeResource;
		this.viestintapalveluResource = viestintapalveluResource;
		this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
		this.valintatietoHakukohteelleKomponentti = valintatietoHakukohteelleKomponentti;
		this.applicationResource = applicationResource;
		this.dokumenttiResource = dokumenttiResource;
	}

	@Override
	public void configure() throws Exception {
		configureDeadLetterChannels();
		congifureKoekutsukirjeet();
		congifureProsessointi();
	}

	private void congifureKoekutsukirjeet() {
		from(koekutsukirjeet)
				//
				.errorHandler(
				//
						deadLetterChannel(kirjeidenLuontiEpaonnistui())
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(security)
				//
				.choice()
				//
				.when(prosessiOnKeskeytetty())
				//
				.log(LoggingLevel.WARN,
						"Ohitetaan prosessi ${property.property_valvomo_prosessi} koska se on merkitty keskeytetyksi!")
				//
				.otherwise()
				//
				.choice()
				// Jos luodaan vain yksittaiselle hakemukselle...
				.when(property(hakemusOids).isNull())
				//
				.to("direct:koekutsukirjeet_hae_valintatiedot_hakemuksille")
				//
				.end()
				//
				.log(LoggingLevel.DEBUG, "Aloitetaan hakemusten haku")
				// ... tehdaan koekutsukirjeet yksittaisille hakemuksille
				.to(hakemuksilleKoekutsukirjeet);
	}

	private void configureDeadLetterChannels() {
		from(kirjeidenLuontiEpaonnistui())
		//
				.log(LoggingLevel.ERROR,
						"Koekutsukirjeiden luonti epaonnistui: ${property.CamelExceptionCaught}");
		// ;
	}

	private void congifureProsessointi() {
		//
		from("direct:koekutsukirjeet_hae_valintatiedot_hakemuksille")
		//
				.errorHandler(
				//
						deadLetterChannel(kirjeidenLuontiEpaonnistui())
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						try {
							exchange.getOut().setBody(
									valintatietoHakukohteelleKomponentti
											.valintatiedotHakukohteelle(
													valintakoeOids(exchange),
													hakukohdeOid(exchange)));
						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Valintatietojen haku hakukohteelle({}) epäonnistui:{}",
									hakukohdeOid(exchange), e.getMessage());
							Collection<Oid> oidit = Lists.newArrayList(Poikkeus
									.valintakoeOids(valintakoeOids(exchange)));
							oidit.add(Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));

							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.VALINTATIETO,
											"Valintatiedot hakukohteelle", e
													.getMessage(), oidit));
							throw e;
						}
					}
				})
				//
				// .bean(valintatietoHakukohteelleKomponentti)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						@SuppressWarnings("unchecked")
						List<HakemusOsallistuminenTyyppi> unfiltered = (List<HakemusOsallistuminenTyyppi>) exchange
								.getIn().getBody();
						Collection<HakemusOsallistuminenTyyppi> filtered = Collections2
								.filter(unfiltered,
										new com.google.common.base.Predicate<HakemusOsallistuminenTyyppi>() {
											public boolean apply(
													HakemusOsallistuminenTyyppi o) {
												for (ValintakoeOsallistuminenTyyppi o1 : o
														.getOsallistumiset()) {
													if (Osallistuminen.OSALLISTUU.equals(o1
															.getOsallistuminen())) {
														return true;
													}
												}
												return false;
											}
										});
						exchange.setProperty(
								hakemusOids,
								Collections2
										.transform(
												filtered,
												new Function<HakemusOsallistuminenTyyppi, String>() {
													@Override
													public String apply(
															HakemusOsallistuminenTyyppi input) {
														return input
																.getHakemusOid();
													}
												}));
						LOG.info("Osallistumattomien pois filtterointi: {}/{}",
								filtered.size(), unfiltered.size());

					}
				})
				//
				.end();

		from(hakemuksilleKoekutsukirjeet)
		//
				.errorHandler(
				//
						deadLetterChannel(kirjeidenLuontiEpaonnistui())
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(security)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						// paivitetaan kokonaistoiden maara
						dokumenttiprosessi(exchange).setKokonaistyo(
								exchange.getProperty(hakemusOids,
										Collection.class).size() + 2); // +
																		// viestintapalvelu
																		// +
																		// dokumenttipalvelu
																		// == +2
					}
				})
				//
				.split(property(hakemusOids),
						new FlexibleAggregationStrategy<Hakemus>()
								.storeInBody().accumulateInCollection(
										ArrayList.class))
				//
				.shareUnitOfWork()
				//
				.parallelProcessing()
				//
				.stopOnException()
				//
				.process(security).to(hakemusOiditHakemuksiksi())
				//
				.end()
				//
				.to(koekutsukirjeetHakemuksista());

		from(koekutsukirjeetHakemuksista())
		//
				.process(security)
				//
				.bean(koekutsukirjeetKomponentti)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						DokumenttiProsessi prosessi = dokumenttiprosessi(exchange);
						InputStream pdf;
						try {
							// LOG.error(
							// "\r\n{}",
							// new GsonBuilder().setPrettyPrinting()
							// .create()
							// .toJson(koekutsukirjeet(exchange)));
							pdf = pipeInputStreams(viestintapalveluResource
									.haeKoekutsukirjeet(koekutsukirjeet(exchange)));
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();

						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Viestintäpalvelulta pdf:n haussa tapahtui virhe: {}",
									e.getMessage());
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VIESTINTAPALVELU,
											"Koekutsukirjeiden synkroninen haku",
											e.getMessage()));
							throw e;
						}
						try {
							String id = generateId();
							dokumenttiResource.tallenna(id,
									"koekutsukirje.pdf",
									defaultExpirationDate().getTime(),
									prosessi.getTags(), "application/pdf", pdf);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
							prosessi.setDokumenttiId(id);
						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Dokumenttipalvelulle tiedonsiirrossa tapahtui virhe: {}",
									e.getMessage());
							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.DOKUMENTTIPALVELU,
											"Dokumentin tallennus", e
													.getMessage()));
							throw e;
						}
					}
				});
		//

		from(hakemusOiditHakemuksiksi())
		//
				.errorHandler(
				//
						deadLetterChannel(kirjeidenLuontiEpaonnistui())
								//
								.maximumRedeliveries(0)
								//
								// .redeliveryDelay(300L)
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(security)
				//
				.choice()
				//
				.when(not(prosessiOnKeskeytetty()))
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						String oid = exchange.getIn().getBody(String.class);
						try {

							exchange.getOut().setBody(
									applicationResource
											.getApplicationByOid(oid));
							//
							// yksi tyo valmistui
							//
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Osallistujen hakeminen haku-app:lta epäonnistui: {}. applicationResource.getApplicationByOid({})",
									e.getMessage(), oid);
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.HAKU,
											"Yritettiin hakea hakemus oidilla (get application by oid)",
											e.getMessage(), Poikkeus
													.hakemusOid(oid)));
							throw e;
						}
					}
				})
				//
				.end();
	}

	private String koekutsukirjeetHakemuksista() {
		return "direct:koekutsukirjeet_hakemuksista";
	}

	private String hakemusOiditHakemuksiksi() {
		return "direct:koekutsukirjeet_hakemusoidit_hakemuksiksi";
	}

	private String kirjeidenLuontiEpaonnistui() {
		return "direct:koekutsukirjeet_epaonnistui";
	}

	@SuppressWarnings("unchecked")
	private Kirjeet<Koekutsukirje> koekutsukirjeet(Exchange exchange) {
		return exchange.getIn().getBody(Kirjeet.class);
	}

}
