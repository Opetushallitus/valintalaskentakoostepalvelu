package fi.vm.sade.valinta.kooste.kela.route.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
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


import fi.vm.sade.organisaatio.resource.api.KelaResource;
import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.rajapinnat.kela.tkuva.util.KelaUtil;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakukohdeDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.Reititys;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.ValintaTulosServiceDto;
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
import fi.vm.sade.valinta.kooste.kela.komponentti.TilaSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.TutkinnontasoSource;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.HaunTyyppiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.OppilaitosKomponentti;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.LogEntry;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Valintatulos;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.TilaResource;
//import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
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
	private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
	private final DokumenttiResource dokumenttiResource;
	private final HaunTyyppiKomponentti haunTyyppiKomponentti;
	private final ApplicationResource applicationResource;
	private final OppilaitosKomponentti oppilaitosKomponentti;
	private final HakuV1Resource hakuResource;
	private final LinjakoodiKomponentti linjakoodiKomponentti;
	private final HakukohdeResource hakukohdeResource;
	private final KoodiService koodiService;
	private final String kelaLuonti;
	private final KelaResource kelaResource;
	private final TilaResource tilaResource;

	@Autowired
	public KelaRouteImpl(
			@Value(KelaRoute.SEDA_KELA_LUONTI) String kelaLuonti,
			DokumenttiResource dokumenttiResource,
			KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti,
			KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti,
			HakuV1Resource hakuResource,
			HaunTyyppiKomponentti haunTyyppiKomponentti,
			ApplicationResource applicationResource,
			OppilaitosKomponentti oppilaitosKomponentti,
			LinjakoodiKomponentti linjakoodiKomponentti,
			HakukohdeResource hakukohdeResource, KoodiService koodiService,
			ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
			KelaResource kelaResource,
			TilaResource tilaResource) {
		this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
		this.koodiService = koodiService;
		this.hakukohdeResource = hakukohdeResource;
		this.oppilaitosKomponentti = oppilaitosKomponentti;
		this.linjakoodiKomponentti = linjakoodiKomponentti;
		this.haunTyyppiKomponentti = haunTyyppiKomponentti;
		this.hakuResource = hakuResource;
		this.kelaLuonti = kelaLuonti;
		this.dokumenttiResource = dokumenttiResource;
		this.kelaHakijaKomponentti = kelaHakijaKomponentti;
		this.kelaDokumentinLuontiKomponentti = kelaDokumentinLuontiKomponentti;
		this.applicationResource = applicationResource;
		this.kelaResource = kelaResource;
		this.tilaResource = tilaResource;
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
							//tilaResource.hakemus(hakuOid, hakukohdeOid, valintatapajonoOid, hakemusOid)
							//Valintatulos d = tilaResource.hakemus("1.2.246.562.29.173465377510", "1.2.246.562.20.17956132108", "14158727968525590216428056898001", "1.2.246.562.11.00001196984");
						  /*	{hakemusOid}/{hakuOid}/{hakukohdeOid}/{valintatapajonoOid}/"
							1.2.246.562.11.00001196984/1.2.246.562.29.173465377510/1.2.246.562.20.17956132108/14158727968525590216428056898001 */
							
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
													hakuOid));
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
													hakuOid));
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
							Boolean kaikkiHautKk = null;
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
															hakutyyppiUri
															));
									throw e;
								}
								String hakutyypinArvo = luontiJaHaut
										.getLuonti()
										.getCache()
										.getHakutyyppi(
												haku.getAsTarjontaHakuDTO()
														.getHakutyyppiUri());

                                String haunKohdejoukkoUri = haku
                                        .getAsTarjontaHakuDTO()
                                        .getKohdejoukkoUri();
                                try {
                                    if (luontiJaHaut.getLuonti().getCache()
                                            .getHaunKohdejoukko(haunKohdejoukkoUri) == null) {
                                        luontiJaHaut
                                                .getLuonti()
                                                .getCache()
                                                .putHaunKohdejoukko(
                                                        haunKohdejoukkoUri,
                                                        haunTyyppiKomponentti
                                                                .haunKohdejoukko(haunKohdejoukkoUri));
                                    }
                                } catch (Exception e) {
                                    luontiJaHaut
                                            .getLuonti()
                                            .getProsessi()
                                            .getPoikkeuksetUudelleenYrityksessa()
                                            .add(new Poikkeus(
                                                    Poikkeus.KOODISTO,
                                                    "Haun kohdejoukolle "
                                                            + haunKohdejoukkoUri
                                                            + " ei saatu arvoa koodistosta",
                                                            haunKohdejoukkoUri));
                                    throw e;
                                }
                                String haunKohdejoukonArvo = luontiJaHaut
                                        .getLuonti()
                                        .getCache()
                                        .getHaunKohdejoukko(
                                                haku.getAsTarjontaHakuDTO()
                                                        .getKohdejoukkoUri());                                
                                
                                
                                // Koodistosta saa hakutyypille arvon ja nimen.
                                // Oletetaan etta
                                // nimi voi vaihtua mutta koodi pysyy vakiona.

                                boolean lisahaku = "03".equals(hakutyypinArvo);
                                boolean kkhaku = "12".equals(haunKohdejoukonArvo);
								if ( kaikkiHautKk == null ) {
									kaikkiHautKk = kkhaku;
								}
								if ( kaikkiHautKk ^ kkhaku ) { // ei saa olla erityyppisia hakuja (kk tai keskiaste)
									String virhe = "Annettujen hakujen on kaikkien oltava kk-hakuja tai niistä mikään ei saa olla kk-haku!";
									luontiJaHaut
											.getLuonti()
											.getProsessi()
											.getPoikkeuksetUudelleenYrityksessa()
											.add(new Poikkeus(
													Poikkeus.KOOSTEPALVELU, virhe,haunKohdejoukonArvo));
									throw new RuntimeException(virhe);
								}
								haut.add(new TunnistettuHaku(haku.getAsTarjontaHakuDTO(), kkhaku, lisahaku));
							}
							luontiJaHaut.getLuonti().setKkHaku(kaikkiHautKk);
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
							//
							// .routeId("KELALUONTI_LISAHAKU")
							//
							for (Haku tunnistettuHaku : luontiJaHaut.getHaut()) {
							
								HakuV1RDTO haku = tunnistettuHaku
										.getAsTarjontaHakuDTO();
								log.info("haetaan haku:"+haku.getOid());
								
								if (haku == null) {
									throw new RuntimeException(
											"Reitill� oli null hakuDTO!");
								}
								// haetaan kaikki hakemukset lisahaulle koska ei
								// voida
								// tietaa tarkastelematta ketka on valittuja.
								try {
							
									Collection<ValintaTulosServiceDto> hakijat = null;
									hakijat = valintaTulosServiceAsyncResource
											.getValintatulokset(haku.getOid())
											.get()
											.stream()
											.filter(vts -> vts
													.getHakutoiveet()
													.stream()
													// non nulls
													.filter(h -> h != null && h.getValintatila() != null && h.getVastaanottotila() != null)
													.anyMatch(
															hakutoive ->

															hakutoive
																	.getVastaanottotila()
																	.isVastaanottanut()
																	&& hakutoive
																			.getValintatila()
																			.isHyvaksytty())
											).collect(Collectors.toList());
									KelaHaku kelahaku = new KelaHaku(hakijat,
											haku, luontiJaHaut.getLuonti()
													.getCache());
									log.info("hakijat:"+hakijat.size());
									haut.add(kelahaku);
								} catch (Exception e) {
									LOG.error("Virhe kelaluonnissa {}\r\n{}",
											e.getMessage(),
											Arrays.toString(e.getStackTrace()));
									luontiJaHaut
											.getLuonti()
											.getProsessi()
											.getPoikkeuksetUudelleenYrityksessa()
											.add(new Poikkeus(
													Poikkeus.SIJOITTELU,
													"Vastaanottaneiden haku sijoittelusta ep�onnistui haulle, koska: "
															+ e.getMessage(),
													haku.getOid()));
									throw new RuntimeException(e);
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
								LOG.warn("Haetaan {} hakemusta, {} er�ss�",
										hakemusOidit.size(),
										oiditSivutettuna.size());
								for (List<String> oidit : oiditSivutettuna) {
									try {
										List<Hakemus> h = applicationResource
												.getApplicationsByOids(oidit);
										hakemukset.addAll(h);
										LOG.warn(
												"Saatiin er� hakemuksia {}. {}/{}",
												h.size(), hakemukset.size(),
												hakemusOidit.size());
									} catch (Exception e) {
										LOG.error(
												"Hakemuspalvelu ei jaksa tarjoilla hakemuksia {}. Yritet��n viel� uudestaan.",
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
							
                            TutkinnontasoSource tutkinnontasoSource = new TutkinnontasoSource() {
                                Map<String, String> c = Maps.newHashMap();
                                Map<String, String> d = Maps.newHashMap();

                                @Override
                                public String getTutkinnontaso(String hakukohdeOid)  {
                                        if (!c.containsKey(hakukohdeOid)) {
                                                try {
                                                        c.put(hakukohdeOid, kelaResource.tutkinnontaso(hakukohdeOid));
                                                } catch (Exception e) {
                                                        LOG.error("Ei saatu kela-rajapinnalta tutkinnon tasoa hakukohteelle (oid: {})\r\n{}",
                                                                hakukohdeOid, e.getMessage());
                                                        throw e;
                                                }
                                        }
                                        return c.get(hakukohdeOid);
                                }

                                @Override
                                public String getKoulutusaste(String hakukohdeOid) {
                                        if (!d.containsKey(hakukohdeOid)) {
                                                try {
                                                        String koulutusaste = kelaResource.koulutusaste(hakukohdeOid);
                                                        if (! koulutusaste.startsWith("ERROR")) {
                                                                d.put(hakukohdeOid, kelaResource.koulutusaste(hakukohdeOid));
                                                        } else {
                                                                throw new RuntimeException("rajapinta palautti koulutusasteen ERROR");
                                                        }

                                                } catch (Exception e) {
                                                        LOG.error("Ei saatu kela-rajapinnalta koulutusastetta hakukohteelle (oid: {})\r\n{}",
                                                                hakukohdeOid, e.getMessage());
                                                        throw e;
                                                }
                                        }
                                        return d.get(hakukohdeOid);
                                }
                        };
                        TilaSource tilaSource = new TilaSource() {
							@Override
							public LogEntry getVastaanottopvm(String hakemusOid,
									String hakuOid, String hakukohdeOid,
									String valintatapajonoOid) {
									Valintatulos valintatulos = null;
									int tries=0;
									
									while(true) {
										try {
											valintatulos = tilaResource.hakemus(hakuOid, hakukohdeOid, valintatapajonoOid, hakemusOid);
											if (tries>0) {
												LOG.error("retry ok");
											}
											break;
										} catch (Exception e) {
											if (tries==20) {
												LOG.error("give up");
												throw e;
											}	
											tries++;
											LOG.error("tilaResource ei jaksa palvella {}. Yritet��n viel� uudestaan. "+tries+"/20...", e.getMessage());
											try {
												Thread.sleep(10000L);
											} catch (InterruptedException e1) {
												e1.printStackTrace();
											}
										}
									}
									
									
									LogEntry ret = null;
									for (LogEntry logEntry : valintatulos.getLogEntries()) {
										if (logEntry.getMuutos().equalsIgnoreCase("VASTAANOTTANUT_SITOVASTI")) {
											return logEntry;
										}
									}
									for (LogEntry logEntry : valintatulos.getLogEntries()) {
										if ( logEntry.getMuutos().equalsIgnoreCase("VASTAANOTTANUT")) {
											return logEntry;
										}
									}
									for (LogEntry logEntry : valintatulos.getLogEntries()) {
										if ( logEntry.getMuutos().equalsIgnoreCase("EHDOLLISESTI_VASTAANOTTANUT")) {
											return logEntry;
										}
									}
									LOG.error("No logentries for event VASTAANOTTANUT_SITOVASTI, VASTAANOTTANUT or EHDOLLISESTI_VASTAANOTTANUT for hakuOid:"+hakuOid+",hakukohdeOid:"+hakukohdeOid+", valintatapajonoOid:"+valintatapajonoOid+", hakemusOid:"+hakemusOid+ " valintatulos tila:"+valintatulos.getTila()); 
									return ret;
							}

                        };
						for (KelaAbstraktiHaku kelahaku : luontiJaRivit
								.getHaut()) {
							rivit.addAll(kelahaku.createHakijaRivit(
									luontiJaRivit.getLuonti().getAlkuPvm(),
									luontiJaRivit.getLuonti().getLoppuPvm(),
									kelahaku.getHaku().getOid(), //TODO_-
									luontiJaRivit.getLuonti().getProsessi(),
									luontiJaRivit.getLuonti().getCache(),
									hakukohdeSource, linjakoodiSource,
									oppilaitosSource, tutkinnontasoSource, tilaSource));
						}
						
						if (rivit.isEmpty()) {
							String virhe = "Kela-dokumenttia ei voi luoda hauille joissa ei ole yht��n valittua hakijaa!";
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
								SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM");
								return new KelaLuontiJaDokumentti(
										luontiJaRivit.getLuonti(),
										kelaDokumentinLuontiKomponentti
												.luo(rivit,
														(luontiJaRivit.getLuonti().getAineistonNimi()+"                          ").substring(0,26)
															+"["+DATE_FORMAT.format(luontiJaRivit.getLuonti().getAlkuPvm())
															+"-"
															+DATE_FORMAT.format(luontiJaRivit.getLuonti().getLoppuPvm())+"]",
														luontiJaRivit
																.getLuonti()
																.getOrganisaationNimi(),
																(luontiJaRivit.getLuonti().isKkHaku() ? "OUHARE" : "OUYHVA")
																));
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
								dokumenttiResource.tallenna(id, 
										luontiJaDokumentti.getLuonti().isKkHaku() ? KelaUtil.createTiedostoNimiOuhare(new Date()) : KelaUtil.createTiedostoNimiYhva14(new Date()),
										expirationTime, tags,
										"application/octet-stream", filedata);							
								luontiJaDokumentti.getLuonti().getProsessi()
										.setDokumenttiId(id);
								luontiJaDokumentti.getLuonti().getProsessi()
										.inkrementoiTehtyjaToita();
								LOG.info("DONE");
							} catch (Exception e) {
								luontiJaDokumentti
										.getLuonti()
										.getProsessi()
										.getPoikkeuksetUudelleenYrityksessa()
										.add(new Poikkeus(
												Poikkeus.DOKUMENTTIPALVELU,
												"Kela-dokumentin tallennus dokumenttipalveluun ep�onnistui"));
								try {
									throw e;
								} catch (Exception e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}
						}));
	}
}
