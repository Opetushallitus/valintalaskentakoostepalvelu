package fi.vm.sade.valinta.kooste.hakuimport.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.haku.dto.HakuImportProsessi;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakuImportRoute;
import fi.vm.sade.valinta.kooste.hakuimport.route.HakukohdeImportRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;
import org.springframework.context.annotation.Profile;

@Profile("default")
@Configuration
public class HakuImportRouteConfig {

    @Bean(name = "hakuImportValvomo")
    public ValvomoServiceImpl<HakuImportProsessi> getValvomoServiceImpl() {
        return new ValvomoServiceImpl<>();
    }

    @Bean
    public HakuImportRoute getHakuImportAktivointiRoute(@Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint(HakuImportRoute.DIRECT_HAKU_IMPORT),
                HakuImportRoute.class);
    }

    @Bean
    public HakukohdeImportRoute getHakukohdeImportAktivointiRoute(@Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context.getEndpoint(HakukohdeImportRoute.DIRECT_HAKUKOHDE_IMPORT),
                HakukohdeImportRoute.class);
    }
}
