package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.toolbox.FlexibleAggregationStrategy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.google.common.collect.Collections2;

import fi.vm.sade.service.valintaperusteet.resource.ValintakoeResource;
import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.dokumenttipalvelu.dto.Message;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeHakemuksilleRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class KoekutsukirjeRouteImpl extends SpringRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(KoekutsukirjeRouteImpl.class);
	private final ViestintapalveluResource viestintapalveluResource;
	private final KoekutsukirjeetKomponentti koekutsukirjeetKomponentti;
	private final ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti;
	private final ApplicationResource applicationResource;
	private final DokumenttiResource dokumenttiResource;
	private final SecurityPreprocessor security = new SecurityPreprocessor();
	private final ValintakoeResource valintakoeResource;

	@Autowired
	public KoekutsukirjeRouteImpl(
			@Qualifier("dokumenttipalveluRestClient") DokumenttiResource dokumenttiResource,
			ValintakoeResource valintakoeResource,
			ViestintapalveluResource viestintapalveluResource,
			KoekutsukirjeetKomponentti koekutsukirjeetKomponentti,
			ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti,
			ApplicationResource applicationResource) {
		this.valintakoeResource = valintakoeResource;
		this.viestintapalveluResource = viestintapalveluResource;
		this.koekutsukirjeetKomponentti = koekutsukirjeetKomponentti;
		this.valintatietoHakukohteelleKomponentti = valintatietoHakukohteelleKomponentti;
		this.applicationResource = applicationResource;
		this.dokumenttiResource = dokumenttiResource;
	}

	@Override
	public void configure() throws Exception {
		from(kirjeidenLuontiEpaonnistui())
		//
				.log(LoggingLevel.ERROR,
						"Koekutsukirjeiden luonti epaonnistui: ${property.CamelExceptionCaught}")
				//
				.setBody(
						constant(new Message(
								"Koekutsukirjeen luonti epäonnistui. Ota yhteys ylläpitoon.",
								Arrays.asList("valintalaskentakoostepalvelu",
										"koekutsukirje"), DateTime.now()
										.plusDays(1).toDate())))
				//
				.bean(dokumenttiResource, "viesti");
		//
		from("direct:koekutsukirjeet_hae_valintatiedot_hakemuksille")
				.bean(valintatietoHakukohteelleKomponentti)
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						@SuppressWarnings("unchecked")
						List<HakemusOsallistuminenTyyppi> unfiltered = (List<HakemusOsallistuminenTyyppi>) exchange
								.getIn().getBody();
						Collection<HakemusOsallistuminenTyyppi> filtered;
						exchange.getOut()
								.setBody(
										filtered = Collections2
												.filter(unfiltered,
														new com.google.common.base.Predicate<HakemusOsallistuminenTyyppi>() {
															public boolean apply(
																	HakemusOsallistuminenTyyppi o) {
																for (ValintakoeOsallistuminenTyyppi o1 : o
																		.getOsallistumiset()) {
																	if (Osallistuminen.OSALLISTUU
																			.equals(o1
																					.getOsallistuminen())) {
																		return true;
																	}
																}
																return false;
															}
														}));
						LOG.info("Osallistumattomien pois filtterointi: {}/{}",
								filtered.size(), unfiltered.size());
					}
				})
				//
				.split(body(), hakemusAggregation())
				// HakemusOsallistuminenTyyppi -> Hakemus
				// Pitaisiko tehda oma convertteri yleisille kaannoksille?
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						HakemusOsallistuminenTyyppi osallistuminen = exchange
								.getIn().getBody(
										HakemusOsallistuminenTyyppi.class);
						exchange.getOut().setBody(
								applicationResource
										.getApplicationByOid(osallistuminen
												.getHakemusOid()));
					}
				})
				//
				.end();

		from(hakemusOiditHakemuksiksi())
		//
				.errorHandler(
				//
						deadLetterChannel(kirjeidenLuontiEpaonnistui())
								//
								.maximumRedeliveries(5)
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(security)
				//
				.bean(applicationResource, "getApplicationByOid");

		from(hakemuksilleKoekutsukirjeet())
		//
				.errorHandler(
				//
						deadLetterChannel(kirjeidenLuontiEpaonnistui())
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(security)
				//
				.split(property("hakemusOids"),
						new FlexibleAggregationStrategy<Hakemus>()
								.storeInBody().accumulateInCollection(
										ArrayList.class))
				//
				.shareUnitOfWork()
				//
				.parallelProcessing()
				//
				.stopOnException()
				//
				.to(hakemusOiditHakemuksiksi())
				//
				.end()
				//
				.to(koekutsukirjeetHakemuksista());

		from(koekutsukirjeet())
		//
				.errorHandler(
				//
						deadLetterChannel(kirjeidenLuontiEpaonnistui())
								.logExhaustedMessageHistory(true)
								.logExhausted(true).logStackTrace(true)
								// hide retry/handled stacktrace
								.logRetryStackTrace(false).logHandled(false))
				//
				.process(security)
				//
				.choice()
				// Jos luodaan vain yksittaiselle hakemukselle...
				.when(property("hakemusOids").isNotNull())
				//
				// ... tehdaankin koekutsukirjeet yksittaisille hakemuksille
				.to(hakemuksilleKoekutsukirjeet())
				//
				.otherwise() // ...muuten
				// ...haetaan kaikille osallistujille
				.to("direct:koekutsukirjeet_hae_valintatiedot_hakemuksille")
				//
				.to(koekutsukirjeetHakemuksista())
				//
				.end();

		from(koekutsukirjeetHakemuksista())
		//
				.bean(koekutsukirjeetKomponentti)
				//
				.bean(viestintapalveluResource, "vieKoekutsukirjeet")
				//
				.setBody(
						constant(new Message(
								"Koekutsukirjeen tiedot kerätty onnistuneesti. Lähetetään tiedot viestintäpalvelulle.",
								Arrays.asList("valintalaskentakoostepalvelu",
										"koekutsukirje"), DateTime.now()
										.plusDays(1).toDate())))
				//
				.bean(dokumenttiResource, "viesti");

	}

	private String koekutsukirjeetHakemuksista() {
		return "direct:koekutsukirjeet_hakemuksista";
	}

	private String hakemusOiditHakemuksiksi() {
		return "direct:koekutsukirjeet_hakemusoidit_hakemuksiksi";
	}

	private String kirjeidenLuontiEpaonnistui() {
		return "direct:koekutsukirjeet_epaonnistui";
	}

	private String hakemuksilleKoekutsukirjeet() {
		return KoekutsukirjeHakemuksilleRoute.DIRECT_KOEKUTSUKIRJEET_HAKEMUKSILLE;
	}

	private String koekutsukirjeet() {
		return KoekutsukirjeRoute.DIRECT_KOEKUTSUKIRJEET;
	}

	private FlexibleAggregationStrategy<Hakemus> hakemusAggregation() {
		return new FlexibleAggregationStrategy<Hakemus>().storeInBody()
				.accumulateInCollection(ArrayList.class);
	}

}
