package fi.vm.sade.valinta.kooste.util;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

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

    public void process(Exchange exchange) throws Exception {
        List<?> list = (List<?>) exchange.removeProperty(Exchange.GROUPED_EXCHANGE);
        if (list != null) {
            exchange.getIn().setBody(list);
        }
    }
}
