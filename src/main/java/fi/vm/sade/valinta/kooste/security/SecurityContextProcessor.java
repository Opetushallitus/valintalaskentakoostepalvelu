package fi.vm.sade.valinta.kooste.security;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityContextProcessor implements Processor {

    public static final String SECURITY_CONTEXT_HEADER = "authentication";

    private static final Logger LOG = LoggerFactory.getLogger(SecurityContextProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {

        exchange.setOut(exchange.getIn());
        exchange.setProperty(SECURITY_CONTEXT_HEADER, SecurityContextHolder.getContext().getAuthentication());

    }
}
