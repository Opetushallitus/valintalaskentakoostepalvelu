package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;

import static org.apache.camel.ExchangePattern.InOnly;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy.CompletionAwareMixin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.KoostepalveluRouteBuilder;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintalaskentaResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHakukohde;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaValintaperusteetJaHakemukset;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.HakukohdeTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.resource.SeurantaResource;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class ValintalaskentaKerrallaRouteImpl extends KoostepalveluRouteBuilder
		implements ValintalaskentaKerrallaRouteValvomo {

	private final static Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaKerrallaRouteImpl.class);
	private static final String DEADLETTERCHANNEL = "direct:valintalaskenta_kerralla_deadletterchannel";
	private static final String AGGREGATOR = "direct:valintalaskenta_kerralla_aggregator";
	private static final String ROUTE_ID = "valintalaskenta_kerralla";
	private final SeurantaResource seurantaResource;
	private final ValintaperusteetRestResource valintaperusteetRestResource;
	private final ValintalaskentaResource valintalaskentaResource;
	private final ApplicationResource applicationResource;
	private final String valintalaskentaKerralla;
	private final String valintalaskentaKerrallaValintaperusteet;
	private final String valintalaskentaKerrallaHakemukset;
	private final String valintalaskentaKerrallaLaskenta;
	private final Cache<String, Laskenta> laskentaCache = CacheBuilder
			.newBuilder().weakValues().expireAfterWrite(3, TimeUnit.HOURS)
			.removalListener(new RemovalListener<String, Laskenta>() {
				public void onRemoval(
						RemovalNotification<String, Laskenta> notification) {
					LOG.info("{} siivottu pois muistista",
							notification.getValue());
				}
			}).build();

	@Autowired
	public ValintalaskentaKerrallaRouteImpl(
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA) String valintalaskentaKerralla,
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_VALINTAPERUSTEET) String valintalaskentaKerrallaValintaperusteet,
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_HAKEMUKSET) String valintalaskentaKerrallaHakemukset,
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA_LASKENTA) String valintalaskentaKerrallaLaskenta,
			SeurantaResource seurantaResource,
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
		return laskentaCache.asMap().values().stream()
				.filter(l -> !l.isValmis()).collect(Collectors.toList());
	}

	@Override
	public Laskenta haeLaskenta(String uuid) {
		Laskenta l = laskentaCache.getIfPresent(uuid);
		if (l != null && l.isValmis()) { // ei palauteta valmistuneita
			return null;
		}
		return l;
	}

	@Override
	public void configure() throws Exception {
		interceptFrom(valintalaskentaKerralla).process(
				Reititys.<LaskentaJaHaku> kuluttaja(l -> {
					Laskenta vanhaLaskenta = laskentaCache.getIfPresent(l
							.getLaskenta().getUuid());
					if (vanhaLaskenta != null) {
						// varmistetaan etta uudelleen ajon reunatapauksessa
						// mahdollisesti viela suorituksessa oleva vanha
						// laskenta
						// lakkaa kayttamasta resursseja ja siivoutuu ajallaan
						// pois
						vanhaLaskenta.getLopetusehto().set(true);
					}
					laskentaCache.put(l.getLaskenta().getUuid(),
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
									return new LaskentaJaValintaperusteetJaHakemukset(
											tyo.getLaskenta(), tyo
													.getHakukohdeOid(), null,
											hakemukset);

								},
								((tyo, poikkeus) -> {
									LOG.error(
											"Hakemuksia ei saatu hakukohteelle({}) haussa({}). {}\r\n{}",
											tyo.getHakukohdeOid(),
											tyo.getLaskenta().getHakuOid(),
											poikkeus.getMessage(), Arrays
													.toString(poikkeus
															.getStackTrace()));
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
				.process(
						Reititys.<LaskentaJaHakukohde, LaskentaJaValintaperusteetJaHakemukset> funktio(
								tyo -> {
									LOG.debug(
											"Valintaperusteet hakukohteelle {}",
											tyo.getHakukohdeOid());
									return new LaskentaJaValintaperusteetJaHakemukset(
											tyo.getLaskenta(), tyo
													.getHakukohdeOid(),
											valintaperusteetRestResource
													.haeValintaperusteet(tyo
															.getHakukohdeOid(),
															null), null);
								},
								((tyo, poikkeus) -> {
									LOG.error(
											"Valintaperusteita ei saatu hakukohteelle({}) haussa({}). {}\r\n{}",
											tyo.getHakukohdeOid(),
											tyo.getLaskenta().getHakuOid(),
											poikkeus.getMessage(), Arrays
													.toString(poikkeus
															.getStackTrace()));
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
												} else {
													LOG.warn(
															"Laskentaa ei tehda hakukohteelle {} koska ei ole hakemuksia",
															tyo.getHakukohdeOid());
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
											valintalaskentaResource
													.laskeKaikki(new LaskeDTO(
															tyo.getHakemukset()
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

	private LaskentaJaValintaperusteetJaHakemukset tyotAsTyo(
			List<LaskentaJaValintaperusteetJaHakemukset> tyot) {
		LaskentaJaValintaperusteetJaHakemukset tyo;
		if (tyot.size() == 2) {
			tyo = tyot.get(0).yhdista(tyot.get(1));
		} else if (tyot.size() == 1) {
			tyo = tyot.get(0);
		} else {
			tyo = null;
			LOG.error("Miten on mahdollista!");
		}
		return tyo;
	}

	@Override
	protected String deadLetterChannelEndpoint() {
		return DEADLETTERCHANNEL;
	}
}
