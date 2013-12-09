package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;

@Configuration
public class HakuImportRouteConfig {

    // @Bean(name = "hakuImportValvomo")
    // public ValvomoServiceImpl<KelaProsessi> getValvomoServiceImpl() {
    // return new ValvomoServiceImpl<KelaProsessi>();
    // }

    @Bean
    public HakuImportRoute getHakuImportAktivointiRoute(@Qualifier("javaDslCamelContext") CamelContext context)
            throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint(HakuImportRoute.DIRECT_HAKU_IMPORT),
                HakuImportRoute.class);
    }
}
