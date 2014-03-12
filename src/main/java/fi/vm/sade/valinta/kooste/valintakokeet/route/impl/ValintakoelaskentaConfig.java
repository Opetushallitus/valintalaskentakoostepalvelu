package fi.vm.sade.valinta.kooste.valintakokeet.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.valintakokeet.route.ValintakoelaskentaMuistissaRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class ValintakoelaskentaConfig {

	@Bean
	public ValintakoelaskentaMuistissaRoute getValintakoelaskentaMuistissaRoute(
			@Value(ValintakoelaskentaMuistissaRoute.SEDA_VALINTAKOELASKENTA_MUISTISSA) String valintakoelaskenta,
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(valintakoelaskenta),
				ValintakoelaskentaMuistissaRoute.class);
	}
}
