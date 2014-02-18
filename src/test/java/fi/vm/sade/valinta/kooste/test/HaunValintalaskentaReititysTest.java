package fi.vm.sade.valinta.kooste.test;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import fi.vm.sade.valinta.kooste.external.resource.haku.proxy.HakemusProxyCachingImpl;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.TavallinenValinnanVaiheTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.resources.HakuResource;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.TarjontaTila;
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohteetTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valintakokeet.komponentti.proxy.HakukohteenValintaperusteetProxyCachingImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.HaeValintaperusteetKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.SuoritaLaskentaKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.proxy.ValinnanVaiheenValintaperusteetProxyCachingImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaConfig;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaRouteImpl;

/**
 * User: wuoti Date: 27.5.2013 Time: 9.30
 */
// @Configuration
// @ContextConfiguration(classes = HaunValintalaskentaReititysTest.class)
// @PropertySource({
// "classpath:META-INF/valintalaskentakoostepalvelu.properties",
// "classpath:test.properties" })
// @ImportResource({ "classpath:META-INF/spring/context/haku-context.xml",
// "classpath:META-INF/spring/context/tarjonta-context.xml",
// "classpath:META-INF/spring/context/valintalaskenta-context.xml",
// "test-context.xml" })
// @RunWith(SpringJUnit4ClassRunner.class)
@Configuration
@Import({ ValintalaskentaRouteImpl.class,
		HaeHakukohteenHakemuksetKomponentti.class, HaeHakemusKomponentti.class,
		HaeValintaperusteetKomponentti.class,
		HaeHakukohteetTarjonnaltaKomponentti.class,
		ValinnanVaiheenValintaperusteetProxyCachingImpl.class,
		HakukohteenValintaperusteetProxyCachingImpl.class,
		SuoritaLaskentaKomponentti.class,
        HakemusProxyCachingImpl.class })
@ContextConfiguration(classes = { KoostepalveluContext.CamelConfig.class,
		HaunValintalaskentaReititysTest.class, ValintalaskentaConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class HaunValintalaskentaReititysTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(HaunValintalaskentaReititysTest.class);

	private static final String[] HAKUKOHDE_OIDS = { "hakukohdeoid1",
			"hakukohdeoid2", "hakukohdeoid3", "hakukohdeoid4", "hakukohdeoid5",
			"hakukohdeoid6s" };

	private static final String HAKUOID = "hakuoid";

	private static final String HAKEMUSOID = "hakemus0";
	private static final String HAKUKOHDEOID = "hakukohde0";
	private static final Integer VALINNANVAIHE = 6;

	@Bean
	public HakukohdeResource getHakukohdeResource() {
		return mock(HakukohdeResource.class);
	}

	@Bean
	public ApplicationResource getApplicationResourceMock() {
		ApplicationResource mock = mock(ApplicationResource.class);

		HakemusList hlist = new HakemusList();
		hlist.setTotalCount(1);
		SuppeaHakemus hk = new SuppeaHakemus();
		hk.setOid(HAKEMUSOID);
		hlist.getResults().add(hk);

		when(
				mock.findApplications(anyString(), anyListOf(String.class),
						anyString(), anyString(), anyString(),
						argThat(new BaseMatcher<String>() {
							@Override
							public boolean matches(Object o) {
								String s = (String) o;

								boolean found = false;
								for (String hakukohde : HAKUKOHDE_OIDS) {
									if (s.equals(hakukohde)) {
										found = true;
										break;
									}
								}

								return found;

							}

							@Override
							public void describeTo(Description description) {
								// To change body of implemented methods use
								// File | Settings | File Templates.
							}
						}), anyInt(), anyInt())).thenReturn(hlist);

		Hakemus hakemus = new Hakemus();
		hakemus.setOid(HAKEMUSOID);

		when(mock.getApplicationByOid(eq(HAKEMUSOID))).thenReturn(hakemus);

		return mock;
	}

	@Bean
	public ValintaperusteService getValintaperusteServiceMock() {
		ValintaperusteService valintaperusteMock = mock(ValintaperusteService.class);
		ValintaperusteetTyyppi vtyyppi = new ValintaperusteetTyyppi();
		vtyyppi.setHakukohdeOid(HAKUKOHDEOID);
		TavallinenValinnanVaiheTyyppi vaihe = new TavallinenValinnanVaiheTyyppi();
		vaihe.setValinnanVaiheJarjestysluku(VALINNANVAIHE);

		when(
				valintaperusteMock
						.haeValintaperusteet(anyListOf(HakuparametritTyyppi.class)))
				.thenReturn(Arrays.asList(vtyyppi));
		return valintaperusteMock;
	}

	@Bean
	public ValintalaskentaService getValintalaskentaService() {
		return mock(ValintalaskentaService.class);
	}

	@Bean(name = "tarjontaServiceClientAsAdmin")
	public TarjontaPublicService getTarjontaPublicServiceMock() {
		TarjontaPublicService tarjontaService = mock(TarjontaPublicService.class);
		TarjontaTyyppi tarjonta = new TarjontaTyyppi();
		for (String oid : HAKUKOHDE_OIDS) {
			HakukohdeTyyppi hakukohde = new HakukohdeTyyppi();
			hakukohde.setOid(oid);
			hakukohde.setHakukohteenTila(TarjontaTila.JULKAISTU);
			tarjonta.getHakukohde().add(hakukohde);
		}
		when(tarjontaService.haeTarjonta(eq(HAKUOID))).thenReturn(tarjonta);
		return tarjontaService;
	}

	@Bean
	public ParametriService getParametriService() {
		ParametriService parametriService = mock(ParametriService.class);
		when(parametriService.valintalaskentaEnabled(HAKUOID)).thenReturn(true);
		return parametriService;
	}

	@Bean
	public OrganisaatioResource getOrganisaatioResourceMock() {
		return mock(OrganisaatioResource.class);
	}

	@Bean
	public KoodiService getKoodiService() {
		return mock(KoodiService.class);
	}

	@Bean
	public HakuResource getHakuResource() {
		return mock(HakuResource.class);
	}

	@Autowired
	private ValintalaskentaService valintalaskentaService;

	@Autowired
	private ApplicationResource applicationResourceMock;

	@Autowired
	private HaunValintalaskentaRoute haunValintalaskentaAktivointiProxy;

	@Test
	public void testLaskentaKooste() {
		haunValintalaskentaAktivointiProxy.aktivoiValintalaskenta(HAKUOID);

		// verify that hakemusservice was indeed called with REST argument!

		// for (String hakukohdeoid : HAKUKOHDE_OIDS) {
		// verify(applicationResourceMock,
		// times(1)).findApplications(anyString(), anyListOf(String.class),
		// anyString(), anyString(), anyString(), eq(hakukohdeoid), anyInt(),
		// anyInt());
		// }

		// verify that the route ended calling valintalaskentaservice!
		verify(valintalaskentaService, times(HAKUKOHDE_OIDS.length)).laske(
				anyListOf(HakemusTyyppi.class),
				anyListOf(ValintaperusteetTyyppi.class));
	}
}
