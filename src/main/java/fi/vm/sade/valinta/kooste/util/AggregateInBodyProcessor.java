package fi.vm.sade.valinta.kooste.util;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Camelin versiossa 2.12.2 on viela splitterin kanssa bugi jonka
 *         seurauksena AbstractListAggregationStrategy:n onCompletion-metodia ei
 *         kutsuta aggregoinnin lopussa. Tehdaan EXCHANGE.GROUPED_AGGREGATION
 *         propertyn siirto bodyyn prosessorilla splittauksen jalkeen.
 */
@Component("aggregateInBody")
public class AggregateInBodyProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(AggregateInBodyProcessor.class);

    public void process(Exchange exchange) throws Exception {
        List<?> list = (List<?>) exchange.removeProperty(Exchange.GROUPED_EXCHANGE);
        if (list != null) {
            if (!list.isEmpty() && list.get(0) != null) {
                if (list.get(0) instanceof DefaultExchange) {
                    List<Object> bodies = new ArrayList<Object>();
                    for (DefaultExchange e : (List<DefaultExchange>) list) {
                        bodies.add(e.getIn().getBody());
                    }
                    exchange.getOut().setBody(bodies);
                } else {
                    exchange.getOut().setBody(list); // not
                                                     // List<DefaultExchange>
                }
                LOG.info("Setting collection with size {} as grouped exchange body. Collection type List<{}>.",
                        new Object[] { list.size(), list.get(0).getClass() });

            } else {
                LOG.info("Setting empty collection as grouped exchange body!");
                exchange.getOut().setBody(list);
            }

        } else {
            LOG.error("No grouped exchange found!");
        }
    }
}
