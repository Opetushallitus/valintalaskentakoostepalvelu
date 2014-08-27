package fi.vm.sade.valinta.kooste.kela.route.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.HakuV1Resource;
import fi.vm.sade.valinta.kooste.kela.dto.Luonti;
import fi.vm.sade.valinta.kooste.kela.route.KelaLuontiRoute;
import fi.vm.sade.valinta.seuranta.resource.DokumentinSeurantaResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class KelaLuontiRouteTest extends CamelTestSupport {

	@Produce(uri = KelaLuontiRoute.SEDA_KELA_LUONTI)
	protected ProducerTemplate template;
	private static final Logger LOG = LoggerFactory
			.getLogger(KelaLuontiRouteTest.class);
	private final HakuV1Resource hakuResource = Mockito
			.mock(HakuV1Resource.class);
	private final DokumentinSeurantaResource seurantaResource = Mockito
			.mock(DokumentinSeurantaResource.class);
	private static final String HAKU1 = "haku1";
	private static final String HAKU2 = "haku2";
	private static final String UUID = "uuid";

	@Test
	public void kelaLuonninTestaus() {
		HakuV1RDTO haku1 = new HakuV1RDTO();
		ResultV1RDTO<HakuV1RDTO> hakuResult1 = new ResultV1RDTO(haku1);
		HakuV1RDTO haku2 = new HakuV1RDTO();
		ResultV1RDTO<HakuV1RDTO> hakuResult2 = new ResultV1RDTO(haku2);

		Mockito.when(hakuResource.findByOid(Mockito.eq(HAKU1))).thenReturn(
				hakuResult1);
		Mockito.when(hakuResource.findByOid(Mockito.eq(HAKU2))).thenReturn(
				hakuResult2);

		Collection<String> hakuOids = Arrays.asList(HAKU1, HAKU2);
		template.sendBodyAndProperty(new Luonti(UUID, hakuOids,
				StringUtils.EMPTY, StringUtils.EMPTY),
				KelaLuontiRoute.LOPETUSEHTO, new AtomicBoolean());
		LOG.error("HEHEHEHE");
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new KelaLuontiRouteImpl(true, hakuResource, seurantaResource);
	}
}
