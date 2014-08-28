package fi.vm.sade.valinta.kooste.kela.route.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.*;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.kela.dto.KelaAbstraktiHaku;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHakijaRivi;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHaku;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLisahaku;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.HaunTyyppiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.OppilaitosKomponentti;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKaikkiPaikanVastaanottaneet;
import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

/**
 * @author Jussi Jartamo
 * 
 *         Route to Kela.
 */
@Component
public class KelaRouteImpl extends AbstractDokumenttiRouteBuilder {

	private static final Logger LOG = LoggerFactory
			.getLogger(KelaRouteImpl.class);

	private final int MAKSIMI_MAARA_HAKEMUKSIA_KERRALLA_HAKEMUSPALVELULTA = 10000;

	private final KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti;
	private final KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti;
	private final SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet;
	private final DokumenttiResource dokumenttiResource;
	private final String kelaLuonti;
	private final HaunTyyppiKomponentti haunTyyppiKomponentti;
	private final ApplicationResource applicationResource;
	private final OppilaitosKomponentti oppilaitosKomponentti;
	private final fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource hakuResource;
	private final LinjakoodiKomponentti linjakoodiKomponentti;
	private final HakukohdeResource hakukohdeResource;
	private final KoodiService koodiService;

	@Autowired
	public KelaRouteImpl(
			@Value(KelaRoute.SEDA_KELA_LUONTI) String kelaLuonti,
			DokumenttiResource dokumenttiResource,
			KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti,
			KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti,
			SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet,
			fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource hakuResource,
			HaunTyyppiKomponentti haunTyyppiKomponentti,
			ApplicationResource applicationResource,
			OppilaitosKomponentti oppilaitosKomponentti,
			LinjakoodiKomponentti linjakoodiKomponentti,
			HakukohdeResource hakukohdeResource, KoodiService koodiService) {
		this.koodiService = koodiService;
		this.hakukohdeResource = hakukohdeResource;
		this.oppilaitosKomponentti = oppilaitosKomponentti;
		this.linjakoodiKomponentti = linjakoodiKomponentti;
		this.haunTyyppiKomponentti = haunTyyppiKomponentti;
		this.hakuResource = hakuResource;
		this.kelaLuonti = kelaLuonti;
		this.dokumenttiResource = dokumenttiResource;
		this.kelaHakijaKomponentti = kelaHakijaKomponentti;
		this.sijoitteluVastaanottaneet = sijoitteluVastaanottaneet;
		this.kelaDokumentinLuontiKomponentti = kelaDokumentinLuontiKomponentti;
		this.applicationResource = applicationResource;
	}

