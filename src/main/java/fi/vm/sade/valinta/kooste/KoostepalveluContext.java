package fi.vm.sade.valinta.kooste;

import fi.vm.sade.authentication.business.service.Authorizer;
import fi.vm.sade.security.OidProvider;
import fi.vm.sade.security.OrganisationHierarchyAuthorizer;
import fi.vm.sade.security.ThreadLocalAuthorizer;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.spring.SpringCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@Import({KelaRouteConfig.class, KoostepalveluContext.CamelConfig.class, JaxrsConfiguration.class})
public class KoostepalveluContext {
    static final String TYHJA_ARVO_POIKKEUS = "Reititysta ei voida jatkaa tyhjalle arvolle!";

    private static final Logger LOG = LoggerFactory.getLogger(KoostepalveluContext.class);

    @Bean
    public OidProvider getOidProvider(@Value("valintalaskentakoostelvelu.organisaatio-service-url") String organisaatioServiceUrl) {
        return new OidProvider(organisaatioServiceUrl);
    }

    @Bean
    public OrganisationHierarchyAuthorizer getOrganisationHierarchyAuthorizer() {
        return new OrganisationHierarchyAuthorizer();
    }

    @Bean(name = "authorizer")
    public Authorizer getAuthorizerImpl(OrganisationHierarchyAuthorizer organisationHierarchyAuthorizer) {
        return new ThreadLocalAuthorizer();
    }

    /**
     * Camel only Context (helps unit testing).
     */
    @Configuration
    public static class CamelConfig {

        @Bean(name = "javaDslCamelContext")
        public static SpringCamelContext getSpringCamelContext(
                @Value("${valintalaskentakoostepalvelu.camelContext.threadpoolsize:10}") Integer threadPoolSize,
                ApplicationContext applicationContext, RoutesBuilder[] routes)
                throws Exception {
            SpringCamelContext camelContext = new SpringCamelContext(applicationContext);
//            camelContext.getTypeConverterRegistry().addTypeConverter(HakemusDTO.class, Hakemus.class, new HakemusToHakemusDTOConverter());
            camelContext.disableJMX();
            camelContext.setAutoStartup(true);
            for (RoutesBuilder route : routes) {
                camelContext.addRoutes(route);
            }
            return camelContext;
        }

        @Bean
        public static PropertySourcesPlaceholderConfigurer propertyPlaceholderConfigurer() {
            return new PropertySourcesPlaceholderConfigurer();
        }
    }
}
