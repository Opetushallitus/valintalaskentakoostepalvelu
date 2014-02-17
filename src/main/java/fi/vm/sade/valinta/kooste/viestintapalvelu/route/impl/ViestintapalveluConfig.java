package fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratHakemuksilleRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratSijoittelussaHyvaksytyilleRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
public class ViestintapalveluConfig {

	@Bean
	public HyvaksymiskirjeRoute getHyvaksymiskirjeRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(context
				.getEndpoint(HyvaksymiskirjeRoute.DIRECT_HYVAKSYMISKIRJEET),
				HyvaksymiskirjeRoute.class);
	}

	@Bean
	public OsoitetarratRoute getOsoitetarratRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(OsoitetarratRoute.DIRECT_OSOITETARRAT),
				OsoitetarratRoute.class);
	}

	@Bean
	public JalkiohjauskirjeRoute getJalkiohjauskirjeRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(context
				.getEndpoint(JalkiohjauskirjeRoute.DIRECT_JALKIOHJAUSKIRJEET),
				JalkiohjauskirjeRoute.class);
	}

	@Bean
	public OsoitetarratSijoittelussaHyvaksytyilleRoute getHyvaksyttyjenOsoitetarratRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(OsoitetarratSijoittelussaHyvaksytyilleRoute.DIRECT_HYVAKSYTTYJEN_OSOITETARRAT),
						OsoitetarratSijoittelussaHyvaksytyilleRoute.class);
	}

	@Bean
	public OsoitetarratHakemuksilleRoute getHakemuksilleOsoitetarratRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper
				.createProxy(
						context.getEndpoint(OsoitetarratHakemuksilleRoute.DIRECT_OSOITETARRAT_HAKEMUKSILLE),
						OsoitetarratHakemuksilleRoute.class);
	}

	@Bean
	public KoekutsukirjeRoute getKoekutsukirjeRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(KoekutsukirjeRoute.DIRECT_KOEKUTSUKIRJEET),
				KoekutsukirjeRoute.class);
	}
}
