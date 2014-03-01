package fi.vm.sade.valinta.kooste.valintakokeet.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.valintakokeet.route.HakukohteenValintakoelaskentaRoute;
import fi.vm.sade.valinta.kooste.valintakokeet.route.HaunValintakoelaskentaRoute;
import fi.vm.sade.valinta.kooste.valvomo.dto.Prosessi;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class ValintakoelaskentaConfig {

	@Bean(name = "haunValintakoelaskentaValvomo")
	public ValvomoService<Prosessi> getValvomoServiceImpl() {
		return new ValvomoServiceImpl<Prosessi>();
	}

	@Bean
	public HakukohteenValintakoelaskentaRoute getHakukohteenValintakoelaskentaRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(HakukohteenValintakoelaskentaRoute.DIRECT_HAKUKOHTEEN_VALINTAKOELASKENTA),
						HakukohteenValintakoelaskentaRoute.class);
	}

	@Bean
	public HaunValintakoelaskentaRoute getHaunValintakoelaskentaRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(HaunValintakoelaskentaRoute.SEDA_HAUN_VALINTAKOELASKENTA),
						HaunValintakoelaskentaRoute.class);
	}
}
