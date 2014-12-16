package fi.vm.sade.valinta.kooste.external.resource;

import org.springframework.context.ApplicationContext;

import fi.vm.sade.valinta.http.HttpResource;

public class AsyncResourceWithCas extends HttpResource {
    public AsyncResourceWithCas(
        String webCasUrl,
        String targetService,
        String appClientUsername,
        String appClientPassword,
        String address,
        ApplicationContext context,
        long timeoutMillis
    ) {
        super(address, AsennaCasFilter.asennaCasFilter(webCasUrl, targetService, appClientUsername, appClientPassword, getJaxrsClientFactoryBean(address), context).createWebClient(), timeoutMillis);
    }
}
