package fi.vm.sade.valinta.kooste.valintatapajono.route.impl;

import org.apache.camel.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoTuontiRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.AbstractDokumenttiRouteBuilder;

public class ValintatapajonoTuontiRouteImpl extends
		AbstractDokumenttiRouteBuilder {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintatapajonoTuontiRouteImpl.class);

	public ValintatapajonoTuontiRouteImpl() {
		super();
	}

	@Override
	public void configure() throws Exception {
		Endpoint valintatapajonoTuonti = endpoint(ValintatapajonoTuontiRoute.SEDA_VALINTATAPAJONO_TUONTI);

		from(valintatapajonoTuonti);
	}
}
