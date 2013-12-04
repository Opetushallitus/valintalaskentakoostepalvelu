package fi.vm.sade.valinta.kooste;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Header;
import org.apache.camel.Producer;
import org.apache.camel.Property;
import org.apache.camel.component.bean.AbstractCamelInvocationHandler;
import org.apache.camel.component.bean.BeanInvocation;
import org.apache.camel.component.bean.MethodInfo;
import org.apache.camel.component.bean.MethodInfoCache;
import org.apache.camel.impl.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class CamelWithAnnotationInvocationHandler extends AbstractCamelInvocationHandler implements InvocationHandler {

    private final Logger LOG = LoggerFactory.getLogger(CamelWithAnnotationInvocationHandler.class);
    private final MethodInfoCache methodInfoCache;

    public CamelWithAnnotationInvocationHandler(Endpoint endpoint, Producer producer, MethodInfoCache methodInfoCache) {
        super(endpoint, producer);
        this.methodInfoCache = methodInfoCache;
    }

    @Override
    public Object doInvokeProxy(Object proxy, Method method, Object[] args) throws Throwable {

        MethodInfo methodInfo = methodInfoCache.getMethodInfo(method);
        final ExchangePattern pattern = methodInfo != null ? methodInfo.getPattern() : ExchangePattern.InOut;
        final Exchange exchange = new DefaultExchange(endpoint, pattern);

        List<Object> bodyArgs = Lists.newArrayListWithExpectedSize(args.length);
        for (int i = 0; i < args.length; ++i) {
            Object value = args[i];
            Annotation[] annotationsInArg = method.getParameterAnnotations()[i];
            if (!updateExchange(value, exchange, annotationsInArg)) {
                bodyArgs.add(value);
            }
        }
        if (bodyArgs.isEmpty()) {
            exchange.getIn().setBody(null);
        } else if (bodyArgs.size() == 1) {
            exchange.getIn().setBody(bodyArgs.get(0));
        } else {
            BeanInvocation invocation = new BeanInvocation(method, bodyArgs.toArray());
            exchange.getIn().setBody(invocation);
        }
        return invokeWithExchange(method, exchange);
    }

    /**
     * @param value
     * @param exchange
     * @param annotations
     * @return true if value could be added to exchange
     */
    private boolean updateExchange(Object value, Exchange exchange, Annotation[] annotations) {
        boolean valueAdded = false;
        for (Annotation annotation : annotations) {
            if (annotation instanceof Property) {
                Property prop = (Property) annotation;
                exchange.setProperty(prop.value(), value);
                valueAdded = true;
            } else if (annotation instanceof Header) {
                Header header = (Header) annotation;
                exchange.getIn().setHeader(header.value(), value);
                valueAdded = true;
            }
        }
        return valueAdded;
    }

    protected Object invokeWithExchange(final Method method, final Exchange exchange) throws Throwable {
        // is the return type a future
        final boolean isFuture = method.getReturnType() == Future.class;

        // create task to execute the proxy and gather the reply
        FutureTask<Object> task = new FutureTask<Object>(new Callable<Object>() {
            public Object call() throws Exception {
                // process the exchange
                LOG.trace("Proxied method call {} invoking producer: {}", method.getName(), producer);
                producer.process(exchange);

                Object answer = afterInvoke(method, exchange, exchange.getPattern(), isFuture);
                LOG.trace("Proxied method call {} returning: {}", method.getName(), answer);
                return answer;
            }
        });

        if (isFuture) {
            // submit task and return future
            if (LOG.isTraceEnabled()) {
                LOG.trace("Submitting task for exchange id {}", exchange.getExchangeId());
            }
            getExecutorService(exchange.getContext()).submit(task);
            return task;
        } else {
            // execute task now
            try {
                task.run();
                return task.get();
            } catch (ExecutionException e) {
                // we don't want the wrapped exception from JDK
                throw e.getCause();
            }
        }
    }
}
