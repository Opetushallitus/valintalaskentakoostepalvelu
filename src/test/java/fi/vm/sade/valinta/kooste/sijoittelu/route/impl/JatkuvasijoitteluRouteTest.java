package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;

import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.impl.PropertyPlaceholderDelegateRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoitteluExchange;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.JatkuvaSijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.resource.SijoitteluResource;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;

/**
 * 
 * @author jussija
 *
 */
public class JatkuvasijoitteluRouteTest extends CamelTestSupport {
	private final static Logger LOG = LoggerFactory
			.getLogger(JatkuvasijoitteluRouteTest.class);
	private final DelayQueue<DelayedSijoitteluExchange> jatkuvaSijoitteluDelayedQueue = new DelayQueue<>();
	private final SijoittelunSeurantaResource sijoittelunSeurantaResource = Mockito
			.mock(SijoittelunSeurantaResource.class);
	private final SijoitteluResource sijoitteluResource = Mockito
			.mock(SijoitteluResource.class);
	private final String jatkuvaSijoitteluTimer = "direct:jatkuvaSijoitteluTimer";
	private final String jatkuvaSijoitteluQueue = "direct:jatkuvaSijoitteluQueue";
	private final ConcurrentHashMap<String, Long> ajossaHakuOids = Mockito
			.spy(new ConcurrentHashMap<>());
	private final JatkuvaSijoitteluRouteImpl jatkuvaSijoitteluRouteImpl = new JatkuvaSijoitteluRouteImpl(
			jatkuvaSijoitteluTimer, jatkuvaSijoitteluQueue, sijoitteluResource,
			sijoittelunSeurantaResource, jatkuvaSijoitteluDelayedQueue,
			ajossaHakuOids);
	@Produce(uri = jatkuvaSijoitteluTimer)
	protected ProducerTemplate timerTemplate;
	@Produce(uri = jatkuvaSijoitteluQueue)
	protected ProducerTemplate queueTemplate;

	@Test
	public void testaaJatkuvaSijoitteluRouteSamanHakuaEiLaitetaJonoonMoneenOtteeseen() {
		final String HK = "hk1";
		Mockito.reset(sijoittelunSeurantaResource, sijoitteluResource);
		SijoitteluDto s = new SijoitteluDto(HK, true, null, null, null, 1);
		Mockito.when(sijoittelunSeurantaResource.hae()).thenReturn(
				Arrays.asList(s));
		for (int i = 0; i < 2; ++i) {
			timerTemplate.send(new DefaultExchange(context()));

			Assert.assertFalse(jatkuvaSijoitteluRouteImpl
					.haeJonossaOlevatSijoittelut().isEmpty());
			Assert.assertTrue(jatkuvaSijoitteluRouteImpl
					.haeJonossaOlevatSijoittelut().size() == 1);
		}
	}

	@Test
	public void testaaJatkuvaSijoitteluRouteAjossaOlevaaHakuaEiLaitetaUudestaanJonoon() {
		final String HK = "hk1";
		Mockito.reset(sijoittelunSeurantaResource, sijoitteluResource);
		SijoitteluDto s = new SijoitteluDto(HK, true, null, null, null, 1);
		Mockito.when(sijoittelunSeurantaResource.hae()).thenReturn(
				Arrays.asList(s));
		timerTemplate.send(new DefaultExchange(context()));
		DelayedSijoitteluExchange exchange = jatkuvaSijoitteluDelayedQueue
				.poll();
		Assert.assertFalse("exchange oli null", exchange == null);
		queueTemplate.send(exchange);
		Assert.assertTrue("ei mennyt ajoon!", ajossaHakuOids.entrySet()
				.stream().filter(e -> HK.equals(e.getKey())).distinct()
				.findFirst().isPresent());
		timerTemplate.send(new DefaultExchange(context()));
		Assert.assertTrue("ei saa menna uudestaan tyojonoon koska oli ajossa",
				jatkuvaSijoitteluRouteImpl.haeJonossaOlevatSijoittelut()
						.isEmpty());
	}

	protected RouteBuilder createRouteBuilder() throws Exception {
		PropertyPlaceholderDelegateRegistry registry = (PropertyPlaceholderDelegateRegistry) context()
				.getRegistry();
		JndiRegistry jndiRegistry = (JndiRegistry) registry.getRegistry();
		jndiRegistry.bind("jatkuvaSijoitteluDelayedQueue",
				jatkuvaSijoitteluDelayedQueue);
		return jatkuvaSijoitteluRouteImpl;
	}
}
