package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluIlmankoulutuspaikkaaRoute;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluKaikkiKoulutuspaikallisetRoute;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluKoulutuspaikallisetRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class SijoitteluRouteConfig {

    @Bean
    public SijoitteluAktivointiRoute getSijoitteluAktivointiRoute(@Qualifier("javaDslCamelContext") CamelContext context)
            throws Exception {
        return ProxyWithAnnotationHelper.createProxy(
                context.getEndpoint(SijoitteluAktivointiRoute.DIRECT_SIJOITTELU_AKTIVOI),
                SijoitteluAktivointiRoute.class);
    }

    @Bean
    public SijoitteluIlmankoulutuspaikkaaRoute getSijoitteluIlmankoulutuspaikkaaRoute(
            @Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(
                context.getEndpoint(SijoitteluIlmankoulutuspaikkaaRoute.DIRECT_SIJOITTELU_ILMAN_KOULUTUSPAIKKAA),
                SijoitteluIlmankoulutuspaikkaaRoute.class);
    }

    @Bean
    public SijoitteluKaikkiKoulutuspaikallisetRoute getSijoitteluKaikkiKoulutuspaikallisetRoute(
            @Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(context
                .getEndpoint(SijoitteluKaikkiKoulutuspaikallisetRoute.DIRECT_SIJOITTELU_KAIKKI_KOULUTUSPAIKALLISET),
                SijoitteluKaikkiKoulutuspaikallisetRoute.class);
    }

    @Bean
    public SijoitteluKoulutuspaikallisetRoute getSijoitteluKoulutuspaikallisetRoute(
            @Qualifier("javaDslCamelContext") CamelContext context) throws Exception {
        return ProxyWithAnnotationHelper.createProxy(
                context.getEndpoint(SijoitteluKoulutuspaikallisetRoute.DIRECT_SIJOITTELU_KAIKKI_KOULUTUSPAIKALLISET),
                SijoitteluKoulutuspaikallisetRoute.class);
    }

}
