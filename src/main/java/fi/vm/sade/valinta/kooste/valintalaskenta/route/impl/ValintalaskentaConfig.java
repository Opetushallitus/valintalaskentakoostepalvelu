package fi.vm.sade.valinta.kooste.valintalaskenta.route.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.ProxyBuilder;
import org.apache.camel.component.bean.ProxyHelper;
import org.apache.camel.component.seda.BlockingQueueFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.util.BlockingLifoQueueFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HakukohteenValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

/**
 * @author Jussi Jartamo.
 */
@Configuration
public class ValintalaskentaConfig {

	@Bean(name = "valintalaskentaValvomo")
	public ValvomoService<ValintalaskentaProsessi> getValvomoServiceImpl() {
		return new ValvomoServiceImpl<ValintalaskentaProsessi>();
	}

	
			
	
	@Bean
	public HaunValintalaskentaRoute getValintalaskentaHaulleRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(HaunValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAULLE),
						HaunValintalaskentaRoute.class);
	}

	@Bean(name = "defaultQueueFactory")
	public <E> BlockingQueueFactory<E> getBlockingQueueFactory() {
		BlockingLifoQueueFactory<E> e = new BlockingLifoQueueFactory<E>();
		return e;
	}

	@Bean
	public HakukohteenValintalaskentaRoute getValintalaskentaHakukohteelleRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(HakukohteenValintalaskentaRoute.DIRECT_VALINTALASKENTA_HAKUKOHTEELLE),
						HakukohteenValintalaskentaRoute.class);
	}
}