	/**
	 * Kela Camel Configuration: Siirto and document generation.
	 */
	public final void configure() {
		Endpoint haeHaku = endpoint("direct:kelaluonti_hae_haku");
		Endpoint valmistaHaku = endpoint("direct:kelaluonti_valmista_haku");
		Endpoint luoLisahaku = endpoint("direct:kelaluonti_luo_lisahaku");
		Endpoint luoHaku = endpoint("direct:kelaluonti_luo_haku");
		Endpoint vientiDokumenttipalveluun = endpoint("direct:kelaluonti_vienti_dokumenttipalveluun");
		/**
		 * Kela-dokkarin luonti reitti
		 */
		from(kelaLuonti)
		//
				.errorHandler(
						deadLetterChannel(kelaFailed())
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(true).logRetryStackTrace(true)
								.logHandled(true))
				//
				.setProperty("cache", constant(new KelaCache(koodiService)))
				//
				.process(SecurityPreprocessor.SECURITY)
				// haetaan kaikkia hakuOideja vastaavat haut tarjonnasta
				.split(body())
				//
				.shareUnitOfWork()
				//
				.parallelProcessing()
				//
				.stopOnException()
				//
				.to(haeHaku).to(valmistaHaku)
				//
				.end()
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						// valmistetaan hakemusoidit silmukkaa varten
						Collection<String> hakemusOidit = Sets.newHashSet();
						for (KelaAbstraktiHaku kelahaku : cache(exchange)
								.getKelaHaut()) {
							hakemusOidit.addAll(kelahaku.getHakemusOids());
						}
						hakemusOidit = Lists.newArrayList(hakemusOidit); // muutetaan
						try {
							int n = 0;
							Collection<List<String>> oiditSivutettuna = Lists
									.newArrayList();
							do {
								List<String> osajoukkoOideista = FluentIterable
										.from(hakemusOidit)
										//
										.skip(n)
										.limit(MAKSIMI_MAARA_HAKEMUKSIA_KERRALLA_HAKEMUSPALVELULTA)
										//
										.toList();
								oiditSivutettuna.add(osajoukkoOideista);
								n += MAKSIMI_MAARA_HAKEMUKSIA_KERRALLA_HAKEMUSPALVELULTA;
							} while (n < hakemusOidit.size());
							List<Hakemus> hakemukset = Lists.newArrayList();
							LOG.warn("Haetaan {} hakemusta, {} erässä",
									hakemusOidit.size(),
									oiditSivutettuna.size());
							for (List<String> oidit : oiditSivutettuna) {
								try {
									List<Hakemus> h = applicationResource
											.getApplicationsByOids(oidit);
									hakemukset.addAll(h);
									LOG.warn(
											"Saatiin erä hakemuksia {}. {}/{}",
											h.size(), hakemukset.size(),
											hakemusOidit.size());
								} catch (Exception e) {
									LOG.error(
											"Hakemuspalvelu ei jaksa tarjoilla hakemuksia {}. Yritetään vielä uudestaan.",
											e.getMessage());
									// annetaan hakuapp:lle vahan aikaa toipua
									// ja yritetaan uudestaan
									Thread.sleep(250L);
									hakemukset.addAll(applicationResource
											.getApplicationsByOids(oidit));
								}
							}
							exchange.getOut().setBody(hakemukset);

						} catch (Exception e) {
							String virhe = "Ei saatu hakemuksia hakupalvelulta!";
							dokumenttiprosessi(exchange)
									.getPoikkeuksetUudelleenYrityksessa().add(
											new Poikkeus(Poikkeus.HAKU, virhe));
							throw new RuntimeException(virhe);
						}
					}
				})
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						@SuppressWarnings("unchecked")
						Collection<Hakemus> hakemukset = (Collection<Hakemus>) exchange
								.getIn().getBody();
						KelaCache cache = cache(exchange);
						for (Hakemus hakemus : hakemukset) {
							cache.put(hakemus);
						}
						// Filtteroidaan ylimaaraiset pois ja bodyyn joukko
						// valmistettavia kela riveja
						List<KelaHakijaRivi> rivit = Lists.newArrayList();
						HakukohdeSource hakukohdeSource = new HakukohdeSource() {
							Map<String, HakukohdeDTO> c = Maps.newHashMap();

							public HakukohdeDTO getHakukohdeByOid(String oid) {
								if (!c.containsKey(oid)) {
									try {
										c.put(oid,
												hakukohdeResource.getByOID(oid));
									} catch (Exception e) {
										LOG.error(
												"Ei saatu tarjonnalta hakukohdetta oidilla {} (/tarjonta-service/rest/hakukohde/{})\r\n{}",
												oid, e.getMessage());
										throw e;
									}
								}
								return c.get(oid);
							}
						};
						LinjakoodiSource linjakoodiSource = new LinjakoodiSource() {
							Map<String, String> c = Maps.newHashMap();

							public String getLinjakoodi(String uri) {
								if (!c.containsKey(uri)) {
									c.put(uri, linjakoodiKomponentti
											.haeLinjakoodi(uri));
								}
								return c.get(uri);
							}
						};
						OppilaitosSource oppilaitosSource = new OppilaitosSource() {
							Map<String, String> c = Maps.newHashMap();

							public String getOppilaitosKoodi(String tarjoajaOid) {
								if (!c.containsKey(tarjoajaOid)) {
									c.put(tarjoajaOid, oppilaitosKomponentti
											.haeOppilaitosKoodi(tarjoajaOid));
								}
								return c.get(tarjoajaOid);
							}
						};
						for (KelaAbstraktiHaku kelahaku : cache.getKelaHaut()) {
							rivit.addAll(kelahaku.createHakijaRivit(cache,
									hakukohdeSource, linjakoodiSource,
									oppilaitosSource));
						}
						if (rivit.isEmpty()) {
							String virhe = "Kela-dokumenttia ei voi luoda hauille joissa ei ole yhtään valittua hakijaa!";
							dokumenttiprosessi(exchange)
									.getPoikkeuksetUudelleenYrityksessa().add(
											new Poikkeus(
													Poikkeus.KOOSTEPALVELU,
													virhe));
							throw new RuntimeException(virhe);
						}
						dokumenttiprosessi(exchange).setKokonaistyo(
								rivit.size() + 1);
						exchange.getOut().setBody(rivit);
					}
				})
				//
				.split(body(), createAccumulatingAggregation())
				//
				// .to(valmistaKelaRivi)
				//
				.bean(kelaHakijaKomponentti)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
					}
				})
				//
				.end()
				//
				.bean(kelaDokumentinLuontiKomponentti)
				//
				.to(vientiDokumenttipalveluun);

		from(vientiDokumenttipalveluun)
		//

				.errorHandler(
						deadLetterChannel(kelaFailed())
								//
								.maximumRedeliveries(3)
								.redeliveryDelay(1500L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						try {
							InputStream filedata = exchange.getIn().getBody(
									InputStream.class);
							String id = generateId();
							Long expirationTime = defaultExpirationDate()
									.getTime();
							List<String> tags = dokumenttiprosessi(exchange)
									.getTags();
							dokumenttiResource.tallenna(id, KelaUtil
									.createTiedostoNimiYhva14(new Date()),
									expirationTime, tags,
									"application/octet-stream", filedata);
							dokumenttiprosessi(exchange).setDokumenttiId(id);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeuksetUudelleenYrityksessa()
									.add(new Poikkeus(
											Poikkeus.DOKUMENTTIPALVELU,
											"Kela-dokumentin tallennus dokumenttipalveluun epäonnistui"));
							throw e;
						}
					}
				});

		from(haeHaku)
		//
				.errorHandler(
						deadLetterChannel(kelaFailed())
								// .useOriginalMessage()
								//
								// (kelaFailed())
								//
								.maximumRedeliveries(3)
								.redeliveryDelay(1500L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.routeId("Haun haku reitti")
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						String hakuOid = exchange.getIn().getBody(String.class);
						HakuV1RDTO haku;
						try {
							haku = hakuResource.findByOid(hakuOid).getResult();
							exchange.getOut().setBody(haku);

							// cache(exchange).put(haku);
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeuksetUudelleenYrityksessa().add(
											new Poikkeus(Poikkeus.TARJONTA,
													"Haun haku oid:lla.",
													new Oid(hakuOid,
															Poikkeus.HAKUOID)));
							throw e;
						}
						try {
							cache(exchange).lukuvuosi(haku);
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeuksetUudelleenYrityksessa()
									.add(new Poikkeus(
											Poikkeus.KOODISTO,
											"Lukuvuoden haku haulle koodistosta URI:lla "
													+ haku.getKoulutuksenAlkamiskausiUri(),
											new Oid(hakuOid, Poikkeus.HAKUOID)));
							throw e;
						}

					}
				});

		from(valmistaHaku)
				.errorHandler(
						deadLetterChannel(kelaFailed())
								// .useOriginalMessage()
								//
								// (kelaFailed())
								//
								.maximumRedeliveries(3)
								.redeliveryDelay(1500L)
								// log exhausted stacktrace
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.routeId("Haun esitiedot keräävä reitti")
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						HakuV1RDTO haku = exchange.getIn().getBody(
								HakuV1RDTO.class);
						String hakutyyppiUri = haku.getHakutyyppiUri();
						try {
							if (cache(exchange).getHakutyyppi(hakutyyppiUri) == null) {
								cache(exchange).putHakutyyppi(
										hakutyyppiUri,
										haunTyyppiKomponentti
												.haunTyyppi(hakutyyppiUri));
							}
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeuksetUudelleenYrityksessa()
									.add(new Poikkeus(
											Poikkeus.KOODISTO,
											"Haun tyypille "
													+ hakutyyppiUri
													+ " ei saatu arvoa koodistosta",
											new Oid(hakutyyppiUri,
													Poikkeus.KOODISTOURI)));
							throw e;
						}
					}
				})
				//
				.choice()
				//
				.when(isLisahakuTyyppi())
				//
				.to(luoLisahaku)
				//
				.otherwise()
				//
				.to(luoHaku)
				//
				.end();

		from(luoLisahaku)
		//
				.errorHandler(deadLetterChannel(kelaFailed()))
				//
				.routeId("Lisähaun luova reitti")
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						HakuV1RDTO haku = exchange.getIn().getBody(
								HakuV1RDTO.class);
						if (haku == null) {
							throw new RuntimeException(
									"Reitillä oli null hakuDTO!");
						}
						String hakuOid = haku.getOid();
						// haetaan kaikki hakemukset lisahaulle koska ei voida
						// tietaa tarkastelematta ketka on valittuja.
						try {
							HakemusList hakemusList = applicationResource
									.findApplications(
											null,
											ApplicationResource.ACTIVE_AND_INCOMPLETE,
											null, null, haku.getOid(), null, 0,
											ApplicationResource.MAX);
							KelaLisahaku kelalisahaku = new KelaLisahaku(
									Collections2.transform(
											hakemusList.getResults(),
											new Function<SuppeaHakemus, String>() {
												@Override
												public String apply(
														SuppeaHakemus suppeaHakemus) {
													return suppeaHakemus
															.getOid();
												}
											}), haku, cache(exchange));
							cache(exchange).addKelaHaku(kelalisahaku);
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeuksetUudelleenYrityksessa()
									.add(new Poikkeus(
											Poikkeus.HAKU,
											"Hakemusten haku haulle epäonnistui",
											new Oid(haku.getOid(),
													Poikkeus.HAKUOID)));
							throw e;
						}
					}
				});
		from(luoHaku)
		//
				.errorHandler(deadLetterChannel(kelaFailed()))
				//
				.routeId("Haun luova reitti")
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						HakuV1RDTO haku = exchange.getIn().getBody(
								HakuV1RDTO.class);
						if (haku == null) {
							throw new RuntimeException(
									"Reitillä oli null hakuDTO!");
						}
						String hakuOid = haku.getOid();
						try {
							Collection<HakijaDTO> hakijat = sijoitteluVastaanottaneet
									.vastaanottaneet(hakuOid);
							KelaHaku kelahaku = new KelaHaku(hakijat, haku,
									cache(exchange));
							cache(exchange).addKelaHaku(kelahaku);
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeuksetUudelleenYrityksessa().add(
											new Poikkeus(Poikkeus.SIJOITTELU,
													"Vastaanottaneiden haku sijoittelusta epäonnistui haulle, koska: "
															+ e.getMessage(),
													new Oid(hakuOid,
															Poikkeus.HAKUOID)));
							throw e;
						}
					}
				});

		from(kelaFailed())
		//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String virhe = null;
						String stacktrace = null;
						try {
							virhe = simple("${exception.message}").evaluate(
									exchange, String.class);
							stacktrace = simple("${exception.stacktrace}")
									.evaluate(exchange, String.class);
						} catch (Exception e) {
						}
						LOG.error(
								"Keladokumentin luonti paattyi virheeseen! {}\r\n{}",
								virhe, stacktrace);
						dokumenttiprosessi(exchange).getPoikkeukset().add(
								new Poikkeus(Poikkeus.KOOSTEPALVELU,
										"Kela-dokumentin luonti", virhe));
						dokumenttiprosessi(exchange).addException(virhe);
						dokumenttiprosessi(exchange)
								.luovutaUudelleenYritystenKanssa();

					}
				})
				//
				.stop();
	}

	/**
	 * @return Arraylist aggregation strategy.
	 */
	private <T> AggregationStrategy createAccumulatingAggregation() {
		return new FlexibleAggregationStrategy<T>().storeInBody()
				.accumulateInCollection(ArrayList.class);
	}

	private KelaCache cache(Exchange exchange) {
		return exchange.getProperty("cache", KelaCache.class);
	}

	private Predicate isLisahakuTyyppi() {
		return new Predicate() {
			public boolean matches(Exchange exchange) {
				HakuV1RDTO haku = exchange.getIn().getBody(HakuV1RDTO.class);
				String hakutyypinArvo = cache(exchange).getHakutyyppi(
						haku.getHakutyyppiUri());
				// Koodistosta saa hakutyypille arvon ja nimen. Oletetaan etta
				// nimi voi vaihtua mutta koodi pysyy vakiona.
				return "03".equals(hakutyypinArvo);
			}
		};
	}

	/**
	 * @return direct:kela_siirto
	 */
	private String kelaFailed() {
		return KelaRoute.DIRECT_KELA_FAILED;
	}

}
