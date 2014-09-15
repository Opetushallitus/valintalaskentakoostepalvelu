package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetValinnanVaiheDTO;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Answers;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.laskenta.ValintalaskentaResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetRestResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaJaHaku;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import static fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute.*;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaKerrallaRouteImpl;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.resource.LaskentaSeurantaResource;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
// @Import({ })
@ContextConfiguration(classes = { ValintalaskentaKerrallaTest.class,
		KoostepalveluContext.CamelConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class ValintalaskentaKerrallaTest {
	private static final Logger LOG = LoggerFactory
			.getLogger(ValintalaskentaKerrallaTest.class);
	private static final String UUID = "uuid";
	private static final String HAKUOID = "hakuOid";

	@Autowired
	private ValintalaskentaKerrallaRoute valintalaskentaKaikilleRoute;
	@Autowired
	private LaskentaSeurantaAsyncResource seurantaResource;

	@Autowired
	private ValintalaskentaAsyncResource valintalaskentaAsyncResource;

	@Test
	public void testaaValintalaskentaKerralla() throws InterruptedException {
		AtomicBoolean lopetusehto = new AtomicBoolean(false);
		List<String> hakukohdeOids = Arrays
				.asList("h1", "h2", "h3", "h4", "h5");
		valintalaskentaKaikilleRoute.suoritaValintalaskentaKerralla(
				new LaskentaJaHaku(new Laskenta(UUID, HAKUOID, hakukohdeOids
						.size(), lopetusehto, null, null), hakukohdeOids),
				lopetusehto);
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

		Mockito.verify(valintalaskentaAsyncResource,
				Mockito.timeout(15000).atLeast(2)).laskeKaikki(
				Mockito.argThat(l), Mockito.any(), Mockito.any());

	}

	@Bean
	public ValintalaskentaKerrallaRouteImpl getValintalaskentaKerrallaRouteImpl(
			LaskentaSeurantaAsyncResource s, ValintaperusteetAsyncResource vr,
			ValintalaskentaAsyncResource vl, ApplicationAsyncResource app) {
		return new ValintalaskentaKerrallaRouteImpl(s, vr, vl, app);
	}

	@Bean
	public ValintalaskentaAsyncResource getValintalaskentaAsyncResource() {
		ValintalaskentaAsyncResource v = new ValintalaskentaAsyncResource() {
			@Override
			public void laske(LaskeDTO laskeDTO, Consumer<String> callback,
					Consumer<Throwable> failureCallback) {
				LOG.error("\r\n############\r\n############ Pelkka laskenta \r\n############");
				callback.accept("ok");
			}

			@Override
			public void laskeKaikki(LaskeDTO laskeDTO,
					Consumer<String> callback,
					Consumer<Throwable> failureCallback) {
				LOG.error("\r\n############\r\n############ Laskenta kaikille vaiheille \r\n############");
				callback.accept("ok");
			}

			@Override
			public void valintakokeet(LaskeDTO laskeDTO,
					Consumer<String> callback,
					Consumer<Throwable> failureCallback) {
				LOG.error("\r\n############\r\n############ Laskenta valintakoevaiheelle \r\n############");
				callback.accept("ok");
			}
		};
		return Mockito.spy(v);
	}

	@Bean
	public ValintalaskentaKerrallaRoute getValintalaskentaKaikilleRoute(
			@Qualifier("javaDslCamelContext") CamelContext context)
			throws Exception {
		return ProxyWithAnnotationHelper.createProxy(
				context.getEndpoint(SEDA_VALINTALASKENTA_KERRALLA),
				ValintalaskentaKerrallaRoute.class);
	}

	@Bean
	public ApplicationAsyncResource getApplicationAsyncResource() {

		final List<Hakemus> l = Lists.newArrayList();
		Hakemus h0 = new Hakemus();
		h0.setOid("hakemus1oid");
		h0.setAnswers(new Answers());
		l.add(h0);

		final ApplicationAdditionalDataDTO addData = new ApplicationAdditionalDataDTO();
		addData.setOid(h0.getOid());

		ApplicationAsyncResource a = new ApplicationAsyncResource() {
			@Override
			public void getApplicationAdditionalData(String hakuOid,
					String hakukohdeOid,
					Consumer<List<ApplicationAdditionalDataDTO>> callback,
					Consumer<Throwable> failureCallback) {
				callback.accept(Lists.newArrayList(addData));
			}

			@Override
			public void getApplicationsByOid(String hakukohdeOid,
					Consumer<List<Hakemus>> callback,
					Consumer<Throwable> failureCallback) {
				if ("h1".equals(hakukohdeOid)) {
					callback.accept(Collections.emptyList());
				} else if ("h2".equals(hakukohdeOid)) {
					callback.accept(Collections.emptyList());
				} else if ("h3".equals(hakukohdeOid)) {
					failureCallback
							.accept(new RuntimeException("unauthorized"));
				} else {
					callback.accept(l);
				}
			}
		};
		return a;
	}

	@Bean
	public LaskentaSeurantaAsyncResource getLaskentaSeurantaAsyncResource() {
		return Mockito.mock(LaskentaSeurantaAsyncResource.class);
	}

	@Bean
	public ValintaperusteetAsyncResource getValintaperusteetAsyncResource() {

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
		ValintaperusteetAsyncResource v = new ValintaperusteetAsyncResource() {
			@Override
			public void haeHakijaryhmat(String hakukohdeOid,
					Consumer<List<ValintaperusteetHakijaryhmaDTO>> callback,
					Consumer<Throwable> failureCallback) {
				callback.accept(Collections.emptyList());
			}

			@Override
			public void haeValintaperusteet(String hakukohdeOid,
					Integer valinnanVaiheJarjestysluku,
					Consumer<List<ValintaperusteetDTO>> callback,
					Consumer<Throwable> failureCallback) {
				if ("h1".equals(hakukohdeOid)) {
					callback.accept(Collections.emptyList());
				} else {
					// Thread.sleep(0);
					callback.accept(vp);
				}
			}

			@Override
			public void haunHakukohteet(String hakuOid,
					Consumer<List<HakukohdeViiteDTO>> callback,
					Consumer<Throwable> failureCallback) {

			}
		};
		return v;
	}

}
