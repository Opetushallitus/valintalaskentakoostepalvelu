package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.exception.ViestintapalveluException;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Oid;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.OsoiteComparator;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.DokumenttiTyyppi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Hyvaksyttyjen Osoitetarrat ja osoitetarrat koekutsua varten
 */
@Component
public class OsoitetarratRouteImpl extends AbstractDokumenttiRouteBuilder {
	private final static Logger LOG = LoggerFactory
			.getLogger(OsoitetarratRouteImpl.class);
	private final static int UUDELLEEN_YRITYSTEN_MAARA = 3;
	private final static long UUDELLEEN_YRITYSTEN_ODOTUSAIKA = 1500L;

	private final ViestintapalveluResource viestintapalveluResource;
	private final ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti;
	private final HaeOsoiteKomponentti osoiteKomponentti;
	private final SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy;
	private final SecurityPreprocessor security = new SecurityPreprocessor();
	private final String osoitetarrat;
	private final DokumenttiResource dokumenttiResource;
	private final ApplicationResource applicationResource;

	@Autowired
	public OsoitetarratRouteImpl(
			@Value(OsoitetarratRoute.SEDA_OSOITETARRAT) String osoitetarrat,
			ViestintapalveluResource viestintapalveluResource,
			ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti,
			HaeOsoiteKomponentti osoiteKomponentti,
			SijoitteluKoulutuspaikkallisetKomponentti sijoitteluProxy,
			ApplicationResource applicationResource,
			DokumenttiResource dokumenttiResource) {
		super();
		this.applicationResource = applicationResource;
		this.osoitetarrat = osoitetarrat;
		this.dokumenttiResource = dokumenttiResource;
		this.viestintapalveluResource = viestintapalveluResource;
		this.valintatietoHakukohteelleKomponentti = valintatietoHakukohteelleKomponentti;
		this.osoiteKomponentti = osoiteKomponentti;
		this.sijoitteluProxy = sijoitteluProxy;
	}

	public static class LuoOsoitteet {
		public Osoitteet luo(List<Osoite> osoitteet) {
			if (osoitteet == null || osoitteet.isEmpty()) {
				throw new ViestintapalveluException(
						"Yritetään luoda nolla kappaletta osoitetarroja!");
			}
			Collections.sort(osoitteet, OsoiteComparator.ASCENDING);
			return new Osoitteet(osoitteet);
		}
	}

	@Override
	public void configure() throws Exception {
		configureValintakokeenOsoitetarrat();
	}

