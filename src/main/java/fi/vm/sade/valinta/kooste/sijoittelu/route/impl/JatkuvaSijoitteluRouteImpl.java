package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Component
public class JatkuvaSijoitteluRouteImpl extends RouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(JatkuvaSijoitteluRouteImpl.class);
	private final String DEADLETTERCHANNEL = "direct:jatkuvan_sijoittelun_deadletterchannel";
	private final JatkuvaSijoittelu jatkuvaSijoittelu;
	private final String quartzInput;

	@Autowired
	public JatkuvaSijoitteluRouteImpl(
			@Value("quartz://jatkuvanSijoittelunAjastin?cron=${valintalaskentakoostepalvelu.jatkuvasijoittelu.cron}") String quartzInput,
			JatkuvaSijoittelu jatkuvaSijoittelu) {
		this.quartzInput = quartzInput;
		this.jatkuvaSijoittelu = jatkuvaSijoittelu;
	}

	@Override
	public void configure() throws Exception {
		from(DEADLETTERCHANNEL)
		//
				.routeId("Jatkuvan sijoittelun deadletterchannel")
				//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						LOG.error(
								"Jatkuvasijoittelu paattyi virheeseen {}\r\n{}",
								simple("${exception.message}").evaluate(
										exchange, String.class),
								simple("${exception.stacktrace}").evaluate(
										exchange, String.class));
					}
				})
				//
				.stop();

		from(quartzInput)
		//
				.errorHandler(
						deadLetterChannel(DEADLETTERCHANNEL)
								//
								.maximumRedeliveries(0)
								//
								.logExhaustedMessageHistory(true)
								.logExhausted(true)
								// hide retry/handled stacktrace
								.logStackTrace(false).logRetryStackTrace(false)
								.logHandled(false))
				//
				.routeId("Jatkuvan sijoittelun ajastin")
				//
				.bean(jatkuvaSijoittelu);
	}

}
