package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.util.ObjectHelper.notNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;

import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.KoostepalveluRouteBuilder;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintalaskentaResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import fi.vm.sade.valinta.kooste.util.HakemusUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHakukohde;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaValintaperusteetJaHakemukset;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import static fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute.LOPETUSEHTO;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusTyyppi;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.resource.LaskentaSeurantaResource;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class ValintalaskentaKerrallaRouteImpl extends
		KoostepalveluRouteBuilder<Laskenta> implements
		ValintalaskentaKerrallaRouteValvomo {
	//
	private final static Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaKerrallaRouteImpl.class);
	private static final String DEADLETTERCHANNEL = "direct:valintalaskenta_kerralla_deadletterchannel";
	private static final String AGGREGATOR = "direct:valintalaskenta_kerralla_aggregator";
	private static final String ROUTE_ID = "VALINTALASKENTA";
	private static final String ROUTE_ID_AGGREGOINTI = "VALINTALASKENTA_AGGREGOINTI";
	private static final String ROUTE_ID_HAKEMUKSET = "VALINTALASKENTA_HAKEMUKSET";
	private static final String ROUTE_ID_LISATIEDOT = "VALINTALASKENTA_LISATIEDOT";
	private static final String ROUTE_ID_VALINTAPERUSTEET = "VALINTALASKENTA_VALINTAPERUSTEET";
	private static final String ROUTE_ID_LASKENTA = "VALINTALASKENTA_LASKENTA";
	private static final String ROUTE_ID_VALINTARYHMAT = "VALINTALASKENTA_VALINTARYHMAT";
	private static final Integer HAE_KAIKKI_VALINNANVAIHEET = new Integer(-1);

	private final LaskentaSeurantaAsyncResource seurantaAsyncResource;
	private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
	private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
	// private final ApplicationResource applicationResource;
	private final ApplicationAsyncResource applicationAsyncResource;
	private final String valintalaskentaKerralla = ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA;
	private final String valintalaskentaKerrallaValintaperusteet = ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_VALINTAPERUSTEET;
	private final String valintalaskentaKerrallaHakemukset = ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_HAKEMUKSET;
	private final String valintalaskentaKerrallaLisatiedot = ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_LISATIEDOT;
	private final String valintalaskentaKerrallaLaskenta = ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_LASKENTA;
	private final String valintalaskentaKerrallaHakijaryhmat = ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_VALINTARYHMAT;
	private ProducerTemplate producerTemplate;

	@Autowired
	public ValintalaskentaKerrallaRouteImpl(
			LaskentaSeurantaAsyncResource seurantaAsyncResource,
			ValintaperusteetAsyncResource valintaperusteetAsyncResource,
			ValintalaskentaAsyncResource valintalaskentaAsyncResource,
			ApplicationAsyncResource applicationAsyncResource) {
		super();
		this.applicationAsyncResource = applicationAsyncResource;
		this.seurantaAsyncResource = seurantaAsyncResource;
		this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
		this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;

	}

	@Override
	public List<Laskenta> ajossaOlevatLaskennat() {
		return getKoostepalveluCache().asMap().values().stream()
				.filter(l -> !l.isValmis()).collect(Collectors.toList());
	}

	@Override
	public Laskenta haeLaskenta(String uuid) {
		Laskenta l = getKoostepalveluCache().getIfPresent(uuid);
		if (l != null && l.isValmis()) { // ei palauteta valmistuneita
			return null;
		}
		return l;
	}

	@Override
	public void configure() throws Exception {
		interceptFrom(valintalaskentaKerralla).process(
				Reititys.<LaskentaJaHaku> kuluttaja(l -> {
					Laskenta vanhaLaskenta = getKoostepalveluCache()
							.getIfPresent(l.getLaskenta().getUuid());
					if (vanhaLaskenta != null) {
						// varmistetaan etta uudelleen ajon reunatapauksessa
						// mahdollisesti viela suorituksessa oleva vanha
						// laskenta
						// lakkaa kayttamasta resursseja ja siivoutuu ajallaan
						// pois
						vanhaLaskenta.getLopetusehto().set(true);
					}
					getKoostepalveluCache().put(l.getLaskenta().getUuid(),
							l.getLaskenta());
				}));
		//
		intercept().when(simple("${property.lopetusehto?.get()}")).stop();

		from(DEADLETTERCHANNEL)
		//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						LOG.error(
								"Valintalaskenta paattyi virheeseen\r\n{}",
								simple("${exception.message}").evaluate(
										exchange, String.class));
						// seurantaAsyncResource.merkkaaLaskennanTila(uuid,
						// tila);
						exchange.getProperty(LOPETUSEHTO, AtomicBoolean.class)
								.set(true);
					}
				})
				//
				.stop();

		/**
		 * Hakee hakukohteet haulle
		 */
		from(valintalaskentaKerralla)
		//
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID)
				//
				.threads(1)
				//
				.log(LoggingLevel.WARN, "Valintalaskenta tyo kaynnistetty")
				//
				// Odotetaan laskenta luokkaa
				//
				.convertBodyTo(LaskentaJaHaku.class)
				//
				// Haetaan hakukohteet prosessointiin
				//
				.process(
						Reititys.<LaskentaJaHaku> kuluttaja(laskentaJaHaku -> {
							laskentaJaHaku
									.getHakukohdeOids()
									.forEach(
											hakukohdeOid -> {
												try {
													LaskentaJaHakukohde tyo = new LaskentaJaHakukohde(
															laskentaJaHaku
																	.getLaskenta(),
															hakukohdeOid);
													producerTemplate
															.sendBodyAndProperty(
																	valintalaskentaKerrallaHakemukset,
																	//
																	ExchangePattern.InOnly,
																	tyo,
																	//
																	LOPETUSEHTO,
																	laskentaJaHaku
																			.getLaskenta()
																			.getLopetusehto());
													producerTemplate
															.sendBodyAndProperty(
																	valintalaskentaKerrallaValintaperusteet,
																	//
																	ExchangePattern.InOnly,
																	tyo,
																	//
																	LOPETUSEHTO,
																	laskentaJaHaku
																			.getLaskenta()
																			.getLopetusehto());
													producerTemplate

															.sendBodyAndProperty(
																	valintalaskentaKerrallaLisatiedot,
																	//
																	ExchangePattern.InOnly,
																	tyo,
																	//
																	LOPETUSEHTO,
																	laskentaJaHaku
																			.getLaskenta()
																			.getLopetusehto());
													producerTemplate

															.sendBodyAndProperty(
																	valintalaskentaKerrallaHakijaryhmat,
																	//
																	ExchangePattern.InOnly,
																	tyo,
																	//
																	LOPETUSEHTO,
																	laskentaJaHaku
																			.getLaskenta()
																			.getLopetusehto());
												} catch (Exception e) {
													LOG.error(
															"{} {}",
															valintalaskentaKerrallaHakemukset,
															e.getMessage());
												}
											});
						}));
		/**
		 * Lisatiedot hakemukselle
		 */
		from(valintalaskentaKerrallaLisatiedot)
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID_LISATIEDOT)
				//
				.throttle(3L)
				//
				.asyncDelayed()
				.timePeriodMillis(150)
				//
				.filter(filtteroiValmistuneetTaiOhitetutTyotPoisLisatietojenHausta())
				//
				.process(
						Reititys.<LaskentaJaHakukohde> kuluttaja(tyo -> {
							// case 0 hakemusta
							// final Map<String,
							// ApplicationAdditionalDataDTO>
							// appData
							applicationAsyncResource.getApplicationAdditionalData(
									tyo.getLaskenta().getHakuOid(),
									tyo.getHakukohdeOid(),
									additionalData -> {
										lahetaLaskentaan(
												tyo,
												new LaskentaJaValintaperusteetJaHakemukset(
														tyo.getLaskenta(),
														tyo.getHakukohdeOid(),
														null, null,
														additionalData, null));
									},
									poikkeus -> {
										LOG.error(
												"Lisatietojen haussa tapahtui virhe: {}",
												poikkeus.getMessage());
										lahetaLaskentaanLuovutus(
												tyo,
												"koska poikkeus "
														+ poikkeus.getMessage());
										seurantaAsyncResource.lisaaIlmoitusHakukohteelle(
												tyo.getLaskenta().getHakuOid(),
												tyo.getHakukohdeOid(),
												new IlmoitusDto(
														IlmoitusTyyppi.VIRHE,
														poikkeus.getMessage()));
									});
						}));

		/**
		 * Hakijaryhmat
		 */
		from(valintalaskentaKerrallaHakijaryhmat)
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID_VALINTARYHMAT)
				//
				.throttle(3L)
				//
				.asyncDelayed()
				.timePeriodMillis(150)
				//
				.filter(filtteroiValmistuneetTaiOhitetutTyotPoisValintaryhmienHausta())
				//
				.process(
						Reititys.<LaskentaJaHakukohde> kuluttaja(tyo -> {
							// ei haeta hakijaryhmia valintakoelaskennalle
							if (Boolean.TRUE.equals(tyo.getLaskenta()
									.getValintakoelaskenta())) {
								lahetaLaskentaan(
										tyo,
										new LaskentaJaValintaperusteetJaHakemukset(
												tyo.getLaskenta(), tyo
														.getHakukohdeOid(),
												null, null, null, Collections
														.emptyList()));
							} else {

								valintaperusteetAsyncResource.haeHakijaryhmat(
										tyo.getHakukohdeOid(),
										hakijaryhmat -> {
											if (hakijaryhmat == null) { // voiko
																		// tulla
																		// nullina.
																		// jos
																		// tulee
																		// niin
																		// laitetaan
																		// tyhjaksi
												hakijaryhmat = Collections
														.emptyList();
											}
											lahetaLaskentaan(
													tyo,
													new LaskentaJaValintaperusteetJaHakemukset(
															tyo.getLaskenta(),
															tyo.getHakukohdeOid(),
															null, null, null,
															hakijaryhmat));
										},
										poikkeus -> {
											LOG.error(
													"Valintaryhmien haussa tapahtui virhe: {}",
													poikkeus.getMessage());
											lahetaLaskentaanLuovutus(
													tyo,
													"koska poikkeus "
															+ poikkeus
																	.getMessage());
											seurantaAsyncResource
													.lisaaIlmoitusHakukohteelle(
															tyo.getLaskenta()
																	.getHakuOid(),
															tyo.getHakukohdeOid(),
															new IlmoitusDto(
																	IlmoitusTyyppi.VIRHE,
																	poikkeus.getMessage()));
										});
							}
						}));

		/**
		 * Hakemukset
		 */
		from(valintalaskentaKerrallaHakemukset)
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID_HAKEMUKSET)
				//
				.throttle(3L)
				//
				.asyncDelayed()
				.timePeriodMillis(150)
				//
				.filter(filtteroiValmistuneetTaiOhitetutTyotPoisHakemustenHausta())
				//
				.process(
						Reititys.<LaskentaJaHakukohde> kuluttaja(tyo -> {
							LOG.debug("Hakemukset hakukohteelle {}",
									tyo.getHakukohdeOid());
							applicationAsyncResource.getApplicationsByOid(
									tyo.getHakukohdeOid(),
									hakemukset -> {
										LOG.info(
												"Hakemukset haettu hakukohteelle({})",
												tyo.getHakukohdeOid());
										if (hakemukset.isEmpty()) {
											tyo.valmistui();
										}
										lahetaLaskentaan(
												tyo,
												new LaskentaJaValintaperusteetJaHakemukset(
														tyo.getLaskenta(),
														tyo.getHakukohdeOid(),
														null, hakemukset, null,
														null));
									},
									poikkeus -> {
										LOG.error(
												"Hakemuksia ei saatu hakukohteelle({}) haussa({}). {}",
												tyo.getHakukohdeOid(), tyo
														.getLaskenta()
														.getHakuOid(), poikkeus
														.getMessage());
										lahetaLaskentaanLuovutus(
												tyo,
												"koska poikkeus "
														+ poikkeus.getMessage());
										seurantaAsyncResource.lisaaIlmoitusHakukohteelle(
												tyo.getLaskenta().getHakuOid(),
												tyo.getHakukohdeOid(),
												new IlmoitusDto(
														IlmoitusTyyppi.VIRHE,
														poikkeus.getMessage()));
									});
						}));

		/**
		 * Hakee valintaperusteet
		 */
		from(valintalaskentaKerrallaValintaperusteet)
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID_VALINTAPERUSTEET)
				//
				.throttle(3L)
				//
				.asyncDelayed()
				.timePeriodMillis(150)
				//
				.filter(filtteroiValmistuneetTaiOhitetutTyotPoisValintaperusteidenHausta())
				//
				.process(
						Reititys.<LaskentaJaHakukohde> kuluttaja(tyo -> {
							LOG.debug("Valintaperusteet hakukohteelle {}",
									tyo.getHakukohdeOid());
							Integer valinnanvaihe = tyo.getLaskenta()
									.getValinnanvaihe();
							if (HAE_KAIKKI_VALINNANVAIHEET
									.equals(valinnanvaihe)) {
								valinnanvaihe = null;
							}
							valintaperusteetAsyncResource.haeValintaperusteet(
									tyo.getHakukohdeOid(),
									valinnanvaihe,
									valintaperusteet -> {
										if (valintaperusteet.isEmpty()) {
											tyo.valmistui(); // vihjataan
																// hakemustyojonoon
																// etta
																// hakemuksia
																// ei
																// tarvitse
																// hakea
																// jos
																// ne on
																// viela
																// hakematta
										}
										lahetaLaskentaan(
												tyo,
												new LaskentaJaValintaperusteetJaHakemukset(
														tyo.getLaskenta(),
														tyo.getHakukohdeOid(),
														valintaperusteet, null,
														null, null));
									},
									poikkeus -> {
										LOG.error(
												"Valintaperusteita ei saatu hakukohteelle({}) haussa({}). {}\r\n{}",
												tyo.getHakukohdeOid(), tyo
														.getLaskenta()
														.getHakuOid(), poikkeus
														.getMessage(),
												Arrays.toString(poikkeus
														.getStackTrace()));
										lahetaLaskentaanLuovutus(
												tyo,
												"koska poikkeus "
														+ poikkeus.getMessage());
										seurantaAsyncResource.lisaaIlmoitusHakukohteelle(
												tyo.getLaskenta().getHakuOid(),
												tyo.getHakukohdeOid(),
												new IlmoitusDto(
														IlmoitusTyyppi.VIRHE,
														poikkeus.getMessage()));
									});

						}));
		//
		/**
		 * Aggregoi
		 */
		from(AGGREGATOR)
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID_AGGREGOINTI)
				//
				.log(LoggingLevel.DEBUG, "Tyo aggregaattorilla")
				/**
				 * AGGREGOI HAKEMUKSET JA VALINTAPERUSTEET YHDEKSI
				 * LASKENTATYOKSI AVAIMELLA (hakukohdeOid,uuid(laskennantyoID))
				 * PURKAA PITKAAN TEKEMATTOMAT TYOT ENNEN PITKAA POIS
				 */
				.aggregate(
						Reititys.<LaskentaJaValintaperusteetJaHakemukset, String> lauseke(tyo -> {
							String aggKey = new StringBuilder()
									.append(tyo.getHakukohdeOid())
									.append(tyo.getLaskenta().getUuid())
									.toString();
							LOG.debug("Correlation key {}", aggKey);
							// LOG.info("{}: {}", aggKey, tyo.getLaskenta());
							return aggKey;
						}),
						//
						new FlexibleAggregationStrategy<LaskentaJaValintaperusteetJaHakemukset>()
								.storeInBody().accumulateInCollection(
										ArrayList.class))

				//
				.completionTimeout(TimeUnit.HOURS.toMillis(2L))
				.completionSize(4)
				//
				.to(InOnly, valintalaskentaKerrallaLaskenta);

		/**
		 * Vie laskentoihin
		 */
		from(valintalaskentaKerrallaLaskenta)
		//
				.errorHandler(deadLetterChannel())
				//
				.routeId(ROUTE_ID_LASKENTA)
				//
				.process(
						Reititys.<List<LaskentaJaValintaperusteetJaHakemukset>> kuluttaja(tyot -> {
							LaskentaWrapper laskenta = new LaskentaWrapper(tyot);
							Consumer<Object> laskennanViimeistelyEliTarkistusLoppuikoKokoLaskenta = (s -> {
								if (laskenta.getLaskenta()
										.merkkaaHakukohdeTehdyksi()) {
									LOG.info(
											"Valintalaskenta paatetty haulle {}, uuid {}",
											laskenta.getLaskenta().getHakuOid(),
											laskenta.getLaskenta().getUuid());
									seurantaAsyncResource.merkkaaLaskennanTila(
											laskenta.getLaskenta().getUuid(),
											LaskentaTila.VALMIS);
								}
								LOG.info(
										"Valintalaskenta paatetty hakukohteelle {} {}",
										laskenta.getHakukohdeOid(),
										laskenta.getLaskenta());
							});
							if (!laskenta.isOnkoToitaOikeaMaara()) {
								LOG.error("Aggregaattorissa oli tyo pitkaan jumissa.");
							} else if (laskenta
									.isOnkoLaskemattakinTehtyEliHakukohteelleEiOllutHakemuksiaTaiValintaperusteita()) {
								String selite = null;
								try {
									if (laskenta.getHakemukset().isEmpty()) {
										selite = "Hakukohteelle ei ollut hakemuksia joten laskentaa ei ole tarvetta suorittaa.";
									}
									if (laskenta.getValintaperusteet()
											.isEmpty()) {
										selite = "Hakukohteelle ei ollut valintaperusteita joten laskentaa ei ole tarvetta suorittaa.";
									}
								} catch (Exception e) {
									selite = e.getMessage();
								}
								LOG.info(
										"Tyo hakukohteelle({}) on laskemattakin tehty. {}",
										laskenta.getHakukohdeOid(), selite);
								laskennanViimeistelyEliTarkistusLoppuikoKokoLaskenta
										.accept(null);
								seurantaAsyncResource.merkkaaHakukohteenTila(
										laskenta.getLaskenta().getUuid(),
										laskenta.getHakukohdeOid(),
										HakukohdeTila.VALMIS);
								seurantaAsyncResource
										.lisaaIlmoitusHakukohteelle(
												laskenta.getLaskenta()
														.getHakuOid(),
												laskenta.getHakukohdeOid(),
												new IlmoitusDto(
														IlmoitusTyyppi.ILMOITUS,
														"Laskentaa ei tehda koska hakukohteeseen ei joko ole hakijoita tai valintaperusteita ei ole syotetty."));

							} else if (laskenta
									.isOnkoOhitettavaEliValintaperusteetTaiHakemuksetTaiLisatiedotPuuttui()) {
								LOG.info(
										"Tyo ohitetaan hakukohteelle({}) koska puuttui {} {} {} {}",
										laskenta.getHakukohdeOid(),
										laskenta.getValintaperusteet() == null ? "--\t\t"
												: "valintaperusteet",
										laskenta.getHakemukset() == null ? "--\t\t"
												: "hakemukset",
										laskenta.getLisatiedot() == null ? "--\t\t"
												: "lisatiedot",
										laskenta.getHakijaryhmat() == null ? "--\t\t"
												: "hakijaryhmat");

								laskennanViimeistelyEliTarkistusLoppuikoKokoLaskenta
										.accept(null);
								seurantaAsyncResource.merkkaaHakukohteenTila(
										laskenta.getLaskenta().getUuid(),
										laskenta.getHakukohdeOid(),
										HakukohdeTila.KESKEYTETTY);
								seurantaAsyncResource
										.lisaaIlmoitusHakukohteelle(
												laskenta.getLaskenta()
														.getHakuOid(),
												laskenta.getHakukohdeOid(),
												new IlmoitusDto(
														IlmoitusTyyppi.VIRHE,
														"Laskentaa ei tehda koska jonkin virheen vuoksi hakemuksia tai valintaperusteita ei saatu haettua."));
							} else {
								// tehdaan varsinainen laskenta
								//
								// VALINTAKOELASKENTA
								//

								Consumer<String> takaisinkutusLaskentapalvelunPaluuarvolleOK = (s -> {
									seurantaAsyncResource
											.merkkaaHakukohteenTila(laskenta
													.getLaskenta().getUuid(),
													laskenta.getHakukohdeOid(),
													HakukohdeTila.VALMIS);
									laskennanViimeistelyEliTarkistusLoppuikoKokoLaskenta
											.accept(s);
								});
								Consumer<Throwable> takaisinkutsuLaskentapalvelunEpaonnistumiselle = (p -> {
									laskennanViimeistelyEliTarkistusLoppuikoKokoLaskenta
											.accept(p);
									LOG.error(
											"Laskentaa suoritettaessa hakukohteelle {} tapahtui poikkeus {}",
											laskenta.getHakukohdeOid(),
											p.getMessage());
									seurantaAsyncResource
											.merkkaaHakukohteenTila(laskenta
													.getLaskenta().getUuid(),
													laskenta.getHakukohdeOid(),
													HakukohdeTila.KESKEYTETTY);

								});
								//
								// Async laskenta kutsut eri laskennan
								// variaatioille
								//
								if (Boolean.TRUE.equals(laskenta.getLaskenta()
										.getValintakoelaskenta())) {
									LOG.info(
											"Valintakoelaskenta hakukohteelle({})",
											laskenta.getHakukohdeOid());
									valintalaskentaAsyncResource.valintakokeet(
											new LaskeDTO(
													laskenta.getHakukohdeOid(),
													laskenta.convertHakemuksetToHakemuksetDTO(getContext()
															.getTypeConverter()),
													laskenta.getValintaperusteet(),
													laskenta.getHakijaryhmat()),
											takaisinkutusLaskentapalvelunPaluuarvolleOK,
											takaisinkutsuLaskentapalvelunEpaonnistumiselle);
								} else {
									//
									// VALINTALASKENTA KAIKELLA
									//
									if (null == laskenta.getLaskenta()
											.getValinnanvaihe()) {
										LOG.info(
												"Koko valintalaskenta hakukohteelle({})",
												laskenta.getHakukohdeOid());
										valintalaskentaAsyncResource.laskeKaikki(
												new LaskeDTO(
														laskenta.getHakukohdeOid(),
														laskenta.convertHakemuksetToHakemuksetDTO(getContext()
																.getTypeConverter()),
														laskenta.getValintaperusteet(),
														laskenta.getHakijaryhmat()),
												takaisinkutusLaskentapalvelunPaluuarvolleOK,
												takaisinkutsuLaskentapalvelunEpaonnistumiselle);
									} else {
										LOG.info(
												"Valintalaskenta valinnanvaiheelle hakukohteelle({})",
												laskenta.getHakukohdeOid());
										//
										// VALINTALASKENTA TIETYLLE
										// VALINNANVAIHEELLE
										//
										valintalaskentaAsyncResource.laske(
												new LaskeDTO(
														laskenta.getHakukohdeOid(),
														laskenta.convertHakemuksetToHakemuksetDTO(getContext()
																.getTypeConverter()),
														laskenta.getValintaperusteet(),
														laskenta.getHakijaryhmat()),
												takaisinkutusLaskentapalvelunPaluuarvolleOK,
												takaisinkutsuLaskentapalvelunEpaonnistumiselle);
									}
								}

							}
						}));
		//
		this.producerTemplate = getContext().createProducerTemplate();
	}

	@Override
	protected String deadLetterChannelEndpoint() {
		return DEADLETTERCHANNEL;
	}

	private Predicate filtteroiValmistuneetTaiOhitetutTyotPoisValintaperusteidenHausta() {
		return or(
		//
				eiLuovutettu(),
				//
				eiValmistunut(tyo -> new LaskentaJaValintaperusteetJaHakemukset(
						tyo.getLaskenta(), tyo.getHakukohdeOid(), Collections
								.emptyList(), null, null, null)));
	}

	private Predicate filtteroiValmistuneetTaiOhitetutTyotPoisHakemustenHausta() {
		return or(
		//
				eiLuovutettu(),
				//
				eiValmistunut(tyo -> new LaskentaJaValintaperusteetJaHakemukset(
						tyo.getLaskenta(), tyo.getHakukohdeOid(), null,
						Collections.emptyList(), null, null)));
	}

	private Predicate or(Predicate left, Predicate right) {
		return new Predicate() {
			@Override
			public boolean matches(Exchange exchange) {
				boolean isLeftOk = left.matches(exchange);
				if (isLeftOk) {
					return right.matches(exchange);
				}
				return isLeftOk;
			}
		};
	}

	private Predicate filtteroiValmistuneetTaiOhitetutTyotPoisLisatietojenHausta() {
		return or(
		//
				eiLuovutettu(),
				//
				eiValmistunut(tyo -> new LaskentaJaValintaperusteetJaHakemukset(
						tyo.getLaskenta(), tyo.getHakukohdeOid(), null, null,
						Collections.emptyList(), null)));
	}

	private Predicate filtteroiValmistuneetTaiOhitetutTyotPoisValintaryhmienHausta() {
		return or(
		//
				eiLuovutettu(),
				//
				eiValmistunut(tyo -> new LaskentaJaValintaperusteetJaHakemukset(
						tyo.getLaskenta(), tyo.getHakukohdeOid(), null, null,
						null, Collections.emptyList())));
	}

	private Predicate eiLuovutettu() {
		return Reititys.<LaskentaJaHakukohde> ehto(tyo -> {
			if (tyo.isLuovutettu()) {
				lahetaLaskentaanLuovutus(tyo, "koska merkitty luovutetuksi");
				return false;
			} else {
				return true;
			}
		});
	}

	private Predicate eiValmistunut(
			Function<LaskentaJaHakukohde, LaskentaJaValintaperusteetJaHakemukset> luoValmisTyo) {
		return Reititys.<LaskentaJaHakukohde> ehto(tyo -> {
			if (tyo.isValmistui()) {
				lahetaLaskentaan(tyo, luoValmisTyo.apply(tyo));
				return false;
			} else {
				return true;
			}
		});
	}

	private void lahetaLaskentaanLuovutus(LaskentaJaHakukohde tyo, String syy) {
		LOG.info("Luovutetaan hakukohteen {} kanssa: {}",
				tyo.getHakukohdeOid(), syy);
		tyo.luovuta();
		lahetaLaskentaan(tyo,
				new LaskentaJaValintaperusteetJaHakemukset(tyo.getLaskenta(),
						tyo.getHakukohdeOid(), null, null, null, null));
	}

	private void lahetaLaskentaan(LaskentaJaHakukohde tyo,
			LaskentaJaValintaperusteetJaHakemukset data) {
		LOG.info("Lahetetaan laskentaan hakukohde {}: {} {} {} {}", tyo
				.getHakukohdeOid(),
				data.getValintaperusteet() == null ? "--\t\t"
						: "valintaperusteet",
				data.getHakemukset() == null ? "--\t\t" : "hakemukset", data
						.getLisatiedot() == null ? "--\t\t" : "lisatiedot",
				data.getHakijaryhmat() == null ? "--\t\t" : "hakijaryhmat");
		producerTemplate.sendBodyAndProperty(AGGREGATOR,
		//
				ExchangePattern.InOnly,
				//
				data,
				//
				LOPETUSEHTO, tyo.getLaskenta().getLopetusehto());
	}
}
