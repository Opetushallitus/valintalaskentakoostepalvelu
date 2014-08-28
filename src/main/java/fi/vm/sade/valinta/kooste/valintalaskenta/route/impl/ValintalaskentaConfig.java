package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;

/**
 * @author Jussi Jartamo.
 */
@Configuration
public class ValintalaskentaConfig {

	@Bean
	public ValintalaskentaKerrallaRoute getValintalaskentaKaikilleRoute(
			@Value(ValintalaskentaKerrallaRoute.SEDA_VALINTALASKENTA_KERRALLA) String routeId,
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(routeId),
				ValintalaskentaKerrallaRoute.class);
	}

}
