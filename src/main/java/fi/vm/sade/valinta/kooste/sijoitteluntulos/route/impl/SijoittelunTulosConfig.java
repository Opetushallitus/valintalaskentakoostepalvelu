package fi.vm.sade.valinta.kooste.sijoitteluntulos.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosHyvaksymiskirjeetRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosOsoitetarratRoute;
import fi.vm.sade.valinta.kooste.sijoitteluntulos.route.SijoittelunTulosTaulukkolaskentaRoute;
import org.springframework.context.annotation.Profile;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Profile("default")
@Configuration
public class SijoittelunTulosConfig {
	@Bean
	public SijoittelunTulosOsoitetarratRoute getSijoittelunTulosOsoitetarratRoute(
			@Value(SijoittelunTulosOsoitetarratRoute.SEDA_SIJOITTELUNTULOS_OSOITETARRAT_HAULLE) String osoitetarrat,
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(osoitetarrat),
				SijoittelunTulosOsoitetarratRoute.class);
	}

	@Bean
	public SijoittelunTulosTaulukkolaskentaRoute getSijoittelunTulosTaulukkolaskentaRoute(
			@Value(SijoittelunTulosTaulukkolaskentaRoute.SEDA_SIJOITTELUNTULOS_TAULUKKOLASKENTA_HAULLE) String taulukkolaskenta,
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(taulukkolaskenta),
				SijoittelunTulosTaulukkolaskentaRoute.class);
	}
}
