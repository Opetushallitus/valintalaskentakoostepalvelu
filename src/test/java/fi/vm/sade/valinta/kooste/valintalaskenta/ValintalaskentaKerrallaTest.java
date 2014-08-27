package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetValinnanVaiheDTO;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintalaskentaResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaKerrallaRouteImpl;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.resource.LaskentaSeurantaResource;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

@Configuration
// @Import({ })
@ContextConfiguration(classes = { ValintalaskentaKerrallaTest.class,
		KoostepalveluContext.CamelConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class ValintalaskentaKerrallaTest {

	private static final int HAKUKOHTEITA = 5;
	private static final String UUID = "uuid";
	private static final String HAKUOID = "hakuOid";

	private static final String ENDPOINT = "direct:testValintalaskentaKerralla";
	private static final String ENDPOINT_VALINTAPERUSTEET = "seda:testValintalaskentaKerralla_valintaperusteet"
			+ "?purgeWhenStopping=true&waitForTaskToComplete=Never" +
			//
			"&concurrentConsumers=1";
	private static final String ENDPOINT_HAKEMUKSET = "seda:testValintalaskentaKerralla_hakemukset"
			+ "?purgeWhenStopping=true&waitForTaskToComplete=Never" +
			//
			"&concurrentConsumers=1";
	private static final String ENDPOINT_LASKENTA = "seda:testValintalaskentaKerralla_laskenta"
			+ "?purgeWhenStopping=true&waitForTaskToComplete=Never" +
			//
			"&concurrentConsumers=1";

	@Autowired
	private ValintalaskentaKerrallaRoute valintalaskentaKaikilleRoute;
	@Autowired
	private LaskentaSeurantaResource seurantaResource;
	@Autowired
	private ValintaperusteetResource valintaperusteetResource;
	@Autowired
	private ValintalaskentaResource valintalaskentaResource;

	@Test
	public void testaaValintalaskentaKerralla() throws InterruptedException {
		AtomicBoolean lopetusehto = new AtomicBoolean(false);
		List<String> hakukohdeOids = valintaperusteetResource
				.haunHakukohteet(HAKUOID).stream().map(h -> h.getOid())
				.collect(Collectors.toList());
		valintalaskentaKaikilleRoute.suoritaValintalaskentaKerralla(
				new LaskentaJaHaku(new Laskenta(UUID, HAKUOID, HAKUKOHTEITA,
						lopetusehto, null, null), hakukohdeOids), lopetusehto);
		Mockito.verify(seurantaResource, Mockito.timeout(15000).times(1))
				.merkkaaLaskennanTila(Mockito.eq(UUID),
						Mockito.eq(LaskentaTila.VALMIS));
		Matcher<LaskeDTO> l = new BaseMatcher<LaskeDTO>() {
			public boolean matches(Object o) {
				if (o instanceof LaskeDTO) {
					LaskeDTO l0 = (LaskeDTO) o;
					if (l0.getHakemus() == null || l0.getHakemus().isEmpty()
							|| l0.getValintaperuste() == null
							|| l0.getValintaperuste().isEmpty()) {
						return false;
					}
					return true;
				} else {
					return false;
				}
			}

			public void describeTo(Description desc) {

			}
		};
		Mockito.verify(valintalaskentaResource, Mockito.timeout(15000).times(3))
				.laskeKaikki(Mockito.argThat(l));

	}

	@Bean
	public ValintalaskentaKerrallaRouteImpl getValintalaskentaKerrallaRouteImpl(
			LaskentaSeurantaResource s, ValintaperusteetRestResource vr,
			ValintalaskentaResource vl, ApplicationResource app) {
		return new ValintalaskentaKerrallaRouteImpl(ENDPOINT,
				ENDPOINT_VALINTAPERUSTEET, ENDPOINT_HAKEMUKSET,
				ENDPOINT_LASKENTA, s, vr, vl, app);
	}

	@Bean
	public ValintalaskentaResource getValintalaskentaResource() {
		return Mockito.mock(ValintalaskentaResource.class);
	}

	@Bean
	public ValintalaskentaKerrallaRoute getValintalaskentaKaikilleRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(ENDPOINT),
				ValintalaskentaKerrallaRoute.class);
	}

	@Bean
	public ApplicationResource getApplicationResource() {
		ApplicationResource a = Mockito.mock(ApplicationResource.class);
		List<Hakemus> l = Lists.newArrayList();
		Hakemus h0 = new Hakemus();
		l.add(h0);

		Mockito.when(
				a.getApplicationsByOid(Mockito.anyString(),
						Mockito.anyListOf(String.class), Mockito.anyInt()))
				.thenReturn(l);
		Mockito.when(
				a.getApplicationsByOid(Mockito.eq("h1"),
						Mockito.anyListOf(String.class), Mockito.anyInt()))
				.thenReturn(Collections.emptyList());
		Mockito.when(
				a.getApplicationsByOid(Mockito.eq("h3"),
						Mockito.anyListOf(String.class), Mockito.anyInt()))
				.thenReturn(Collections.emptyList());
		Mockito.when(
				a.getApplicationsByOid(Mockito.eq("h3"),
						Mockito.anyListOf(String.class), Mockito.anyInt()))
				.thenThrow(new RuntimeException("unauthorized"));// thenReturn(Collections.emptyList());
		return a;
	}

	@Bean
	public LaskentaSeurantaResource getSeurantaResource() {
		return Mockito.mock(LaskentaSeurantaResource.class);
	}

	@Bean
	public ValintaperusteetRestResource getValintaperusteetRestResource() {
		ValintaperusteetRestResource v = Mockito
				.mock(ValintaperusteetRestResource.class);
		final List<ValintaperusteetDTO> vp = Lists.newArrayList();
		ValintaperusteetDTO v0 = new ValintaperusteetDTO();
		ValintaperusteetValinnanVaiheDTO vv0 = new ValintaperusteetValinnanVaiheDTO();
		vv0.setValinnanVaiheJarjestysluku(0);
		v0.setValinnanVaihe(vv0);
		vp.add(v0);

		ValintaperusteetDTO v1 = new ValintaperusteetDTO();
		ValintaperusteetValinnanVaiheDTO vv1 = new ValintaperusteetValinnanVaiheDTO();
		vv1.setValinnanVaiheJarjestysluku(1);
		v1.setValinnanVaihe(vv1);
		vp.add(v1);

		Mockito.when(
				v.haeValintaperusteet(Mockito.anyString(), Mockito.eq(null)))
				.thenAnswer(new Answer<List<ValintaperusteetDTO>>() {
					@Override
					public List<ValintaperusteetDTO> answer(
							InvocationOnMock invocation) throws Throwable {
						Thread.sleep(0);
						return vp;
					}
				});
		Mockito.when(v.haeValintaperusteet(Mockito.eq("h1"), Mockito.eq(null)))
				.thenReturn(Collections.emptyList());
		return v;
	}

	@Bean
	public ValintaperusteetResource getValintaperusteetResource() {
		ValintaperusteetResource v = Mockito
				.mock(ValintaperusteetResource.class);
		List<HakukohdeViiteDTO> l = Lists.newArrayList();
		for (int i = 0; i < HAKUKOHTEITA; ++i) {
			HakukohdeViiteDTO h1 = new HakukohdeViiteDTO();
			h1.setTila("JULKAISTU");
			h1.setOid("h" + i);
			l.add(h1);
		}

		Mockito.when(v.haunHakukohteet(Mockito.anyString())).thenReturn(l);
		return v;
	}
}
