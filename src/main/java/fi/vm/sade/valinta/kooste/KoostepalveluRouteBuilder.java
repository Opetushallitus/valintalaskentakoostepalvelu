package fi.vm.sade.valinta.kooste;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.spring.SpringRouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public abstract class KoostepalveluRouteBuilder<T> extends SpringRouteBuilder {

	private static final Logger LOG = LoggerFactory
			.getLogger(KoostepalveluRouteBuilder.class);

	/**
	 * 
	 * @return default deadletterchannel endpoint
	 */
	protected abstract String deadLetterChannelEndpoint();

	private final Cache<String, T> koostepalveluCache = configureCache();

	protected Cache<String, T> configureCache() {
		return CacheBuilder.newBuilder().weakValues()
				.expireAfterWrite(3, TimeUnit.HOURS)
				.removalListener(new RemovalListener<String, T>() {
					public void onRemoval(
							RemovalNotification<String, T> notification) {
						LOG.info("{} siivottu pois muistista",
								notification.getValue());
					}
				}).build();
	}

	protected Cache<String, T> getKoostepalveluCache() {
		return koostepalveluCache;
	}

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
