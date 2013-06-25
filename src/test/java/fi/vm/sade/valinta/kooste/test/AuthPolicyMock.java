package fi.vm.sade.valinta.kooste.test;

import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.Policy;
import org.apache.camel.spi.RouteContext;

/**
 * Created with IntelliJ IDEA.
 * User: jukais
 * Date: 25.6.2013
 * Time: 12.17
 * To change this template use File | Settings | File Templates.
 */
public class AuthPolicyMock implements Policy {

    @Override
    public void beforeWrap(RouteContext routeContext, ProcessorDefinition<?> definition) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Processor wrap(RouteContext routeContext, Processor processor) {
        return processor;
    }
}
