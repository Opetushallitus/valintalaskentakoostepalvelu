package fi.vm.sade.valinta.kooste.pistesyotto.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.pistesyotto.route.PistesyottoTuontiRoute;
import fi.vm.sade.valinta.kooste.pistesyotto.route.PistesyottoVientiRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class PistesyottoConfig {
	@Bean
	public PistesyottoVientiRoute getPistesyottoVientiRoute(
			@Value(PistesyottoVientiRoute.SEDA_PISTESYOTTO_VIENTI) String pistesyottoVienti,
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(pistesyottoVienti),
				PistesyottoVientiRoute.class);
	}

	@Bean
	public PistesyottoTuontiRoute getPistesyottoTuontiRoute(
			@Value(PistesyottoTuontiRoute.SEDA_PISTESYOTTO_TUONTI) String pistesyottoTuonti,
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(pistesyottoTuonti),
				PistesyottoTuontiRoute.class);
	}
}
