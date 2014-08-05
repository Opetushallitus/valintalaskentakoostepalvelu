package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.valinta.kooste.KoostepalveluRouteBuilder;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintalaskentaResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHakukohde;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaMaski;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaValintaperusteetJaHakemukset;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
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
public class ValintalaskentaKerrallaRouteImpl extends KoostepalveluRouteBuilder {

	private final static Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaKerrallaRouteImpl.class);
	private static final String DEADLETTERCHANNEL = "direct:valintalaskenta_kerralla_deadletterchannel";
	private static final String ROUTE_ID = "valintalaskenta_kerralla";
	private final SeurantaResource seurantaResource;
	private final ValintaperusteetResource valintaperusteetResource;
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
			SeurantaResource seurantaResource,
			ValintaperusteetResource valintaperusteetResource,
			ValintaperusteetRestResource valintaperusteetRestResource,
			ValintalaskentaResource valintalaskentaResource,
			ApplicationResource applicationResource) {
		this.applicationResource = applicationResource;
		this.seurantaResource = seurantaResource;
		this.valintalaskentaKerrallaValintaperusteet = valintalaskentaKerrallaValintaperusteet;
		this.valintalaskentaKerrallaHakemukset = valintalaskentaKerrallaHakemukset;
		this.valintalaskentaKerralla = valintalaskentaKerralla;
		this.valintalaskentaKerrallaLaskenta = valintalaskentaKerrallaLaskenta;
		this.valintaperusteetResource = valintaperusteetResource;
		this.valintaperusteetRestResource = valintaperusteetRestResource;
		this.valintalaskentaResource = valintalaskentaResource;
	}

	@Override
	public void configure() throws Exception {
		interceptFrom(valintalaskentaKerralla)
		//
				.setProperty(LOPETUSEHTO, constant(new AtomicBoolean(false)));
		intercept().when(simple("${property.lopetusehto?.get()}")).stop();

		from(DEADLETTERCHANNEL)

		.process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				LOG.error(
						"Valintalaskenta paattyi virheeseen\r\n{}",
						simple("${exception.message}").evaluate(exchange,
								String.class));
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
				.log(LoggingLevel.WARN, "Valintalaskenta tyo kaynnistetty")
				//
				// Odotetaan laskenta luokkaa
				//
				.convertBodyTo(LaskentaJaMaski.class)
				//
				// Haetaan hakukohteet prosessointiin
				//
				.split(Reititys.<LaskentaJaMaski, List<LaskentaJaHakukohde>> lauseke(
						laskentaJaMaski -> {
							Laskenta laskenta = laskentaJaMaski.getLaskenta();
							Maski maski = laskentaJaMaski.getMaski();
							Collection<String> oidit = maski.maskaa(valintaperusteetResource
									.haunHakukohteet(laskenta.getHakuOid())
									.stream()
									//
									.filter((h -> {
										boolean julkaistu = "JULKAISTU"
												.equals(h.getTila());
										if (!julkaistu) {
											LOG.warn(
													"Ohitetaan hakukohde {} koska sen tila on {}.",
													h.getOid(), h.getTila());
										}
										return julkaistu;
									})).map(u -> u.getOid())
									//
									.collect(Collectors.toSet()));
							if (oidit.size() == 0) {
								throw new RuntimeException(
										"Laskentaan tarvitaan vahintaan yksi hakukohde");
							}
							laskenta.setLaskettavienHakukohteidenMaara(oidit
									.size());
							return oidit
									// after mask
									.stream()
									.map(oid -> new LaskentaJaHakukohde(
											laskenta, oid))
									.collect(Collectors.toList());
						},
						//
						// Poikkeus hakukohteiden haussa
						//
						((i, e) -> LOG
								.error("Hakukohteita ei saatu haettua haulle({}). {}\r\n{}",
										i.getLaskenta().getHakuOid(),
										e.getMessage(),
										Arrays.toString(e.getStackTrace())))))
				//
				// .shareUnitOfWork()
				//
				// Suorita tyo
				//
				.to(valintalaskentaKerrallaValintaperusteet,
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
				.to(valintalaskentaKerrallaLaskenta);

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
				.to(valintalaskentaKerrallaLaskenta);

		from(valintalaskentaKerrallaLaskenta)
				.errorHandler(deadLetterChannel())
				/**
				 * AGGREGOI HAKEMUKSET JA VALINTAPERUSTEET YHDEKSI
				 * LASKENTATYOKSI AVAIMELLA (hakukohdeOid,uuid(laskennantyoID))
				 * PURKAA PITKAAN TEKEMATTOMAT TYOT POIS 10MIN TIMEOUTILLA
				 */
				.aggregate(
						Reititys.<LaskentaJaValintaperusteetJaHakemukset, String> lauseke(tyo -> new StringBuilder()
								.append(tyo.getHakukohdeOid())
								.append(tyo.getLaskenta().getUuid()).toString()),
						new AggregationStrategy() {
							public Exchange aggregate(Exchange oldExchange,
									Exchange newExchange) {
								LaskentaJaValintaperusteetJaHakemukset o1 = null;
								LaskentaJaValintaperusteetJaHakemukset o2 = null;
								if (oldExchange != null) {
									o1 = oldExchange
											.getIn()
											.getBody(
													LaskentaJaValintaperusteetJaHakemukset.class);
								}
								if (newExchange != null) {
									o2 = newExchange
											.getIn()
											.getBody(
													LaskentaJaValintaperusteetJaHakemukset.class);
								}
								if (o1 != null && o2 != null) {
									newExchange.getOut()
											.setBody(o1.yhdista(o2));
								}
								// LOG.error("o1 ({}) and o2 ({})", o1, o2);
								return newExchange;
							}
						})
				// Molemmat osatyot valmiina, eli hakemukset haettu ja
				// valintaperusteet
				.completionSize(2)
				// Purkaa valmistumattomat viimeistaan muutamien minuuttien
				// jalkeen
				.completionTimeout(TimeUnit.MINUTES.toMillis(5L))
				//
				.process(
						Reititys.<LaskentaJaValintaperusteetJaHakemukset> kuluttaja(
								tyo -> {
									try {
										if (!tyo.isValmisLaskettavaksi()) {
											LOG.error(
													"Laskentaa ei voida tehda hakukohteelle {}",
													tyo.getHakukohdeOid());
											seurantaResource
													.merkkaaHakukohteenTila(
															tyo.getLaskenta()
																	.getUuid(),
															tyo.getHakukohdeOid(),
															HakukohdeTila.KESKEYTETTY);
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
												seurantaResource
														.merkkaaHakukohteenTila(
																tyo.getLaskenta()
																		.getUuid(),
																tyo.getHakukohdeOid(),
																HakukohdeTila.VALMIS);
												return;
											} else if (tyo
													.getValintaperusteet()
													.isEmpty()) {
												LOG.warn(
														"Laskentaa ei tehda hakukohteelle {} koska ei ole valintaperusteita",
														tyo.getHakukohdeOid());
												seurantaResource
														.merkkaaHakukohteenTila(
																tyo.getLaskenta()
																		.getUuid(),
																tyo.getHakukohdeOid(),
																HakukohdeTila.VALMIS);
												return;
											}
											LOG.debug(
													"Laskenta hakukohteelle {}. Valmis laskettavaksi == {}",
													tyo.getHakukohdeOid(),
													tyo.isValmisLaskettavaksi());
											LaskeDTO laskeDTO = new LaskeDTO();
											List<HakemusDTO> hakemukset = tyo
													.getHakemukset()
													.stream()
													.map(h -> getContext()
															.getTypeConverter()
															.tryConvertTo(
																	HakemusDTO.class,
																	h))
													.collect(
															Collectors.toList());

											laskeDTO.setHakemus(hakemukset);
											Collections
													.sort(tyo
															.getValintaperusteet(),
															new Comparator<ValintaperusteetDTO>() {
																public int compare(
																		ValintaperusteetDTO o1,
																		ValintaperusteetDTO o2) {
																	return new Integer(
																			o1.getValinnanVaihe()
																					.getValinnanVaiheJarjestysluku())
																			.compareTo(o2
																					.getValinnanVaihe()
																					.getValinnanVaiheJarjestysluku());
																}
															});
											for (ValintaperusteetDTO vp : tyo
													.getValintaperusteet()) {
												laskeDTO.setValintaperuste(Arrays
														.asList(vp));
												if (isValintakoelaskenta(vp)) {
													valintalaskentaResource
															.valintakokeet(laskeDTO);
													LOG.debug(
															"Valintakoelaskenta hakukohteelle {} suoritettu valinnanvaiheelle {}",
															tyo.getHakukohdeOid(),
															vp.getValinnanVaihe()
																	.getValinnanVaiheJarjestysluku());
												} else {
													valintalaskentaResource
															.laske(laskeDTO);
													LOG.debug(
															"Valintalaskenta hakukohteelle {} suoritettu valinnanvaiheelle {}",
															tyo.getHakukohdeOid(),
															vp.getValinnanVaihe()
																	.getValinnanVaiheJarjestysluku());
												}
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
											seurantaResource
													.merkkaaLaskennanTila(tyo
															.getLaskenta()
															.getUuid(),
															LaskentaTila.VALMIS);
										}
									}
								},
								((tyo, poikkeus) -> {
									LOG.error(
											"Laskentaa suoritettaessa hakukohteelle {} tapahtui poikkeus {}",
											tyo.getHakukohdeOid(),
											poikkeus.getMessage());
									seurantaResource.merkkaaHakukohteenTila(tyo
											.getLaskenta().getUuid(), tyo
											.getHakukohdeOid(),
											HakukohdeTila.KESKEYTETTY);
									return true;
								})))
				//
				.stop();
	}

	private boolean isValintakoelaskenta(ValintaperusteetDTO vp) {
		return vp.getValinnanVaihe().getValintakoe() != null
				&& !vp.getValinnanVaihe().getValintakoe().isEmpty();
	}

	@Override
	protected String deadLetterChannelEndpoint() {
		return DEADLETTERCHANNEL;
	}
}
