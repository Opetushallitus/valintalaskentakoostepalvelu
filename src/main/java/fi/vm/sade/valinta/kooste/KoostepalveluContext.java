package fi.vm.sade.valinta.kooste;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import fi.vm.sade.authentication.business.service.Authorizer;
import fi.vm.sade.authentication.cas.CasFriendlyCache;
import fi.vm.sade.authentication.cas.CasFriendlyCxfInterceptor;
import fi.vm.sade.security.OidProvider;
import fi.vm.sade.security.OrganisationHierarchyAuthorizer;
import fi.vm.sade.security.ThreadLocalAuthorizer;
import fi.vm.sade.valinta.kooste.converter.HakemusToHakemusDTOConverter;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.ThreadPoolRejectedPolicy;
import org.apache.camel.spi.ThreadPoolProfile;
import org.apache.camel.spring.SpringCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

@Configuration
@Import({KelaRouteConfig.class, KoostepalveluContext.CamelConfig.class, JaxrsConfiguration.class})
public class KoostepalveluContext {
    static final String TYHJA_ARVO_POIKKEUS = "Reititysta ei voida jatkaa tyhjalle arvolle!";

    private static final Logger LOG = LoggerFactory.getLogger(KoostepalveluContext.class);

    @Bean(name = "sessionCache")
    public CasFriendlyCache getCasFriendlyCache() {
        return new CasFriendlyCache();
    }

    @Bean(name = "casTicketInterceptor")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public CasFriendlyCxfInterceptor<?> getCasFriendlyCxfInterceptor(CasFriendlyCache casCache) {
        return new CasFriendlyCxfInterceptor<>();
    }

    @Bean
    public OidProvider getOidProvider() {
        return new OidProvider();
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
    @Profile("default")
    @Configuration
    public static class CamelConfig {

        @Bean(name = "javaDslCamelContext")
        public static SpringCamelContext getSpringCamelContext(
                @Value("${valintalaskentakoostepalvelu.camelContext.threadpoolsize:10}") Integer threadPoolSize,
                ApplicationContext applicationContext, RoutesBuilder[] routes)
                throws Exception {
            SpringCamelContext camelContext = new SpringCamelContext(applicationContext);
            camelContext.getTypeConverterRegistry().addTypeConverter(HakemusDTO.class, Hakemus.class, new HakemusToHakemusDTOConverter());
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
