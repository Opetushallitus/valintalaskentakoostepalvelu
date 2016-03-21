package fi.vm.sade.valinta.kooste.external.resource;

import com.google.common.collect.Lists;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.springframework.context.ApplicationContext;

import fi.vm.sade.valinta.http.HttpResource;

import java.util.List;

public class AsyncResourceWithCas extends HttpResource {
    private static JAXRSClientFactoryBean asennaCasFilter(AbstractPhaseInterceptor casInterceptor, JAXRSClientFactoryBean bean, ApplicationContext context) {
        if ("default".equalsIgnoreCase(System.getProperty("spring.profiles.active", "default"))) {
            List<Interceptor<? extends Message>> interceptors = Lists.newArrayList();
            interceptors.add(casInterceptor);
            bean.setOutInterceptors(interceptors);
            bean.setInInterceptors(interceptors);
        }
        return bean;
    }

    public AsyncResourceWithCas(
        AbstractPhaseInterceptor casInterceptor,
        String address,
        ApplicationContext context,
        long timeoutMillis
    ) {
        super(address, asennaCasFilter(casInterceptor, getJaxrsClientFactoryBean(address), context).createWebClient(), timeoutMillis);
    }
}
