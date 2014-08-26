package fi.vm.sade.valinta.kooste.kela.route.impl;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.KoostepalveluRouteBuilder;
import fi.vm.sade.valinta.kooste.kela.dto.KelaLuonti;
import fi.vm.sade.valinta.kooste.kela.route.KelaLuontiRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;

public class KelaLuontiRouteImpl extends KoostepalveluRouteBuilder<KelaLuonti> {
	private final static Logger LOG = LoggerFactory
			.getLogger(KelaLuontiRouteImpl.class);
	private final static String DEADLETTERCHANNEL = "direct:kela_luonti_route_deadletterchannel";

	@Override
	public void configure() throws Exception {
		from(KelaLuontiRoute.SEDA_KELA_LUONTI);

		from(DEADLETTERCHANNEL)
		//
				.process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						LOG.error(
								"Kelatiedoston luonti paattyi virheeseen\r\n{}",
								simple("${exception.message}").evaluate(
										exchange, String.class));
						exchange.getProperty(
								ValintalaskentaKerrallaRoute.LOPETUSEHTO,
								AtomicBoolean.class).set(true);
					}
				})
				//
				.stop();
	}

	@Override
	protected String deadLetterChannelEndpoint() {
		return DEADLETTERCHANNEL;
	}
}
