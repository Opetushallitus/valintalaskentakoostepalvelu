package fi.vm.sade.valinta.kooste.kela.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

/**
 * @author Jussi Jartamo.
 */
@Configuration
//
// @ComponentScan(basePackageClasses = KelaRouteConfig.class)
public abstract class KelaRouteConfig {

    @Bean(name = "kelaValvomo")
    public ValvomoServiceImpl<KelaProsessi> getValvomoServiceImpl() {
        return new ValvomoServiceImpl<KelaProsessi>();
    }

    @Bean
    public KelaRoute getKelaRoute(@Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        // return new
        // ProxyBuilder(context).endpoint(KelaRoute.DIRECT_KELA_LUONTI).build(KelaRoute.class);
        return ProxyWithAnnotationHelper
                .createProxy(context.getEndpoint(KelaRoute.DIRECT_KELA_LUONTI), KelaRoute.class);
    }

    @Bean
    public KelaFtpRoute getKelaFtpRoute(@Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint(KelaRoute.DIRECT_KELA_SIIRTO),
                KelaFtpRoute.class);
    }
}
