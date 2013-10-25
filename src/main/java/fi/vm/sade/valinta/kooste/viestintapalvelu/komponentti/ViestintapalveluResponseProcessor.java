package fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti;

import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import fi.vm.sade.valinta.kooste.exception.ViestintapalveluException;

@Component("viestintapalveluProcessor")
public class ViestintapalveluResponseProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(ViestintapalveluResponseProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        if (exchange.getIn().getBody() instanceof Response) {
            Response response = (Response) exchange.getIn().getBody();
            if (response.getStatus() == 302) { // FOUND
                throw new ViestintapalveluException("Sinulla ei ole käyttöoikeuksia viestintäpalveluun!");
            }
            if (response.getStatus() != Response.Status.ACCEPTED.getStatusCode()) {
                LOG.error("Response {}, \r\n{}, \r\n{}, \r\n{}",
                        new Object[] { response.getStatus(), response.getEntity(), response.getMetadata(), response });

                throw new ViestintapalveluException("Viestintäpalvelu epäonnistui (status " + response.getStatus()
                        + ") osoitetarrojen luonnissa. Yritä uudelleen tai ota yhteyttä ylläpitoon!");
            }
        }
        exchange.setOut(exchange.getIn());
    }
}
