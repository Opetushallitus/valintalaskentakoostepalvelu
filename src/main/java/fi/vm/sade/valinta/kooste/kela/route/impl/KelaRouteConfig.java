package fi.vm.sade.valinta.kooste.kela.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class KelaRouteConfig {

    @Bean(name = "kelaValvomo")
    public ValvomoServiceImpl<KelaProsessi> getValvomoServiceImpl() {
        return new ValvomoServiceImpl<KelaProsessi>();
    }

    @Bean
    public KelaRoute getKelaRoute(
            @Value(KelaRoute.SEDA_KELA_LUONTI) String kelaluonti,
            @Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint(kelaluonti), KelaRoute.class);
    }

    @Bean
    public KelaFtpRoute getKelaFtpRoute(
            @Value(KelaRoute.KELA_SIIRTO) String kelasiirto,
            @Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint(kelasiirto), KelaFtpRoute.class);
    }
}
