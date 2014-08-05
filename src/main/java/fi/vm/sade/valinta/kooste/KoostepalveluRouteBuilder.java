package fi.vm.sade.valinta.kooste;

import java.util.function.Function;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.spring.SpringRouteBuilder;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public abstract class KoostepalveluRouteBuilder extends SpringRouteBuilder {

	/**
	 * 
	 * @return default deadletterchannel endpoint
	 */
	protected abstract String deadLetterChannelEndpoint();

	/**
	 * 
	 * @return default dead letter channel
	 */
	protected DefaultErrorHandlerBuilder deadLetterChannel() {
		return deadLetterChannel(deadLetterChannelEndpoint())
				//
				.maximumRedeliveries(0)
				//
				.logExhaustedMessageHistory(true).logExhausted(true)
				// hide retry/handled stacktrace
				.logStackTrace(false).logRetryStackTrace(false)
				.logHandled(false);
	}

}
