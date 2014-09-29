package fi.vm.sade.valinta.kooste.kela.route.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.sijoittelu.tulos.dto.HakemuksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.kela.dto.Haku;
import fi.vm.sade.valinta.kooste.kela.dto.KelaAbstraktiHaku;
import fi.vm.sade.valinta.kooste.kela.dto.KelaCache;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHakijaRivi;
import fi.vm.sade.valinta.kooste.kela.dto.KelaHaku;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuontiJaAbstraktitHaut;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuontiJaDokumentti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuontiJaHaut;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuontiJaRivit;
import fi.vm.sade.valinta.kooste.kela.dto.TunnistamatonHaku;
import fi.vm.sade.valinta.kooste.kela.dto.TunnistettuHaku;
import fi.vm.sade.valinta.kooste.kela.komponentti.HakukohdeSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.LinjakoodiSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.OppilaitosSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.HaunTyyppiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.OppilaitosKomponentti;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKaikkiPaikanVastaanottaneet;
import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HakutoiveenValintatapajonoComparator;
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
	private final HaunTyyppiKomponentti haunTyyppiKomponentti;
	private final ApplicationResource applicationResource;
	private final OppilaitosKomponentti oppilaitosKomponentti;
	private final HakuV1Resource hakuResource;
	private final LinjakoodiKomponentti linjakoodiKomponentti;
	private final HakukohdeResource hakukohdeResource;
	private final KoodiService koodiService;
	private final String kelaLuonti;

	@Autowired
	public KelaRouteImpl(
			@Value(KelaRoute.SEDA_KELA_LUONTI) String kelaLuonti,
			DokumenttiResource dokumenttiResource,
			KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti,
			KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti,
			SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet,
			HakuV1Resource hakuResource,
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

	private DefaultErrorHandlerBuilder deadLetterChannel() {
		return deadLetterChannel(KelaRoute.DIRECT_KELA_FAILED)
				.logExhaustedMessageHistory(true).logExhausted(true)
				.logStackTrace(true).logRetryStackTrace(true).logHandled(true);
	}

	/**
	 * Kela Camel Configuration: Siirto and document generation.
	 */
	public final void configure() {
		Endpoint haeHakuJaValmistaHaku = endpoint("direct:kelaluonti_hae_ja_valmista_haku");
		Endpoint tarkistaHaunTyyppi = endpoint("direct:kelaluonti_tarkista_haun_tyyppi");
		Endpoint keraaHakujenDatat = endpoint("direct:kelaluonti_keraa_hakujen_datat");
		Endpoint vientiDokumenttipalveluun = endpoint("direct:kelaluonti_vienti_dokumenttipalveluun");

		/**
		 * Deadletterchannel
		 */
		from(KelaRoute.DIRECT_KELA_FAILED)
		//
				.routeId("KELALUONTI_DEADLETTERCHANNEL")
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

		from(haeHakuJaValmistaHaku)
		//
				.routeId("KELALUONTI_HAKU")
				//
				.errorHandler(deadLetterChannel())
				//
				// .maximumRedeliveries(1).redeliveryDelay(1500L))
				//
				.process(
						Reititys.<KelaLuonti, KelaLuontiJaHaut> funktio(luonti -> {

							List<Haku> haut = Lists.newArrayList();
							for (String hakuOid : luonti.getHakuOids()) {
								HakuV1RDTO haku;
								try {
									ResultV1RDTO<HakuV1RDTO> hakuResult = hakuResource
											.findByOid(hakuOid);
									haku = hakuResult.getResult();
									haut.add(new TunnistamatonHaku(haku));
									// cache(exchange).put(haku);
								} catch (Exception e) {
									luonti.getProsessi()
											.getPoikkeuksetUudelleenYrityksessa()
											.add(new Poikkeus(
													Poikkeus.TARJONTA,
													"Haun haku oid:lla.",
													new Oid(hakuOid,
															Poikkeus.HAKUOID)));
									throw e;
								}
								try {
									luonti.getCache().lukuvuosi(haku);
								} catch (Exception e) {
									luonti.getProsessi()
											.getPoikkeuksetUudelleenYrityksessa()
											.add(new Poikkeus(
													Poikkeus.KOODISTO,
													"Lukuvuoden haku haulle koodistosta URI:lla "
															+ haku.getKoulutuksenAlkamiskausiUri(),
													new Oid(hakuOid,
															Poikkeus.HAKUOID)));
									throw e;
								}
							}
							return new KelaLuontiJaHaut(luonti, haut);
						}))
				//
				.to(tarkistaHaunTyyppi);

		from(tarkistaHaunTyyppi)
		//
				.routeId("KELALUONTI_TARKISTA_TYYPPI")
				//
				.errorHandler(deadLetterChannel())
				//
				// .maximumRedeliveries(3).redeliveryDelay(1500L))
				//
				.process(
						Reititys.<KelaLuontiJaHaut, KelaLuontiJaHaut> funktio(luontiJaHaut -> {
							Collection<Haku> haut = Lists.newArrayList();
							for (Haku haku : luontiJaHaut.getHaut()) {
								String hakutyyppiUri = haku
										.getAsTarjontaHakuDTO()
										.getHakutyyppiUri();
								try {
									if (luontiJaHaut.getLuonti().getCache()
											.getHakutyyppi(hakutyyppiUri) == null) {
										luontiJaHaut
												.getLuonti()
												.getCache()
												.putHakutyyppi(
														hakutyyppiUri,
														haunTyyppiKomponentti
																.haunTyyppi(hakutyyppiUri));
									}
								} catch (Exception e) {
									luontiJaHaut
											.getLuonti()
											.getProsessi()
											.getPoikkeuksetUudelleenYrityksessa()
											.add(new Poikkeus(
													Poikkeus.KOODISTO,
													"Haun tyypille "
															+ hakutyyppiUri
															+ " ei saatu arvoa koodistosta",
													new Oid(
															hakutyyppiUri,
															Poikkeus.KOODISTOURI)));
									throw e;
								}
								String hakutyypinArvo = luontiJaHaut
										.getLuonti()
										.getCache()
										.getHakutyyppi(
												haku.getAsTarjontaHakuDTO()
														.getHakutyyppiUri());
								// Koodistosta saa hakutyypille arvon ja nimen.
								// Oletetaan etta
								// nimi voi vaihtua mutta koodi pysyy vakiona.
								if ("03".equals(hakutyypinArvo)) { // onko
																	// lisahaku
									haut.add(new TunnistettuHaku(haku
											.getAsTarjontaHakuDTO(), false,
											true));
								} else if ("12".equals(hakutyypinArvo)) { // onko
									// kk-haku
									haut.add(new TunnistettuHaku(haku
											.getAsTarjontaHakuDTO(), true,
											false));
								} else {
									haut.add(new TunnistettuHaku(haku
											.getAsTarjontaHakuDTO(), false,
											false));
								}
							}
							return new KelaLuontiJaHaut(luontiJaHaut
									.getLuonti(), haut);
						})).to(keraaHakujenDatat);

		from(keraaHakujenDatat)
		//
				.routeId("KELALUONTI_KERAA_HAKUJEN_DATAT")
				//
				.errorHandler(deadLetterChannel())
				//
				// .maximumRedeliveries(3).redeliveryDelay(1500L))
				//
				// Keraa hakujen datat palveluista
				//
				.process(
						Reititys.<KelaLuontiJaHaut, KelaLuontiJaAbstraktitHaut> funktio(luontiJaHaut -> {
							Collection<KelaAbstraktiHaku> haut = Lists
									.newArrayList();
							//
							// Varmistetaan etta ainoastaan hyvaksyttyja ja
							// vastaanottaneita
							//
							LOG.info("Filtteroidaan haussa ylimaaraiset hakijat pois keladokumentista!");

							Predicate<HakijaDTO> hakijaFilter = hakija -> hakija
									.getHakutoiveet()
									.stream()
									.anyMatch(
											hakutoive -> hakutoive
													.getHakutoiveenValintatapajonot()
													.stream()
													.sorted(HakutoiveenValintatapajonoComparator.DEFAULT)
													.anyMatch(
															jono -> (HakemuksenTila.HYVAKSYTTY
																	.equals(jono
																			.getTila()) || HakemuksenTila.VARASIJALTA_HYVAKSYTTY
																	.equals(jono
																			.getTila()))
																	&& (jono.getVastaanottotieto() != null && jono
																			.getVastaanottotieto()
																			.equals(ValintatuloksenTila.VASTAANOTTANUT))));
							//
							// Korkeakouluhakijoille
							// VASTAANOTTANUT_SITOVASTI voidaan
							// tulkita vastaanottaneeksi
							//
							Predicate<HakijaDTO> korkeakouluHakijaFilter = hakija -> hakija
									.getHakutoiveet()
									.stream()
									.anyMatch(
											hakutoive -> hakutoive
													.getHakutoiveenValintatapajonot()
													.stream()
													.sorted(HakutoiveenValintatapajonoComparator.DEFAULT)
													.anyMatch(
															jono -> (HakemuksenTila.HYVAKSYTTY
																	.equals(jono
																			.getTila()) || HakemuksenTila.VARASIJALTA_HYVAKSYTTY
																	.equals(jono
																			.getTila()))
																	&& (jono.getVastaanottotieto() != null && jono
																			.getVastaanottotieto()
																			.equals(ValintatuloksenTila.VASTAANOTTANUT_SITOVASTI))));
							//
							// .routeId("KELALUONTI_LISAHAKU")
							//
							for (Haku tunnistettuHaku : luontiJaHaut.getHaut()) {
								HakuV1RDTO haku = tunnistettuHaku
										.getAsTarjontaHakuDTO();
								if (haku == null) {
									throw new RuntimeException(
											"Reitillä oli null hakuDTO!");
								}
								// haetaan kaikki hakemukset lisahaulle koska ei
								// voida
								// tietaa tarkastelematta ketka on valittuja.
								try {
									Collection<HakijaDTO> hakijat = null;
									if (tunnistettuHaku.isKorkeakouluhaku()) {
										//
										// Korkeakouluhakijoille
										// VASTAANOTTANUT_SITOVASTI voidaan
										// tulkita vastaanottaneeksi
										//
										LOG.warn(
												"KK-haulle({}) sijoittelusta vastaanottaneet",
												haku.getOid());
										hakijat = sijoitteluVastaanottaneet
												.vastaanottaneet(haku.getOid())
												.stream()
												.filter(korkeakouluHakijaFilter)
												.collect(Collectors.toList());
									} else {
										if (tunnistettuHaku.isLisahaku()) {
											//
											// Hoidetaan niinkuin mika
											// tahansa haku mika ei ole kk-haku
											//
											LOG.warn(
													"Lisahaulle({}) sijoittelusta vastaanottaneet",
													haku.getOid());
										} else {
											LOG.warn(
													"Haulle({}) sijoittelusta vastaanottaneet",
													haku.getOid());
										}
										hakijat = sijoitteluVastaanottaneet
												.vastaanottaneet(haku.getOid())
												.stream().filter(hakijaFilter)
												.collect(Collectors.toList());
									}
									KelaHaku kelahaku = new KelaHaku(hakijat,
											haku, luontiJaHaut.getLuonti()
													.getCache());
									haut.add(kelahaku);
								} catch (Exception e) {
									luontiJaHaut
											.getLuonti()
											.getProsessi()
											.getPoikkeuksetUudelleenYrityksessa()
											.add(new Poikkeus(
													Poikkeus.SIJOITTELU,
													"Vastaanottaneiden haku sijoittelusta epäonnistui haulle, koska: "
															+ e.getMessage(),
													new Oid(haku.getOid(),
															Poikkeus.HAKUOID)));
									throw e;
								}

							}

							return new KelaLuontiJaAbstraktitHaut(luontiJaHaut
									.getLuonti(), haut);
						}));

		/**
		 * Kela-dokkarin luonti reitti
		 */
		from(kelaLuonti)
		//
				.errorHandler(deadLetterChannel())
				//
				.routeId("KELALUONTI")
				//
				.to(haeHakuJaValmistaHaku)
				//
				.process(
						Reititys.<KelaLuontiJaAbstraktitHaut> kuluttaja(luonti -> {
							// valmistetaan hakemusoidit silmukkaa varten
							Collection<String> hakemusOidit = Sets.newHashSet();
							for (KelaAbstraktiHaku kelahaku : luonti.getHaut()) {
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
										// annetaan hakuapp:lle vahan aikaa
										// toipua
										// ja yritetaan uudestaan
										Thread.sleep(50L);
										hakemukset.addAll(applicationResource
												.getApplicationsByOids(oidit));
									}
								}
								KelaCache cache = luonti.getLuonti().getCache();
								for (Hakemus hakemus : hakemukset) {
									cache.put(hakemus);
								}
							} catch (Exception e) {
								String virhe = "Ei saatu hakemuksia hakupalvelulta!";
								luonti.getLuonti()
										.getProsessi()
										.getPoikkeuksetUudelleenYrityksessa()
										.add(new Poikkeus(Poikkeus.HAKU, virhe));
								throw new RuntimeException(virhe);
							}
						}))
				//
				.process(
						Reititys.<KelaLuontiJaAbstraktitHaut, KelaLuontiJaRivit> funktio(luontiJaRivit -> {

							// Filtteroidaan ylimaaraiset pois ja bodyyn joukko
							// valmistettavia kela riveja
							List<KelaHakijaRivi> rivit = Lists.newArrayList();
							HakukohdeSource hakukohdeSource = new HakukohdeSource() {
								Map<String, HakukohdeDTO> c = Maps.newHashMap();

								public HakukohdeDTO getHakukohdeByOid(String oid) {
									if (!c.containsKey(oid)) {
										try {
											c.put(oid, hakukohdeResource
													.getByOID(oid));
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
								Map<String, String> d = Maps.newHashMap();

								public String getOppilaitosKoodi(
										String tarjoajaOid) {
									if (!c.containsKey(tarjoajaOid)) {
										c.put(tarjoajaOid,
												oppilaitosKomponentti
														.haeOppilaitosKoodi(tarjoajaOid));
									}
									return c.get(tarjoajaOid);
								}

								public String getOppilaitosnumero(
										String tarjoajaOid) {
									if (!d.containsKey(tarjoajaOid)) {
										d.put(tarjoajaOid,
												oppilaitosKomponentti
														.haeOppilaitosnumero(tarjoajaOid));
									}
									return d.get(tarjoajaOid);
								}
							};
							for (KelaAbstraktiHaku kelahaku : luontiJaRivit
									.getHaut()) {
								rivit.addAll(kelahaku.createHakijaRivit(
										luontiJaRivit.getLuonti().getCache(),
										hakukohdeSource, linjakoodiSource,
										oppilaitosSource));
							}
							if (rivit.isEmpty()) {
								String virhe = "Kela-dokumenttia ei voi luoda hauille joissa ei ole yhtään valittua hakijaa!";
								luontiJaRivit
										.getLuonti()
										.getProsessi()
										.getPoikkeuksetUudelleenYrityksessa()
										.add(new Poikkeus(
												Poikkeus.KOOSTEPALVELU, virhe));
								throw new RuntimeException(virhe);
							}
							luontiJaRivit.getLuonti().getProsessi()
									.setKokonaistyo(rivit.size() + 1);
							return new KelaLuontiJaRivit(luontiJaRivit
									.getLuonti(), rivit);
						}))
				//
				.process(
						Reititys.<KelaLuontiJaRivit, KelaLuontiJaDokumentti> funktio(luontiJaRivit -> {
							Collection<TKUVAYHVA> rivit = luontiJaRivit
									.getRivit()
									.stream()
									.map(rivi -> {
										try {
											return kelaHakijaKomponentti
													.luo(rivi);
										} finally {
											luontiJaRivit.getLuonti()
													.getProsessi()
													.inkrementoiTehtyjaToita();
										}
									}).collect(Collectors.toList());

							try {
								return new KelaLuontiJaDokumentti(
										luontiJaRivit.getLuonti(),
										kelaDokumentinLuontiKomponentti
												.luo(rivit,
														luontiJaRivit
																.getLuonti()
																.getAineistonNimi(),
														luontiJaRivit
																.getLuonti()
																.getOrganisaationNimi()));
							} catch (Exception e) {
								String virhe = "Kela-dokumenttia ei saatu luotua!";
								luontiJaRivit
										.getLuonti()
										.getProsessi()
										.getPoikkeuksetUudelleenYrityksessa()
										.add(new Poikkeus(
												Poikkeus.KOOSTEPALVELU, virhe));
								throw new RuntimeException(virhe);
							}
						}))
				//
				.to(vientiDokumenttipalveluun);

		from(vientiDokumenttipalveluun)
		//
				.routeId("KELALUONTI_DOKUMENTTIPALVELUUN")
				//
				.errorHandler(deadLetterChannel())
				//
				// .maximumRedeliveries(3).redeliveryDelay(1500L))
				//
				.process(
						Reititys.<KelaLuontiJaDokumentti> kuluttaja(luontiJaDokumentti -> {
							LOG.info(
									"Aloitetaan keladokumentin(uuid {}) siirtovaihe dokumenttipalveluun.",
									luontiJaDokumentti.getLuonti().getUuid());
							try {
								InputStream filedata = new ByteArrayInputStream(
										luontiJaDokumentti.getDokumentti());
								String id = generateId();
								Long expirationTime = defaultExpirationDate()
										.getTime();
								List<String> tags = luontiJaDokumentti
										.getLuonti().getProsessi().getTags();
								dokumenttiResource.tallenna(id, KelaUtil
										.createTiedostoNimiYhva14(new Date()),
										expirationTime, tags,
										"application/octet-stream", filedata);
								luontiJaDokumentti.getLuonti().getProsessi()
										.setDokumenttiId(id);
								luontiJaDokumentti.getLuonti().getProsessi()
										.inkrementoiTehtyjaToita();
							} catch (Exception e) {
								luontiJaDokumentti
										.getLuonti()
										.getProsessi()
										.getPoikkeuksetUudelleenYrityksessa()
										.add(new Poikkeus(
												Poikkeus.DOKUMENTTIPALVELU,
												"Kela-dokumentin tallennus dokumenttipalveluun epäonnistui"));
								throw e;
							}
						}));
	}
}
