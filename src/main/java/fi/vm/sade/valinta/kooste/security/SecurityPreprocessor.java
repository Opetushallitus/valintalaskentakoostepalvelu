package fi.vm.sade.valinta.kooste.security;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @author Jussi Jartamo
 *         <p/>
 *         Asentaa autentikaation workerille jos tarpeen ja taltioi
 *         autentikaation propertyyn workeria varten.
 */
@Component
public class SecurityPreprocessor implements Processor {

	public static final String SECURITY_CONTEXT_HEADER = "authentication";

	private static final Logger LOG = LoggerFactory
			.getLogger(SecurityPreprocessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		exchange.setOut(exchange.getIn());
		Authentication currentAuth = SecurityContextHolder.getContext()
				.getAuthentication();
		if (currentAuth == null) {
			// should current auth be null
			Authentication newAuth = (Authentication) exchange
					.getProperty(SecurityPreprocessor.SECURITY_CONTEXT_HEADER);
			assert (newAuth != null); // <- should never be null!

			SecurityContextHolder.getContext().setAuthentication(newAuth);
		} else {
			exchange.setProperty(SECURITY_CONTEXT_HEADER, currentAuth);
		}

	}
}
