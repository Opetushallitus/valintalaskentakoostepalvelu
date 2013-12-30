package fi.vm.sade.valinta.kooste.valintatieto.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.valintatieto.route.ValintatietoHakukohteelleRoute;
import fi.vm.sade.valinta.kooste.valintatieto.route.ValintatietoRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class ValintatietoRouteConfig {

    @Bean(name = "valintatietoValvomo")
    public ValvomoServiceImpl<Prosessi> getValvomoServiceImpl() {
        return new ValvomoServiceImpl<Prosessi>();
    }

    @Bean
    public ValintatietoRoute getValintatietoRoute(@Qualifier("javaDslCamelContext") CamelContext context)
            throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint("direct:valintatietoReitti"),
                ValintatietoRoute.class);
    }

    @Bean
    public ValintatietoHakukohteelleRoute getValintatietoHakukohteelleRoute(
            @Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint("direct:valintatietoHakukohteelleReitti"),
                ValintatietoHakukohteelleRoute.class);
    }
}
