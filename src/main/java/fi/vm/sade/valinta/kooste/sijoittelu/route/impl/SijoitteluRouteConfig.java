package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import java.util.concurrent.DelayQueue;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoitteluExchange;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class SijoitteluRouteConfig {

    @Bean
    public JatkuvaSijoitteluRouteImpl getJatkuvaSijoitteluRouteImpl(
            @Value("timer://jatkuvaSijoitteluTimer?${valintalaskentakoostepalvelu.jatkuvasijoittelu.timer:fixedRate=true&period=5minutes}") String jatkuvaSijoitteluTimer,
            @Value("seda:jatkuvaSijoitteluAjo?purgeWhenStopping=true&waitForTaskToComplete=Never&concurrentConsumers=1&queue=#jatkuvaSijoitteluDelayedQueue") String jatkuvaSijoitteluQueue,
            SijoitteleAsyncResource sijoitteluAsyncResource,
            SijoittelunSeurantaResource sijoittelunSeurantaResource,
            @Qualifier("jatkuvaSijoitteluDelayedQueue") DelayQueue<DelayedSijoitteluExchange> jatkuvaSijoitteluDelayedQueue
    ) {
        return new JatkuvaSijoitteluRouteImpl(jatkuvaSijoitteluTimer, jatkuvaSijoitteluQueue, sijoitteluAsyncResource, sijoittelunSeurantaResource, jatkuvaSijoitteluDelayedQueue);
    }

    @Bean(name = "jatkuvaSijoitteluDelayedQueue")
    public DelayQueue<DelayedSijoitteluExchange> createDelayQueue() {
        return new DelayQueue<>();
    }

    @Bean
    public SijoitteluAktivointiRoute getSijoitteluAktivointiRoute(
            @Qualifier("javaDslCamelContext") CamelContext context,
            @Value(SijoitteluAktivointiRoute.SIJOITTELU_REITTI) String sijoitteluAktivoi) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint(sijoitteluAktivoi), SijoitteluAktivointiRoute.class);
    }
}
