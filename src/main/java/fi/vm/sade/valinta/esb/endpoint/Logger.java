/**
 * 
 */
package fi.vm.sade.valinta.esb.endpoint;



import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.cxf.message.MessageContentsList;


public class Logger implements Processor {


    @Override
    public void process(Exchange exchange) throws Exception {
        System.out.println("LOG=====");
        System.out.println(exchange.getIn().getBody());
        System.out.println("=====END");
    }
}
