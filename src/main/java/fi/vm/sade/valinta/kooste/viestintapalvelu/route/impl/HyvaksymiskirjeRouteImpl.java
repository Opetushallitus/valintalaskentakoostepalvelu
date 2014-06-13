package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.OPH;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.letter.LetterBatch;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.predicate.SijoittelussaHyvaksyttyHakijaPredicate;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;

@Component
public class HyvaksymiskirjeRouteImpl extends AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(HyvaksymiskirjeRouteImpl.class);
	private final ViestintapalveluResource viestintapalveluResource;
	private final HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti;
	private final String hyvaksymiskirjeet;
	private final SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy;
	private final DokumenttiResource dokumenttiResource;
	private final String dokumenttipalveluUrl;
	private final ApplicationResource applicationResource;

	@Autowired
	public HyvaksymiskirjeRouteImpl(
			@Value(HyvaksymiskirjeRoute.SEDA_HYVAKSYMISKIRJEET) String hyvaksymiskirjeet,
			ViestintapalveluResource viestintapalveluResource,
			HyvaksymiskirjeetKomponentti hyvaksymiskirjeetKomponentti,
			SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy,
			@Value("${valintalaskentakoostepalvelu.dokumenttipalvelu.rest.url:''}") String dokumenttipalveluUrl,
			DokumenttiResource dokumenttiResource,
			ApplicationResource applicationResource) {
		super();
		this.applicationResource = applicationResource;
		this.dokumenttipalveluUrl = dokumenttipalveluUrl;
		this.dokumenttiResource = dokumenttiResource;
		this.sijoitteluProxy = sijoitteluProxy;
		this.hyvaksymiskirjeet = hyvaksymiskirjeet;
		this.viestintapalveluResource = viestintapalveluResource;
		this.hyvaksymiskirjeetKomponentti = hyvaksymiskirjeetKomponentti;
	}

	@Override
	public void configure() throws Exception {
		configureDeadLetterChannels();
		from(hyvaksymiskirjeet)
				//

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
				.routeId("Hyväksymiskirjeet työjono")
				// TODO: Hae osoitteet erikseen
				.process(SecurityPreprocessor.SECURITY)
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
				.to("direct:hyvaksymiskirjeet_jatketaan")
				//
				.end();

		from("direct:hyvaksymiskirjeet_jatketaan")
		// TODO: Cache ulkopuolisiin palvelukutsuihin
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						final String hakukohdeOid = hakukohdeOid(exchange);
						if (hakukohdeOid == null) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.KOOSTEPALVELU,
											"Hyväksymiskirjeitä yritettiin luoda ilman hakukohdetta!"));
							throw new RuntimeException(
									"Hyväksymiskirjeitä yritettiin luoda ilman hakukohdetta!");
						}
						List<String> hakemusOids = hakemusOids(exchange);
						if (hakemusOids == null) {
							hakemusOids = Collections.<String> emptyList();
						}
						//
						//
						//
						Collection<HakijaDTO> hakukohteenHakijat;
						try {
							hakukohteenHakijat = sijoitteluProxy
									.koulutuspaikalliset(hakuOid(exchange),
											hakukohdeOid,
											SijoitteluResource.LATEST);
							// sijoittelu returns all hakemukset. include
							// // HYVAKSYTYT only
							// LOG.error("HYVÄKSYTTYJÄ ENNEN FILTTERÖINTIÄ! {}",
							// hakukohteenHakijat.size());
							Collection<HakijaDTO> ascending = Collections2
									.filter(hakukohteenHakijat,
											new SijoittelussaHyvaksyttyHakijaPredicate(
													hakukohdeOid));
							hakukohteenHakijat = ascending;
							// LOG.error("HYVÄKSYTTYJÄ JÄLKEEN FILTTERÖINNIN {}",
							// hakukohteenHakijat.size());
							final Set<String> whitelist = Sets
									.newHashSet(hakemusOids);
							if (!whitelist.isEmpty()) {
								hakukohteenHakijat = Collections2.filter(
										hakukohteenHakijat,
										new Predicate<HakijaDTO>() {
											public boolean apply(HakijaDTO input) {
												return whitelist.contains(input
														.getHakemusOid());
											}
										});

							}

							// exchange.getOut().setBody(hakukohteenHakijat);
							dokumenttiprosessi(exchange).setKokonaistyo(2);
						} catch (Exception e) {
							e.printStackTrace();
							Collection<Oid> oidit = Lists.newArrayList(Poikkeus
									.hakuOid(hakuOid(exchange)), Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.SIJOITTELU,
											"Sijoittelusta ei saatu haettua hyväksyttyjä hakijoita",
											e.getMessage(), oidit));
							throw new RuntimeException(
									"Sijoittelusta hyväksyttyjen hakijoiden haku epäonnistui",
									e);
						}
						if (hakukohteenHakijat.isEmpty()) {
							Collection<Oid> oidit = Lists.newArrayList(Poikkeus
									.hakuOid(hakuOid(exchange)), Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.SIJOITTELU,
											"Sijoittelussa ei ollut yhtään hyväksyttyä hakijaa!",
											"", oidit));
							throw new RuntimeException(
									"Sijoittelussa ei ollut yhtään hyväksyttyä hakijaa!");
						}

						try {

							List<Hakemus> hakemukset = kutsuKahdesti(FluentIterable
									.from(hakukohteenHakijat)
									.transform(
											new Function<HakijaDTO, String>() {
												@Override
												public String apply(
														HakijaDTO input) {
													return input
															.getHakemusOid();
												}
											}).toList());

							exchange.getOut().setBody(
									hyvaksymiskirjeetKomponentti
											.teeHyvaksymiskirjeet(
													hakukohteenHakijat,
													hakemukset, hakukohdeOid,
													hakuOid(exchange),
													exchange.getProperty(
															OPH.TARJOAJAOID,
															String.class),
													exchange.getProperty(
															"sisalto",
															String.class),
													exchange.getProperty("tag",
															String.class)));
						} catch (Exception e) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.HAKU,
											"Hakupalvelulta ei saatu hakemuksia!"));
							throw new RuntimeException(
									"Hakupalvelulta ei saatu hakemuksia! "
											+ e.getMessage()
											+ " "
											+ Arrays.toString(e.getStackTrace()));
						}

					}

				})
				//
				// .bean()
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						DokumenttiProsessi prosessi = dokumenttiprosessi(exchange);
						LetterBatch kirjeet = exchange.getIn().getBody(
								LetterBatch.class);
						if (kirjeet == null || kirjeet.getLetters() == null
								|| kirjeet.getLetters().isEmpty()) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.VALINTATIETO,
											"Hyväksymiskirjeitä ei voida muodostaa tyhjälle tulosjoukolle."));
							throw new RuntimeException(
									"Hyväksymiskirjeitä ei voida muodostaa tyhjälle tulosjoukolle.");
						}
						InputStream pdf;
						try {

							// LOG.error("\r\n{}",
							// new GsonBuilder().setPrettyPrinting()
							// .create().toJson(osoitteet));
							Gson gson = new GsonBuilder().setPrettyPrinting()
									.create();
							String json = gson.toJson(kirjeet);
							// LOG.error("\r\n{}\r\n", json);
							pdf = pipeInputStreams(viestintapalveluResource
									.haeKirjeSync(json));

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
											"Osoitteet pdf:n synkroninen haku viestintäpalvelulta",
											e.getMessage()));
							throw e;
						}
						String id = generateId();
						Long expirationTime = defaultExpirationDate().getTime();
						List<String> tags = prosessi.getTags();
						if (id == null || expirationTime == null
								|| tags == null || pdf == null) {
							String tila = new StringBuilder().append(id)
									.append(expirationTime).append(" tags=")
									.append(tags == null).append(" pdf=")
									.append(pdf == null).toString();
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.DOKUMENTTIPALVELU,
											"Dokumenttipalvelun kutsumisen esiehdot ei täyty!",
											tila));
							throw new RuntimeException(
									"Dokumenttipalvelun kutsumisen esiehdot ei täyty!"
											+ tila);
						}
						try {

							dokumenttiResource.tallenna(id,
									"hyvaksymiskirjeet.pdf", expirationTime,
									tags, "application/pdf", pdf);
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
											"Hyväksymiskirjeet pdf:n tallennus dokumenttipalvelulle. "
													+ dokumenttipalveluUrl, e
													.getMessage()));

							throw e;
						}
					}
				});
		// .bean(viestintapalveluResource, "haeHyvaksymiskirjeet");
	}

	private void configureDeadLetterChannels() {
		from(kirjeidenLuontiEpaonnistui())
		//
				.log(LoggingLevel.ERROR,
						"Hyväksymiskirjeiden luonti epaonnistui: ${property.CamelExceptionCaught}")
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						if (dokumenttiprosessi(exchange).getPoikkeukset()
								.isEmpty()) {
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(Poikkeus.KOOSTEPALVELU,
											"Hyväksymiskirjeitä ei voitu muodostaa. Ota yhteys ylläpitoon."));
						}

					}
				})
				//
				.stop();

		//
		// ;
	}

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

	private String kirjeidenLuontiEpaonnistui() {
		return "direct:hyvaksymiskirjeet_epaonnistui";
	}
}
