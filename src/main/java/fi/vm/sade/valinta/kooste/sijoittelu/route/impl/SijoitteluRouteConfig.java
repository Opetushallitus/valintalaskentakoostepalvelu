package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import java.util.concurrent.DelayQueue;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class SijoitteluRouteConfig {

	@Bean
	public SijoitteluAktivointiRoute getSijoitteluAktivointiRoute(
			@Qualifier("javaDslCamelContext") CamelContext context,
			@Value(SijoitteluAktivointiRoute.SIJOITTELU_REITTI) String sijoitteluAktivoi)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(sijoitteluAktivoi),
				SijoitteluAktivointiRoute.class);
	}

}
