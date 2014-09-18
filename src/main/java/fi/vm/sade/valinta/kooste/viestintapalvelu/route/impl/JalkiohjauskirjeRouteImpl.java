package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static fi.vm.sade.sijoittelu.tulos.dto.ValintatuloksenTila.*;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakutoiveDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluIlmankoulutuspaikkaaKomponentti;
import fi.vm.sade.valinta.kooste.util.Formatter;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.KieliUtil;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.MetaHakukohde;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Teksti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeRoute;

@Component
public class JalkiohjauskirjeRouteImpl extends AbstractDokumenttiRouteBuilder {
	private final static Logger LOG = LoggerFactory
			.getLogger(JalkiohjauskirjeRouteImpl.class);
	private static final String TYHJA_TARJOAJANIMI = "Tuntematon koulu!";
	private final ViestintapalveluResource viestintapalveluResource;
	private final KirjeetHakukohdeCache kirjeetHakukohdeCache;
	private final JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti;
	private final SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluIlmankoulutuspaikkaaKomponentti;
	private final DokumenttiResource dokumenttiResource;
	private final String jalkiohjauskirjeet;
	private final ApplicationResource applicationResource;

	@Autowired
	public JalkiohjauskirjeRouteImpl(
			ApplicationResource applicationResource,
			@Value(JalkiohjauskirjeRoute.SEDA_JALKIOHJAUSKIRJEET) String jalkiohjauskirjeet,
			ViestintapalveluResource viestintapalveluResource,
			JalkiohjauskirjeetKomponentti jalkiohjauskirjeetKomponentti,
			KirjeetHakukohdeCache kirjeetHakukohdeCache,
			DokumenttiResource dokumenttiResource,
			SijoitteluIlmankoulutuspaikkaaKomponentti sijoitteluProxy) {
		super();
		this.kirjeetHakukohdeCache = kirjeetHakukohdeCache;
		this.applicationResource = applicationResource;
		this.dokumenttiResource = dokumenttiResource;
		this.viestintapalveluResource = viestintapalveluResource;
		this.jalkiohjauskirjeetKomponentti = jalkiohjauskirjeetKomponentti;
		this.sijoitteluIlmankoulutuspaikkaaKomponentti = sijoitteluProxy;
		this.jalkiohjauskirjeet = jalkiohjauskirjeet;
	}