	private void configureValintakokeenOsoitetarrat() throws Exception {
		from("direct:osoitetarrat_hakemustenhaku_epaonnistui_deadletterchannel")
		//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						// Ei haluta poikkeustapauksesta raportoinnissa enaa
						// uutta poikkeusta
						Object oid = exchange.getIn().getBody();
						if (oid == null) {
							oid = "null";
						}
						LOG.error(
								"Hakemuksen hakeminen haku-app:lta epäonnistui: {}. applicationResource.getApplicationByOid({})",
								exchange.getException().getMessage(),
								oid.toString());
						dokumenttiprosessi(exchange)
								.getPoikkeukset()
								.add(new Poikkeus(
										Poikkeus.HAKU,
										"Yritettiin hakea hakemus oidilla (get application by oid)",
										exchange.getException().getMessage(),
										Poikkeus.hakemusOid(oid.toString())));
					}

				});

		from("direct:osoitetarrat_haeHakemuksetJaOsoitteet")
		//
				.errorHandler(
				//
						deadLetterChannel(
								"direct:osoitetarrat_hakemustenhaku_epaonnistui_deadletterchannel")
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
				.process(haeHakemuksetJaOsoitteet());

		from(osoitetarrat)
		//
				.process(security)
				//
				.choice()
				// Jos luodaan vain yksittaiselle hakemukselle...
				.when(property("hakemusOids").isNotNull())
				//
				.setBody(property("hakemusOids"))
				//
				.otherwise()
				//
				.to("direct:osoitetarrat_resolve_hakemukset")
				//
				.end()
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						dokumenttiprosessi(exchange).setKokonaistyo(
								exchange.getIn().getBody(Collection.class)
										.size() + 2);
					}
				})
				//
				.split(body(), osoiteAggregation())
				//
				.shareUnitOfWork()
				//
				// .process(haeHakemuksetJaOsoitteet()) //
				// haeOsoitteetValittamattaSaadaankoHakemusta())

				//
				// .parallelProcessing()
				//
				.stopOnException()
				//
				.to("direct:osoitetarrat_haeHakemuksetJaOsoitteet")
				//
				.end()
				//

				// enrich to Osoitteet
				.bean(new LuoOsoitteet())
				//
				.process(security)
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						DokumenttiProsessi prosessi = dokumenttiprosessi(exchange);
						Osoitteet osoitteet = exchange.getIn().getBody(
								Osoitteet.class);

						InputStream pdf;
						try {

							// LOG.error("\r\n{}",
							// new GsonBuilder().setPrettyPrinting()
							// .create().toJson(osoitteet));
							pdf = pipeInputStreams(viestintapalveluResource
									.haeOsoitetarratSync(osoitteet));

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

							dokumenttiResource.tallenna(id, "osoitetarrat.pdf",
									expirationTime, tags, "application/pdf",
									pdf);
							dokumenttiprosessi(exchange)
									.inkrementoiTehtyjaToita();
							prosessi.setDokumenttiId(id);
						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Dokumenttipalvelulle tiedonsiirrossa tapahtui virhe: {}",
									e.getMessage());
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.DOKUMENTTIPALVELU,
											"Osoitteet pdf:n tallennus dokumenttipalvelulle",
											e.getMessage()));
							throw e;
						}
					}
				});
		// .bean(viestintapalveluResource, "haeOsoitetarrat");
		// "DokumenttiTyyppi"
		from("direct:osoitetarrat_resolve_hakemukset")
				//
				.process(security)
				//
				.choice()
				//
				.when(property("DokumenttiTyyppi").isEqualTo(
						DokumenttiTyyppi.VALINTAKOKEESEEN_OSALLISTUJAT))
				//
				.process(new Processor() {
					public void process(Exchange exchange) throws Exception {
						try {
							List<HakemusOsallistuminenTyyppi> h = valintatietoHakukohteelleKomponentti
									.valintatiedotHakukohteelle(
											valintakoeOids(exchange),
											hakukohdeOid(exchange));

							List<String> hakemusOids = Lists.newArrayList();
							for (HakemusOsallistuminenTyyppi h0 : h) {
								for (ValintakoeOsallistuminenTyyppi o0 : h0
										.getOsallistumiset()) {
									if (Osallistuminen.OSALLISTUU.equals(o0
											.getOsallistuminen())) {
										// add
										hakemusOids.add(h0.getHakemusOid());
									}
								}
							}
							exchange.getOut().setBody(hakemusOids);

						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Valintatietojen haku hakukohteelle({}) epäonnistui:{}",
									hakukohdeOid(exchange), e.getMessage());
							Collection<Oid> oidit = Lists.newArrayList(Poikkeus
									.valintakoeOids(valintakoeOids(exchange)));
							oidit.add(Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.VALINTATIETO,
											"Valintatietoja ei saatu haettua hakukohteelle",
											e.getMessage(), oidit));
							throw e;
						}
					}
				})
				//
				.when(property("DokumenttiTyyppi").isEqualTo(
						DokumenttiTyyppi.SIJOITTELUSSA_HYVAKSYTYT))
				//
				//
				.process(new Processor() {
					@SuppressWarnings("unchecked")
					@Override
					public void process(Exchange exchange) throws Exception {
						List<String> l = Lists.newArrayList();
						try {

							for (HakijaDTO hakija : sijoitteluProxy
									.koulutuspaikalliset(hakuOid(exchange),
											hakukohdeOid(exchange),
											SijoitteluResource.LATEST)) {
								l.add(hakija.getHakemusOid());
							}
							exchange.getOut().setBody(l);

						} catch (Exception e) {
							e.printStackTrace();
							LOG.error(
									"Sijoittelussa hyväksyttyjä ei saatu haettua haussa({}) hakukohteelle({}): {}",
									hakuOid(exchange), hakukohdeOid(exchange),
									e.getMessage());
							Collection<Oid> oidit = Lists.newArrayList(Poikkeus
									.hakuOid(hakuOid(exchange)), Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));

							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.SIJOITTELU,
											"Sijoittelussa hyväksyttyjä ei saatu haettua hakukohteelle",
											e.getMessage(), oidit));
							throw e;
						}
						if (l.isEmpty()) {
							Collection<Oid> oidit = Lists.newArrayList(Poikkeus
									.hakuOid(hakuOid(exchange)), Poikkeus
									.hakukohdeOid(hakukohdeOid(exchange)));
							dokumenttiprosessi(exchange)
									.getPoikkeukset()
									.add(new Poikkeus(
											Poikkeus.SIJOITTELU,
											"Tässä sijoittelussa yksikään hakemus ei ollut hyväksyttynä",
											"", oidit));
							throw new RuntimeException(
									"Tässä sijoittelussa yksikään hakemus ei ollut hyväksyttynä");
						}
					}
				})
				//
				.otherwise()
				//
				.end();
	}

	private FlexibleAggregationStrategy<Osoite> osoiteAggregation() {
		return new FlexibleAggregationStrategy<Osoite>().storeInBody()
				.accumulateInCollection(ArrayList.class);
	}

	private Processor haeHakemuksetJaOsoitteet() {
		return new Processor() {
			public void process(Exchange exchange) throws Exception {
				String oid = exchange.getIn().getBody(String.class);
				Hakemus hakemus;
				try {
					hakemus = applicationResource.getApplicationByOid(oid);
				} catch (Exception e) {
					e.printStackTrace();
					dokumenttiprosessi(exchange).getPoikkeukset().add(
							new Poikkeus(Poikkeus.HAKU,
									"Hakemuspalvelulta ei saatu hakemuksia", e
											.getMessage(), Poikkeus
											.hakemusOid(oid)));
					throw e;
				}

				try {
					//
					// Koodisto ei kayta autentikaatiota. Tama tapahtuu vain jos
					// koodistopalvelu on alhaalla
					//
					exchange.getOut().setBody(
							osoiteKomponentti.haeOsoite(hakemus));

				} catch (Exception e) {
					e.printStackTrace();
					LOG.error(
							"Koodistopalvelukutsun tekevässä lohkossa tapahtui poikkeus: {}",
							e.getMessage());
					dokumenttiprosessi(exchange)
							.getPoikkeukset()
							.add(new Poikkeus(
									Poikkeus.KOODISTO,
									"Koodistopalvelukutsun tekevässä lohkossa tapahtui poikkeus",
									e.getMessage(), Poikkeus.hakemusOid(oid)));
					throw e;
				}
				//
				// Yksi työ valmistui
				//
				dokumenttiprosessi(exchange).inkrementoiTehtyjaToita();
			}
		};
	}

}
