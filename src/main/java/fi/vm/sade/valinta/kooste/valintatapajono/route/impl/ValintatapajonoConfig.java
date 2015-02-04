package fi.vm.sade.valinta.kooste.valintatapajono.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class ValintatapajonoConfig {

	@Bean
	public ValintatapajonoVientiRoute getValintatapajonoVientiRoute(
			@Value(ValintatapajonoVientiRoute.SEDA_VALINTATAPAJONO_VIENTI) String valintatapajonoVienti,
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(ValintatapajonoVientiRoute.SEDA_VALINTATAPAJONO_VIENTI),
						ValintatapajonoVientiRoute.class);
	}

}
