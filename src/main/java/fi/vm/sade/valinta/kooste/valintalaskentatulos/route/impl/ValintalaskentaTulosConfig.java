package fi.vm.sade.valinta.kooste.valintalaskentatulos.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.JalkiohjaustulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.SijoittelunTulosExcelRoute;
import fi.vm.sade.valinta.kooste.valintalaskentatulos.route.ValintalaskentaTulosExcelRoute;
import org.springframework.context.annotation.Profile;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Profile("default")
@Configuration
public class ValintalaskentaTulosConfig {
	@Bean
	public JalkiohjaustulosExcelRoute getJalkiohjaustulosExcelRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(JalkiohjaustulosExcelRoute.DIRECT_JALKIOHJAUS_EXCEL),
						JalkiohjaustulosExcelRoute.class);
	}

	@Bean
	public SijoittelunTulosExcelRoute getSijoittelunTulosExcelRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(SijoittelunTulosExcelRoute.SEDA_SIJOITTELU_EXCEL),
						SijoittelunTulosExcelRoute.class);
	}

	@Bean
	public ValintalaskentaTulosExcelRoute getValintalaskentaTulosExcelRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(ValintalaskentaTulosExcelRoute.DIRECT_VALINTALASKENTA_EXCEL),
						ValintalaskentaTulosExcelRoute.class);
	}

}
