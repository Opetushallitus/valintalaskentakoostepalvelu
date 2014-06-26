package fi.vm.sade.valinta.kooste;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.valinta.kooste.converter.HakemusToHakemusTyyppiConverter;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;

/**
 * @author Jussi Jartamo.
 */
@Configuration
@Import({ KelaRouteConfig.class, KoostepalveluContext.CamelConfig.class })
public class KoostepalveluContext {

	private static final Logger LOG = LoggerFactory
			.getLogger(KoostepalveluContext.class);

	/**
	 * Camel only Context (helps unit testing).
	 */
	@Configuration
	public static class CamelConfig {

		/**
		 * @param applicationContext
		 * @param routes
		 * @return Spring Camel Context koko koostepalvelulle refaktoroinnin
		 *         jalkeen!
		 * @throws Exception
		 */
		@Bean(name = "javaDslCamelContext")
		public static SpringCamelContext getSpringCamelContext(
				@Value("${valintalaskentakoostepalvelu.camelContext.threadpoolsize:10}") Integer threadPoolSize,
				ApplicationContext applicationContext, RoutesBuilder[] routes)
				throws Exception {
			SpringCamelContext camelContext = new SpringCamelContext(
					applicationContext);
			ThreadPoolProfile t = new ThreadPoolProfile();
			t.setId("ValintalaskentakoostepalveluThreadPool");
			t.setDefaultProfile(true);
			t.setPoolSize(threadPoolSize);
			t.setMaxPoolSize(threadPoolSize);
			t.setMaxQueueSize(10000);
			t.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
			// defaultThreadPoolProfile
			camelContext.getExecutorServiceManager()
					.setDefaultThreadPoolProfile(t);
			// <threadPoolProfile id="defaultThreadPoolProfile"
			// defaultProfile="true"
			// poolSize="10" maxPoolSize="20" maxQueueSize="1000"
			// rejectedPolicy="CallerRuns"/>
			//
			// Hakemus -> HakemusTyyppi Converter
			camelContext.getTypeConverterRegistry().addTypeConverter(
					HakemusTyyppi.class, Hakemus.class,
					new HakemusToHakemusTyyppiConverter());

            camelContext.getTypeConverterRegistry().addTypeConverter(
                    HakemusDTO.class, Hakemus.class,
                    new HakemusToHakemusDTOConverter());

			// camelContext.disableJMX();
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
