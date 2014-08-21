package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import junit.framework.Assert;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mockito.Mockito;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.SijoitteluResource;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
public class SijoitteluRouteTest extends CamelTestSupport {

	@Produce(uri = SijoitteluAktivointiRoute.SIJOITTELU_REITTI)
	protected ProducerTemplate template;

	private SijoitteluResource sijoitteluResource = Mockito
			.mock(SijoitteluResource.class);

	protected RouteBuilder createRouteBuilder() throws Exception {
		return new SijoitteluRouteImpl(sijoitteluResource);
	}

	@Test
	public void testaaReitti() {
		Sijoittelu sijoittelu = new Sijoittelu(StringUtils.EMPTY);
		template.sendBody(sijoittelu);
		Assert.assertEquals(true, sijoittelu.isValmis());
	}
}
