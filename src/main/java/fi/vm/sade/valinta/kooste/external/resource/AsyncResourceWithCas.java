package fi.vm.sade.valinta.kooste.external.resource;

import org.springframework.context.ApplicationContext;

public class AsyncResourceWithCas extends AsyncResource {
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
