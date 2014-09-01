package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import static org.apache.camel.ExchangePattern.InOnly;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.KoostepalveluRouteBuilder;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintalaskentaResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import fi.vm.sade.valinta.kooste.util.HakemusUtil;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHakukohde;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaValintaperusteetJaHakemukset;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
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
	private static final String ROUTE_ID = "valintalaskenta_kerralla";
	private static final Integer HAE_KAIKKI_VALINNANVAIHEET = new Integer(-1);
	private final LaskentaSeurantaResource seurantaResource;
	private final ValintaperusteetRestResource valintaperusteetRestResource;
	private final ValintalaskentaResource valintalaskentaResource;
	private final ApplicationResource applicationResource;
	private final String valintalaskentaKerralla;
	private final String valintalaskentaKerrallaValintaperusteet;
	private final String valintalaskentaKerrallaHakemukset;
	private final String valintalaskentaKerrallaLaskenta;

	@Autowired
	public ValintalaskentaKerrallaRouteImpl(
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA) String valintalaskentaKerralla,
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_VALINTAPERUSTEET) String valintalaskentaKerrallaValintaperusteet,
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_HAKEMUKSET) String valintalaskentaKerrallaHakemukset,
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_LASKENTA) String valintalaskentaKerrallaLaskenta,
			LaskentaSeurantaResource seurantaResource,
			ValintaperusteetRestResource valintaperusteetRestResource,
			ValintalaskentaResource valintalaskentaResource,
			ApplicationResource applicationResource) {
		this.applicationResource = applicationResource;
		this.seurantaResource = seurantaResource;
		this.valintalaskentaKerrallaValintaperusteet = valintalaskentaKerrallaValintaperusteet;
		this.valintalaskentaKerrallaHakemukset = valintalaskentaKerrallaHakemukset;
		this.valintalaskentaKerralla = valintalaskentaKerralla;
		this.valintalaskentaKerrallaLaskenta = valintalaskentaKerrallaLaskenta;
		this.valintaperusteetRestResource = valintaperusteetRestResource;
		this.valintalaskentaResource = valintalaskentaResource;
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
						exchange.getProperty(
								ValintalaskentaKerrallaRoute.LOPETUSEHTO,
								AtomicBoolean.class).set(true);
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
				.log(LoggingLevel.WARN, "Valintalaskenta tyo kaynnistetty")
				//
				// Odotetaan laskenta luokkaa
				//
				.convertBodyTo(LaskentaJaHaku.class)
				//
				// Haetaan hakukohteet prosessointiin
				//
				.split(Reititys
						.<LaskentaJaHaku, List<LaskentaJaHakukohde>> lauseke(laskentaJaHaku -> {
							return laskentaJaHaku
									.getHakukohdeOids()
									// after mask
									.stream()
									.map(oid -> new LaskentaJaHakukohde(
											laskentaJaHaku.getLaskenta(), oid))
									.collect(Collectors.toList());
						}))
				//
				.to(InOnly, valintalaskentaKerrallaValintaperusteet,
						valintalaskentaKerrallaHakemukset);
		/**
		 * Hakee hakemukset
		 */
		from(valintalaskentaKerrallaHakemukset)
				.errorHandler(deadLetterChannel())
				//
				.choice()
				//
				.when(Reititys.<LaskentaJaHakukohde> ehto(tyo -> {
					return tyo.isLuovutettu();
				}))
				//
				.process(
						Reititys.<LaskentaJaHakukohde, LaskentaJaValintaperusteetJaHakemukset> funktio(
						//
						(tyo -> {
							LOG.error(
									"Koska valintaperusteita ei saatu niin ei haeta hakemuksiakaan suotta hakukohteelle {}",
									tyo.getHakukohdeOid());
							return new LaskentaJaValintaperusteetJaHakemukset(
									tyo.getLaskenta(), tyo.getHakukohdeOid(),
									null, null);
						})))
				//
				.to(InOnly, AGGREGATOR)
				//
				.otherwise()
				//
				.throttle(1)
				.timePeriodMillis(50)
				//
				.process(
						Reititys.<LaskentaJaHakukohde, LaskentaJaValintaperusteetJaHakemukset> funktio(
								tyo -> {
									LOG.debug("Hakemukset hakukohteelle {}",
											tyo.getHakukohdeOid());
									List<Hakemus> hakemukset = applicationResource
											.getApplicationsByOid(
													tyo.getHakukohdeOid(),
													ApplicationResource.ACTIVE_AND_INCOMPLETE,
													ApplicationResource.MAX);

									final Map<String, ApplicationAdditionalDataDTO> appData = applicationResource
											.getApplicationAdditionalData(
													tyo.getLaskenta()
															.getHakuOid(),
													tyo.getHakukohdeOid())
											.parallelStream()
											.collect(
											//
													Collectors
															.toMap(ApplicationAdditionalDataDTO::getOid,
																	i -> i));
									hakemukset = hakemukset
											.parallelStream()
											.map(h -> {
												Map<String, String> addData = appData
														.get(h.getOid())
														.getAdditionalData();
												if (addData == null) {
													throw new RuntimeException(
															"Lisatietoja ei saatu hakemukselle "
																	+ h.getOid());
												}
												if (h.getAnswers() != null) {
													h.getAnswers()
															.setLisatiedot(
																	addData);
												} else {
													throw new RuntimeException(
															"Hakemuksella ("
																	+ h.getOid()
																	+ ") ei ollut vastaus (answers) tietuetta!");
												}
												return h;
											}).collect(Collectors.toList());
									return new LaskentaJaValintaperusteetJaHakemukset(
											tyo.getLaskenta(), tyo
													.getHakukohdeOid(), null,
											hakemukset);

								},
								((tyo, poikkeus) -> {
									tyo.luovuta();
									LOG.error(
											"Hakemuksia ei saatu hakukohteelle({}) haussa({}). {}\r\n{}",
											tyo.getHakukohdeOid(),
											tyo.getLaskenta().getHakuOid(),

											poikkeus.getMessage(), Arrays
													.toString(poikkeus
															.getStackTrace()));
									seurantaResource.lisaaIlmoitusHakukohteelle(
											tyo.getLaskenta().getHakuOid(), tyo
													.getHakukohdeOid(),
											new IlmoitusDto(
													IlmoitusTyyppi.VIRHE,
													poikkeus.getMessage()));
									return true; // poikkeus on kasitelty.
													// jatketaan prosessointia
								}),
								(tyo -> new LaskentaJaValintaperusteetJaHakemukset(
										tyo.getLaskenta(), tyo
												.getHakukohdeOid(), null, null))))
				//
				.to(InOnly, AGGREGATOR);// valintalaskentaKerrallaLaskenta);

		/**
		 * Hakee valintaperusteet
		 */
		from(valintalaskentaKerrallaValintaperusteet)
				.errorHandler(deadLetterChannel())
				//
				.choice()
				//
				.when(Reititys.<LaskentaJaHakukohde> ehto(tyo -> {
					return tyo.isLuovutettu();
				}))
				//
				.process(
						Reititys.<LaskentaJaHakukohde, LaskentaJaValintaperusteetJaHakemukset> funktio(
						//
						(tyo -> {
							LOG.error(
									"Koska hakemuksia ei saatu niin ei haeta valintaperusteitakaan suotta hakukohteelle {}",
									tyo.getHakukohdeOid());
							return new LaskentaJaValintaperusteetJaHakemukset(
									tyo.getLaskenta(), tyo.getHakukohdeOid(),
									null, null);
						})))
				//
				.to(InOnly, AGGREGATOR)
				//
				.otherwise()
				//
				.throttle(1)
				.timePeriodMillis(50)
				//
				.process(
						Reititys.<LaskentaJaHakukohde, LaskentaJaValintaperusteetJaHakemukset> funktio(
								tyo -> {
									LOG.debug(
											"Valintaperusteet hakukohteelle {}",
											tyo.getHakukohdeOid());
									Integer valinnanvaihe = tyo.getLaskenta()
											.getValinnanvaihe();
									if (HAE_KAIKKI_VALINNANVAIHEET
											.equals(valinnanvaihe)) {
										valinnanvaihe = null;
									}
									return new LaskentaJaValintaperusteetJaHakemukset(
											tyo.getLaskenta(), tyo
													.getHakukohdeOid(),
											valintaperusteetRestResource
													.haeValintaperusteet(tyo
															.getHakukohdeOid(),
															valinnanvaihe),
											null);
								},
								((tyo, poikkeus) -> {
									tyo.luovuta();
									LOG.error(
											"Valintaperusteita ei saatu hakukohteelle({}) haussa({}). {}\r\n{}",
											tyo.getHakukohdeOid(),
											tyo.getLaskenta().getHakuOid(),
											poikkeus.getMessage(), Arrays
													.toString(poikkeus
															.getStackTrace()));
									seurantaResource.lisaaIlmoitusHakukohteelle(
											tyo.getLaskenta().getHakuOid(), tyo
													.getHakukohdeOid(),
											new IlmoitusDto(
													IlmoitusTyyppi.VIRHE,
													poikkeus.getMessage()));
									return true; // poikkeus on kasitelty.
													// jatketaan prosessointia
								}),
								(tyo -> new LaskentaJaValintaperusteetJaHakemukset(
										tyo.getLaskenta(), tyo
												.getHakukohdeOid(), null, null))))
				//
				.to(InOnly, AGGREGATOR); // valintalaskentaKerrallaLaskenta);
		/**
		 * Aggregoi
		 */
		from(AGGREGATOR)
				.errorHandler(deadLetterChannel())
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
							return aggKey;
						}), new AggregationStrategy() {
							public Exchange aggregate(Exchange oldExchange,
									Exchange newExchange) {
								if (oldExchange == null) {
									return newExchange;
								} else {
									LaskentaJaValintaperusteetJaHakemukset tyo1 = oldExchange
											.getIn()
											.getBody(
													LaskentaJaValintaperusteetJaHakemukset.class);
									LaskentaJaValintaperusteetJaHakemukset tyo2 = newExchange
											.getIn()
											.getBody(
													LaskentaJaValintaperusteetJaHakemukset.class);
									oldExchange.getOut().setBody(
											tyo1.yhdista(tyo2));
									return oldExchange;
								}
							}
						})

				//
				.completionTimeout(TimeUnit.HOURS.toMillis(2L))
				.completionSize(2)
				//
				.to(InOnly, valintalaskentaKerrallaLaskenta);
		/**
		 * Vie laskentoihin
		 */
		from(valintalaskentaKerrallaLaskenta)
		//
				.errorHandler(deadLetterChannel())
				//
				.process(
						Reititys.<LaskentaJaValintaperusteetJaHakemukset> kuluttaja(
								tyo -> {
									if (!tyo.isYhdistetty()) {
										LOG.error("Aggregaattorissa oli tyo pitkaan jumissa.");
										return;
									}
									try {
										if (!tyo.isValmisLaskettavaksi()) {
											LOG.error(
													"Laskentaa ei voida tehda hakukohteelle {}. Hakemukset null=={} ja valintaperusteet null=={}",
													tyo.getHakukohdeOid(),
													null == tyo.getHakemukset(),
													null == tyo
															.getValintaperusteet());
											try {
												seurantaResource
														.merkkaaHakukohteenTila(
																tyo.getLaskenta()
																		.getUuid(),
																tyo.getHakukohdeOid(),
																HakukohdeTila.KESKEYTETTY);
											} catch (Exception e) {
												LOG.error(
														"Seurantapalvelu on alhaalla! {}\r\n{}",
														e.getMessage(),
														Arrays.toString(e
																.getStackTrace()));
											}
											return;
										} else {
											if (tyo.getHakemukset().isEmpty()) {
												if (tyo.getValintaperusteet()
														.isEmpty()) {
													LOG.warn(
															"Laskentaa ei tehda hakukohteelle {} koska ei ole hakemuksia eika valintaperusteita",
															tyo.getHakukohdeOid());
													try {
														seurantaResource
																.lisaaIlmoitusHakukohteelle(
																		tyo.getLaskenta()
																				.getHakuOid(),
																		tyo.getHakukohdeOid(),
																		new IlmoitusDto(
																				IlmoitusTyyppi.ILMOITUS,
																				"Laskentaa ei tehda hakukohteelle koska ei ole hakemuksia eika valintaperusteita"));
													} catch (Exception e) {// nice
																			// to
																			// have
																			// loggausta.
																			// jos
																			// ei
																			// toimi
																			// niin
																			// ei
																			// haittaa
													}
												} else {
													LOG.warn(
															"Laskentaa ei tehda hakukohteelle {} koska ei ole hakemuksia",
															tyo.getHakukohdeOid());
													try {
														seurantaResource
																.lisaaIlmoitusHakukohteelle(
																		tyo.getLaskenta()
																				.getHakuOid(),
																		tyo.getHakukohdeOid(),
																		new IlmoitusDto(
																				IlmoitusTyyppi.ILMOITUS,
																				"Laskentaa ei tehda hakukohteelle koska ei ole hakemuksia"));
													} catch (Exception e) {// nice
																			// to
																			// have
																			// loggausta.
																			// jos
																			// ei
																			// toimi
																			// niin
																			// ei
																			// haittaa
													}
												}
												try {
													seurantaResource
															.merkkaaHakukohteenTila(
																	tyo.getLaskenta()
																			.getUuid(),
																	tyo.getHakukohdeOid(),
																	HakukohdeTila.VALMIS);
												} catch (Exception e) {
													LOG.error(
															"Seurantapalvelu on alhaalla! {}\r\n{}",
															e.getMessage(),
															Arrays.toString(e
																	.getStackTrace()));
												}
												return;
											} else if (tyo
													.getValintaperusteet()
													.isEmpty()) {
												LOG.warn(
														"Laskentaa ei tehda hakukohteelle {} koska ei ole valintaperusteita",
														tyo.getHakukohdeOid());
												try {
													seurantaResource
															.lisaaIlmoitusHakukohteelle(
																	tyo.getLaskenta()
																			.getHakuOid(),
																	tyo.getHakukohdeOid(),
																	new IlmoitusDto(
																			IlmoitusTyyppi.ILMOITUS,
																			"Laskentaa ei tehda hakukohteelle koska ei ole valintaperusteita"));
												} catch (Exception e) {// nice
																		// to
																		// have
																		// loggausta.
																		// jos
																		// ei
																		// toimi
																		// niin
																		// ei
																		// haittaa
												}
												try {
													seurantaResource
															.merkkaaHakukohteenTila(
																	tyo.getLaskenta()
																			.getUuid(),
																	tyo.getHakukohdeOid(),
																	HakukohdeTila.VALMIS);
												} catch (Exception e) {
													LOG.error(
															"Seurantapalvelu on alhaalla! {}\r\n{}",
															e.getMessage(),
															Arrays.toString(e
																	.getStackTrace()));
												}
												return;
											}
											LOG.debug(
													"Laskenta hakukohteelle {}. Valmis laskettavaksi == {}",
													tyo.getHakukohdeOid(),
													tyo.isValmisLaskettavaksi());
											//
											// VALINTAKOELASKENTA
											//
											if (Boolean.TRUE.equals(tyo
													.getLaskenta()
													.getValintakoelaskenta())) {
												valintalaskentaResource
														.valintakokeet(new LaskeDTO(
																tyo.getHakemukset()
																		// kaikki
																		// saikeet
																		// on
																		// varmasti
																		// jo
																		// tyollistettyja
																		// parellelstream
																		// tod.nak
																		// hidastaa
																		// .parallelStream()
																		.parallelStream()
																		.map(h -> getContext()
																				.getTypeConverter()
																				.tryConvertTo(
																						HakemusDTO.class,
																						h))
																		.collect(
																				Collectors
																						.toList()),
																tyo.getValintaperusteet()));
											} else {
												//
												// VALINTALASKENTA KAIKELLA
												//
												if (null == tyo.getLaskenta()
														.getValinnanvaihe()) {
													valintalaskentaResource
															.laskeKaikki(new LaskeDTO(
																	tyo.getHakemukset()
																			// kaikki
																			// saikeet
																			// on
																			// varmasti
																			// jo
																			// tyollistettyja
																			// parellelstream
																			// tod.nak
																			// hidastaa
																			// .parallelStream()
																			.parallelStream()

																			.map(h -> getContext()
																					.getTypeConverter()
																					.tryConvertTo(
																							HakemusDTO.class,
																							h))
																			.collect(
																					Collectors
																							.toList()),
																	tyo.getValintaperusteet()));
												} else {
													//
													// VALINTALASKENTA TIETYLLE
													// VALINNANVAIHEELLE
													//
													valintalaskentaResource
															.laske(new LaskeDTO(
																	tyo.getHakemukset()
																			.parallelStream()
																			// .parallelStream()
																			.map(h -> getContext()
																					.getTypeConverter()
																					.tryConvertTo(
																							HakemusDTO.class,
																							h))
																			.collect(
																					Collectors
																							.toList()),
																	tyo.getValintaperusteet()));
												}
											}
											try {
												seurantaResource
														.merkkaaHakukohteenTila(
																tyo.getLaskenta()
																		.getUuid(),
																tyo.getHakukohdeOid(),
																HakukohdeTila.VALMIS);
											} catch (Exception e) {
												LOG.error(
														"Seurantapalvelu on alhaalla! {}\r\n{}",
														e.getMessage(),
														Arrays.toString(e
																.getStackTrace()));
											}
										}
									} finally {
										LOG.info(
												"Valintalaskenta paatetty hakukohteelle {}",
												tyo.getHakukohdeOid());
										if (tyo.getLaskenta()
												.merkkaaHakukohdeTehdyksi()) {
											LOG.info(
													"Valintalaskenta paatetty haulle {}, uuid {}",
													tyo.getLaskenta()
															.getHakuOid(), tyo
															.getLaskenta()
															.getUuid());
											try {
												seurantaResource
														.merkkaaLaskennanTila(
																tyo.getLaskenta()
																		.getUuid(),
																LaskentaTila.VALMIS);
											} catch (Exception e) {
												LOG.error(
														"Seurantapalvelu on alhaalla! {}\r\n{}",
														e.getMessage(),
														Arrays.toString(e
																.getStackTrace()));
											}
										}
									}
								},
								((tyo, poikkeus) -> {
									LOG.error(
											"Laskentaa suoritettaessa hakukohteelle {} tapahtui poikkeus {}",
											tyo.getHakukohdeOid(),
											poikkeus.getMessage());
									try {
										seurantaResource.merkkaaHakukohteenTila(
												tyo.getLaskenta().getUuid(),
												tyo.getHakukohdeOid(),
												HakukohdeTila.KESKEYTETTY);
									} catch (Exception e) {
										LOG.error(
												"Seurantapalvelu on alhaalla! {}\r\n{}",
												e.getMessage(),
												Arrays.toString(e
														.getStackTrace()));
									}
									return true;
								})))
				//
				.stop();
	}

	@Override
	protected String deadLetterChannelEndpoint() {
		return DEADLETTERCHANNEL;
	}
}
