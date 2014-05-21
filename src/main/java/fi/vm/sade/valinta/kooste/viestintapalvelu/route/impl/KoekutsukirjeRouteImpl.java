package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import static org.apache.camel.builder.PredicateBuilder.not;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetValintakoeResource;
import fi.vm.sade.valinta.kooste.function.SuppeaHakemusFunction;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.function.ValintakoeOsallistuminenDTOFunction;
import fi.vm.sade.valinta.kooste.valintalaskenta.tulos.predicate.OsallistujatPredicate;
import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Koekutsukirje;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.OsoiteComparator;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.tulos.resource.ValintakoeResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class KoekutsukirjeRouteImpl extends AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(KoekutsukirjeRouteImpl.class);
	private final static int UUDELLEEN_YRITYSTEN_MAARA = 3;
	private final static long UUDELLEEN_YRITYSTEN_ODOTUSAIKA = 1500L;

	private final ViestintapalveluResource viestintapalveluResource;
	private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
	private final ValintakoeResource valintakoeResource;
	private final ValintaperusteetValintakoeResource valintaperusteetValintakoeResource;
	private final HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti;
	private final ApplicationResource applicationResource;
	private final DokumenttiResource dokumenttiResource;
	private final SecurityPreprocessor security = new SecurityPreprocessor();
	// private final ValintakoeResource valintakoeResource;
	private final String koekutsukirjeet;
	private final String hakemuksilleKoekutsukirjeet;

	@Autowired
	public KoekutsukirjeRouteImpl(
			@Value(KoekutsukirjeRoute.SEDA_KOEKUTSUKIRJEET) String koekutsukirjeet,
			@Value("direct:koekutsukirjeet_hakemuksilleKoekutsukirjeet") String hakemuksilleKoekutsukirjeet,
			DokumenttiResource dokumenttiResource,
			// ValintakoeResource valintakoeResource,
			ViestintapalveluResource viestintapalveluResource,
			KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
			ValintakoeResource valintakoeResource,
			ApplicationResource applicationResource,
			ValintaperusteetValintakoeResource valintaperusteetValintakoeResource,
			HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti) {
		super();
		this.haeHakukohteenHakemuksetKomponentti = haeHakukohteenHakemuksetKomponentti;
		this.valintaperusteetValintakoeResource = valintaperusteetValintakoeResource;
		this.koekutsukirjeet = koekutsukirjeet;
		this.hakemuksilleKoekutsukirjeet = hakemuksilleKoekutsukirjeet;
		// this.valintakoeResource = valintakoeResource;
		this.viestintapalveluResource = viestintapalveluResource;
		this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
		this.valintakoeResource = valintakoeResource;
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
				.when(property(OPH.HAKEMUSOIDS).isNull())
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
						"Koekutsukirjeiden luonti epaonnistui: ${property.CamelExceptionCaught}")
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						if (dokumenttiprosessi(exchange).getPoikkeukset()
								.isEmpty()) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.KOOSTEPALVELU,
											"Koekutsukirjeitä ei voitu muodostaa. Ota yhteys ylläpitoon."));
						}

					}
				})
				//
				.stop();

		//
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
				.log(LoggingLevel.INFO, "Haetaan koekutsukirjeitä varten oidit")
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						// VT-838
						Collection<String> valintalaskennastaHaettavatValintakokeet = Lists
								.newArrayList();
						boolean haetaanKaikkiHakukohteenHakijatValintakokeeseen = false;
						//
						// Haetaan valintaperusteista valintakokeet VT-838
						//
						try {
							for (String oid : valintakoeOids(exchange)) {
								ValintakoeDTO valintakoeDTO = valintaperusteetValintakoeResource
										.readByOid(oid);
								if (valintakoeDTO == null) {
									LOG.error(
											"Null valintakoe osoitteesta /valintaperusteet-service/resources/valintakoe/{}",
											oid);
									throw new RuntimeException(
											"Valintaperusteet palautti null valintakokeen!");
								}
								if (Boolean.TRUE.equals(valintakoeDTO
										.getKutsutaankoKaikki())) {

									haetaanKaikkiHakukohteenHakijatValintakokeeseen = true;
								} else {
									valintalaskennastaHaettavatValintakokeet
											.add(oid);
								}

							}
						} catch (Exception e) {
							LOG.error(
									"Valintakokeen tietojen haku hakukohteelle({}) epäonnistui:{}",
									hakukohdeOid(exchange), e.getMessage());
							Collection<Oid> oidit = Lists.newArrayList();
							oidit.add(Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VALINTAPERUSTEET,
											"Valintakokeen tietoja ei saatu haettua valintaperusteet palvelulta",
											e.getMessage(), oidit));
							throw e;
						}
						// järkevyystarkistus
						Set<String> hakemusOids = Sets.newHashSet();
						try {
							if (haetaanKaikkiHakukohteenHakijatValintakokeeseen) {
								hakemusOids
										.addAll(FluentIterable
												.from(haeHakukohteenHakemuksetKomponentti
														.haeHakukohteenHakemukset(hakukohdeOid(exchange)))
												.transform(
														SuppeaHakemusFunction.TO_HAKEMUS_OIDS)
												.toSet());
							}
						} catch (Exception e) {
							LOG.error(
									"Hakukohteelle({}) hakijoiden haku epäonnistui: {}",
									hakukohdeOid(exchange), e.getMessage());

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.HAKU,
											"Valintakokeen tietoja ei saatu haettua valintaperusteet palvelulta",
											e.getMessage(),
											Poikkeus.hakukohdeOid(hakukohdeOid(exchange))));
							throw e;
						}
						if (!valintalaskennastaHaettavatValintakokeet.isEmpty()) {
							try {
								List<ValintakoeOsallistuminenDTO> unfiltered = valintakoeResource
										.hakuByHakutoive(hakukohdeOid(exchange));

								Collection<ValintakoeOsallistuminenDTO> filtered = Collections2
										.filter(unfiltered,
												OsallistujatPredicate
														.vainOsallistujat(
																hakukohdeOid(exchange),
																valintalaskennastaHaettavatValintakokeet));
								hakemusOids.addAll(Collections2
										.transform(
												filtered,
												ValintakoeOsallistuminenDTOFunction.TO_HAKEMUS_OIDS));

								LOG.info(
										"Osallistumattomien pois filtterointi: {}/{}",
										filtered.size(), unfiltered.size());
							} catch (Exception e) {
								e.printStackTrace();
								LOG.error(
										"Valintatietojen haku hakukohteelle({}) epäonnistui:{}",
										hakukohdeOid(exchange), e.getMessage());
								Collection<Oid> oidit = Lists.newArrayList(Poikkeus
										.valintakoeOids(valintakoeOids(exchange)));
								oidit.add(Poikkeus
										.hakukohdeOid(hakukohdeOid(exchange)));

								dokumenttiprosessi(exchange).getPoikkeukset()
										.add(new Poikkeus(
												Poikkeus.VALINTATIETO,
												"Valintatiedot hakukohteelle",
												e.getMessage(), oidit));
								throw e;
							}
						}
						if (hakemusOids.isEmpty()) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus("Ei koekutsuttavia",
											"Koekutsuja ei voida muodostaa ilman osallistujia"));
							throw new RuntimeException(
									"Koekutsuja ei voida muodostaa ilman valintakoetta johon osallistuu edes joku.");
						}
						exchange.setProperty(OPH.HAKEMUSOIDS, hakemusOids);
					}
				})
				//
				.log(LoggingLevel.ERROR,
						"Oidit haettu ${property.hakemusOids.size()}")

				// .bean(valintatietoHakukohteelleKomponentti)
				//
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
								exchange.getProperty(OPH.HAKEMUSOIDS,
										Collection.class).size() + 2); // +
																		// viestintapalvelu
																		// +
																		// dokumenttipalvelu
																		// == +2
					}
				})
				//
				.split(property(OPH.HAKEMUSOIDS),
						new FlexibleAggregationStrategy<Hakemus>()
								.storeInBody().accumulateInCollection(
										ArrayList.class))
				//
				.shareUnitOfWork()
				//
				// .parallelProcessing()
				//
				.stopOnException()
				//
				.process(security).to(hakemusOiditHakemuksiksi())
				//
				.end()
				//
				.to(koekutsukirjeetHakemuksista());
		//
		// Haku-app kutsu täällä
		//
		from(hakemusOiditHakemuksiksi())
		//
				.errorHandler(
				//
						deadLetterChannel(kirjeidenLuontiEpaonnistui())
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
				.process(security)
				//
				.choice()
				//
				.when(not(prosessiOnKeskeytetty()))
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						if (exchange.getIn().getBody() == null) {
							LOG.error("Hakemusta yritettiin hakea null oidilla.");
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.HAKU,
											"Yritettiin hakea hakemus oidilla (get application by oid)",
											"", Poikkeus.hakemusOid(null)));
							throw new RuntimeException(
									"Hakemusta yritettiin hakea null oidilla");
						}

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
							Kirjeet<Koekutsukirje> k = koekutsukirjeet(exchange);
							Collections.sort(k.getLetters(),
									new Comparator<Koekutsukirje>() {
										@Override
										public int compare(Koekutsukirje o1,
												Koekutsukirje o2) {
											try {
												return OsoiteComparator.ASCENDING.compare(
														o1.getAddressLabel(),
														o2.getAddressLabel());
											} catch (Exception e) {
												LOG.error(
														"Koekutsukirjeellä ei ole osoitetta! {} {}",
														o1, o2);
												return 0;
											}
										}
									});
							// LOG.error(
							// "\r\n{}",
							// new GsonBuilder().setPrettyPrinting()
							// .create()
							// .toJson(koekutsukirjeet(exchange)));
							pdf = pipeInputStreams(viestintapalveluResource
									.haeKoekutsukirjeet(k));
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
