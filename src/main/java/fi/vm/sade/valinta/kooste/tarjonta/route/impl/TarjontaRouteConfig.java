package fi.vm.sade.valinta.kooste.tarjonta.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.tarjonta.route.LinjakoodiRoute;
import fi.vm.sade.valinta.kooste.tarjonta.route.OrganisaatioRoute;
import fi.vm.sade.valinta.kooste.tarjonta.route.TarjontaHakuRoute;
import fi.vm.sade.valinta.kooste.tarjonta.route.TarjontaNimiRoute;
import org.springframework.context.annotation.Profile;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Profile("default")
@Configuration
public class TarjontaRouteConfig {

    @Bean
    public LinjakoodiRoute getLinjakoodiRoute(@Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint("direct:linjakoodiReitti"),
                LinjakoodiRoute.class);
    }

    @Bean
    public OrganisaatioRoute getOrganisaatioRoute(@Qualifier("javaDslCamelContext") CamelContext context)
            throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint("direct:organisaatioReitti"),
                OrganisaatioRoute.class);
    }

    @Bean
    public TarjontaHakuRoute getTarjontaHakuRoute(@Qualifier("javaDslCamelContext") CamelContext context)
            throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint("direct:tarjontaHakuReitti"),
                TarjontaHakuRoute.class);
    }

    @Bean
    public TarjontaNimiRoute getTarjontaNimiRoute(@Qualifier("javaDslCamelContext") CamelContext context)
            throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint(TarjontaNimiRoute.DIRECT_TARJONTA_NIMI),
                TarjontaNimiRoute.class);
    }
}