	@Override
	public void configure() throws Exception {
		Endpoint luontiEpaonnistui = endpoint("direct:jalkiohjauskirjeet_deadletterchannel");
		from(luontiEpaonnistui)
		//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						String syy;
						if (exchange.getException() == null) {
							syy = "Jälkiohjauskirjeiden luonti epäonnistui tuntemattomasta syystä. Ota yheys ylläpitoon.";
						} else {
							syy = exchange.getException().getMessage();
						}
						dokumenttiprosessi(exchange).getPoikkeukset().add(
								new Poikkeus(Poikkeus.KOOSTEPALVELU,
										"Jälkiohjauskirjeiden luonti", syy));
					}
				})
				//
				.stop();
		from(jalkiohjauskirjeet)
				//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(SecurityPreprocessor.SECURITY)
				//
				.choice()
				//
				.when(prosessiOnKeskeytetty())
				//
				.log(LoggingLevel.WARN,
						"Ohitetaan prosessi ${property.property_valvomo_prosessi} koska se on merkitty keskeytetyksi!")
				//
				.stop()
				//
				.otherwise()
				//
				.to("direct:jalkiohjauskirjeet_jatketaan")
				//
				.end();

		from("direct:jalkiohjauskirjeet_jatketaan")
		//
				.errorHandler(
				//
						deadLetterChannel(luontiEpaonnistui)
								//
								.maximumRedeliveries(0)
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				// Kaikille sijoittelun antamille valitsemattomille
				// jalkiohjauskirje
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						try {
							final List<HakijaDTO> hyvaksymattomatHakijat = sijoitteluIlmankoulutuspaikkaaKomponentti
									.ilmankoulutuspaikkaa(exchange.getProperty(
											OPH.HAKUOID, String.class),
											SijoitteluResource.LATEST);
							exchange.getOut().setBody(hyvaksymattomatHakijat);
						} catch (Exception e) {
							LOG.error("Ei saatu sijoittelulta hyväksymättömiä hakijoita!");
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.SIJOITTELU,
											"Ei saatu sijoittelulta hyväksymättömiä hakijoita!",
											""));
							throw e;
						}
					}

				})
				// TODO: Hae osoitteet erikseen
				// TODO: Cache ulkopuolisiin palvelukutsuihin
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						final String preferoitukielikoodi = exchange
								.getProperty(KieliUtil.PREFEROITUKIELIKOODI,
										StringUtils.EMPTY, String.class);
						@SuppressWarnings("unchecked")
						Collection<HakijaDTO> hyvaksymattomatHakijat = exchange
								.getIn().getBody(List.class);

						hyvaksymattomatHakijat = hyvaksymattomatHakijat
								.stream()
								//
								// Filtteröidään puutteellisilla tiedoilla
								// olevat
								// hakijat pois
								//
								.filter(hakija -> {
									if (hakija == null
											|| hakija.getHakutoiveet() == null
											|| hakija.getHakutoiveet()
													.isEmpty()) {
										LOG.error("Hakija ilman hakutoiveita!");
										return false;
									}
									return true;
								})
								//
								// OVT-8553 Itse itsensa peruuttaneet pois
								//
								.filter(hakija -> hakija
										.getHakutoiveet()
										.stream()
										.anyMatch(
												hakutoive -> hakutoive
														.getHakutoiveenValintatapajonot()
														.stream()
														.noneMatch(
																valintatapajono -> valintatapajono
																		.getVastaanottotieto() == PERUNUT)))
								//
								.collect(Collectors.toList());

						final Set<String> filterOids = Sets
								.<String> newHashSet(hakemusOids(exchange));
						if (!filterOids.isEmpty()) {
							hyvaksymattomatHakijat = hyvaksymattomatHakijat
									.stream()
									//
									.filter(hakija -> filterOids
											.contains(hakija.getHakemusOid()))
									//
									.collect(Collectors.toList());
						}

						if (hyvaksymattomatHakijat.isEmpty()) {
							LOG.error("Jälkiohjauskirjeitä ei voida luoda kun ei ole hakijoita!");
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.KOOSTEPALVELU,
											"Jälkiohjauskirjeiden luonti",
											"Jälkiohjauskirjeitä ei voida luoda kun ei ole jälkiohjattavaa hakijaa!"));
							throw new RuntimeException(
									"Jälkiohjauskirjeitä ei voida luoda kun ei ole hakijoita!");
						}

						dokumenttiprosessi(exchange).setKokonaistyo(3); // hakemukset
						// +
						// tarjonnasta
						// haku +
						// osoitteiden haku +
						// dokumentin
						// luonti +
						// dokumentin
						// vienti

						Collection<Hakemus> hakemukset;
						try {

							hakemukset = kutsuKahdesti(FluentIterable
									.from(hyvaksymattomatHakijat)
									.transform(
											new Function<HakijaDTO, String>() {
												public String apply(
														HakijaDTO input) {
													return input
															.getHakemusOid();
												}
											}).toList());
						} catch (Exception e) {

							LOG.error(
									"Jälkiohjauskirjeitä varten ei saatu hakemuksia {}:\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));
							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.HAKU,
											"Hakemusten haku",
											"hakemusten haku epäonnistui!"));
							throw e;
						}
						if (StringUtils.isEmpty(preferoitukielikoodi)) {
							// OK
						} else {
							// VAIN RUOTSINKIELISET HALUTAAN
							if (KieliUtil.RUOTSI.equals(preferoitukielikoodi)) {

								hakemukset = FluentIterable.from(hakemukset)
										.filter(new Predicate<Hakemus>() {
											@Override
											public boolean apply(Hakemus input) {
												return KieliUtil.RUOTSI
														.equals(new HakemusWrapper(
																input)
																.getAsiointikieli());
											}
										}).toList();

							} else {
								// VAIN SUOMENKIELISET HALUTAAN
								hakemukset = FluentIterable.from(hakemukset)
										.filter(new Predicate<Hakemus>() {
											@Override
											public boolean apply(Hakemus input) {
												return !KieliUtil.RUOTSI
														.equals(new HakemusWrapper(
																input)
																.getAsiointikieli());
											}
										}).toList();
							}
						}
						final Set<String> oidSet = FluentIterable
								.from(hakemukset)
								.transform(new Function<Hakemus, String>() {
									@Override
									public String apply(Hakemus input) {
										return input.getOid();
									}
								}).toSet();
						final Map<String, MetaHakukohde> metaKohteet = new HashMap<String, MetaHakukohde>();
						for (HakijaDTO hakija : hyvaksymattomatHakijat) {
							if (!oidSet.contains(hakija.getHakemusOid())) {
								continue;
							}
							for (HakutoiveDTO hakutoive : hakija
									.getHakutoiveet()) {
								if (dokumenttiprosessi(exchange)
										.isKeskeytetty()) {
									dokumenttiprosessi(exchange)
											.getPoikkeukset()
											.add(new Poikkeus(
													Poikkeus.KOOSTEPALVELU,
													"Jälkiohjauskirjeen muodostus on keskeytetty!",
													""));
									throw new RuntimeException(
											"Jälkiohjauskirjeen muodostus on keskeytetty!");
								}
								String hakukohdeOid = hakutoive
										.getHakukohdeOid();
								if (!metaKohteet.containsKey(hakukohdeOid)) { // lisataan
																				// puuttuva
																				// hakukohde
									try {

										metaKohteet
												.put(hakukohdeOid,
														kirjeetHakukohdeCache
																.haeHakukohde(hakukohdeOid));

									} catch (Exception e) {
										e.printStackTrace();
										LOG.error(
												"Tarjonnasta ei saatu hakukohdetta {}: {}",
												new Object[] { hakukohdeOid,
														e.getMessage() });
										metaKohteet
												.put(hakukohdeOid,
														new MetaHakukohde(
																new Teksti(
																		new StringBuilder()
																				.append("Hakukohde ")
																				.append(hakukohdeOid)
																				.append(" ei löydy tarjonnasta!")
																				.toString()),
																new Teksti(
																		TYHJA_TARJOAJANIMI)));
									}

								}
							}
						}
						try {
							exchange.getOut().setBody(
									jalkiohjauskirjeetKomponentti
											.teeJalkiohjauskirjeet(
													preferoitukielikoodi,
													hyvaksymattomatHakijat,
													hakemukset, metaKohteet,
													hakuOid(exchange),
													exchange.getProperty(
															"templateName",
															"jalkiohjauskirje",
															String.class),
													exchange.getProperty(
															"sisalto",
															String.class),
													exchange.getProperty("tag",
															String.class)));
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
						} catch (Exception e) {
							LOG.error(
									"Jälkiohjauskirjeitä ei saatu muodostettua: {}\r\n{}",
									e.getMessage(), e.getStackTrace());
							e.printStackTrace();
							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.HAKU,
											"Hakemusten haku",
											"hakemusten haku epäonnistui!"));
							throw e;
						}

					}
				})
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						DokumenttiProsessi prosessi = dokumenttiprosessi(exchange);
						LetterBatch kirjeet = jalkiohjauskirjeet(exchange);
						if (kirjeet == null || kirjeet.getLetters() == null
								|| kirjeet.getLetters().isEmpty()) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.VALINTATIETO,
											"Jälkiohjauskirjeitä ei voida muodostaa tyhjälle tulosjoukolle."));
							throw new RuntimeException(
									"Jälkiohjauskirjeitä ei voida muodostaa tyhjälle tulosjoukolle.");
						}
						InputStream pdf;
						try {

							// LOG.error("\r\n{}",
							// new GsonBuilder().setPrettyPrinting()
							// .create().toJson(osoitteet));
							Gson gson = new GsonBuilder().create();
							String json = gson.toJson(kirjeet);
							LOG.error(
									"Siirretään jälkiohjauskirjeisiin {} kirjettä koolta {}",
									kirjeet.getLetters().size(), Formatter
											.humanReadableByteCount(json
													.length()));

							// LOG.error("\r\n{}\r\n", json);
							pdf = pipeInputStreams(viestintapalveluResource
									.haeKirjeSyncZip(json));
							// LOG.error(
							// "\r\n{}",
							// new GsonBuilder().setPrettyPrinting()
							// .create()
							// .toJson(koekutsukirjeet(exchange)));

							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();

						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Viestintäpalvelulta pdf:n haussa tapahtui virhe {}:\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));
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
									"jalkiohjauskirjeet.zip",
									defaultExpirationDate().getTime(),
									prosessi.getTags(), "application/zip", pdf);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
							prosessi.setDokumenttiId(id);
						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Dokumenttipalvelulle tiedonsiirrossa tapahtui virhe {}:\r\n{}",
									e.getMessage(),
									Arrays.toString(e.getStackTrace()));
							dokumenttiprosessi(exchange).getPoikkeukset().add(
									new Poikkeus(Poikkeus.DOKUMENTTIPALVELU,
											"Dokumentin tallennus", e
													.getMessage()));
							throw e;
						}
					}
				});
	}

	/**
	 * 
	 */
	private List<Hakemus> kutsuKahdesti(List<String> oids) throws Exception {
		Exception e = null;
		for (int i = 0; i < 2; ++i) {
			try {
				return applicationResource.getApplicationsByOids(oids);
			} catch (Exception ex) {
				e = ex;
				Thread.sleep(1500);
			}
		}
		throw e;
	}

	@SuppressWarnings("unchecked")
	private LetterBatch jalkiohjauskirjeet(Exchange exchange) {
		return exchange.getIn().getBody(LetterBatch.class);
	}

}
