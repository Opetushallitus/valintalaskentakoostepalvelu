package fi.vm.sade.valinta.kooste.sijoitteluntulos.route.impl;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.security.SecurityPreprocessor;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosHyvaksymiskirjeetRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

@Component
public class SijoittelunTulosRouteImpl extends AbstractDokumenttiRouteBuilder {

	public SijoittelunTulosRouteImpl() {

	}

	public void configure() throws Exception {
		configureTaulukkolaskenta();
		configureHyvaksymiskirjeet();
	}

	private void configureTaulukkolaskenta() {
		Endpoint taulukkolaskennatKokoHaulle = endpoint(SijoittelunTulosTaulukkolaskentaRoute.SEDA_SIJOITTELUNTULOS_TAULUKKOLASKENTA_HAULLE);
		Endpoint luontiEpaonnistui = endpoint("direct:taulukkolaskennatKokoHaulle_deadletterchannel");
		from(taulukkolaskennatKokoHaulle)
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
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {

					}
				})
				//
				.stop();
	}

	private void configureHyvaksymiskirjeet() {
		Endpoint hyvaksymiskirjeetKokoHaulle = endpoint(SijoittelunTulosHyvaksymiskirjeetRoute.SEDA_SIJOITTELUNTULOS_HYVAKSYMISKIRJEET_HAULLE);
		Endpoint luontiEpaonnistui = endpoint("direct:hyvaksymiskirjeetKokoHaulle_deadletterchannel");
		from(hyvaksymiskirjeetKokoHaulle)
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
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {

					}
				})
				//
				.stop();
	}

}
