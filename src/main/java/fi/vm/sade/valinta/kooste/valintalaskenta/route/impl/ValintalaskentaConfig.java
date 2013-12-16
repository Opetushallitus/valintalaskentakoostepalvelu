package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HakukohteenValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

/**
 * @author Jussi Jartamo.
 */
@Configuration
public class ValintalaskentaConfig {

    @Bean(name = "valintalaskentaValvomo")
    public ValvomoServiceImpl<Object> getValvomoServiceImpl() {
        return new ValvomoServiceImpl<Object>();
    }

    @Bean
    public HaunValintalaskentaRoute getValintalaskentaHaulleRoute(@Qualifier("javaDslCamelContext") CamelContext context)
            throws Exception {
        return ProxyWithAnnotationHelper.createProxy(
                context.getEndpoint(HaunValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAULLE),
                HaunValintalaskentaRoute.class);
    }

    @Bean
    public HakukohteenValintalaskentaRoute getValintalaskentaHakukohteelleRoute(
            @Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(
                context.getEndpoint(HakukohteenValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAKUKOHTEELLE),
                HakukohteenValintalaskentaRoute.class);
    }
}
