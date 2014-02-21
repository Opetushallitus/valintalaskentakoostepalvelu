package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.Arrays;
import java.util.List;

import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.HakuAppHakemus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.HakuAppHakemusOids;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.TarjonnanHakukohdeOids;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.Valintalaskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.Valintaperusteet;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

@Configuration
@Import({ ValintalaskentaMuistissaRouteImpl.class })
@ContextConfiguration(classes = { ValintalaskentaMuistissaTest.class,
		KoostepalveluContext.CamelConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class ValintalaskentaMuistissaTest {

	private List<String> hakukohdeOids = Arrays.asList("h1", "h2", "h3");

	@Autowired
	private CamelContext camelContext;
	@Autowired
	private TarjonnanHakukohdeOids tarjonnanHakukohdeOids;
	@Autowired
	private HakuAppHakemusOids hakuAppHakemusOids;
	@Autowired
	private HakuAppHakemus hakuAppHakemus;

	@Test
	public void testaaMaskaus() throws Exception {
		Mockito.when(
				tarjonnanHakukohdeOids.getHakukohdeOids(Mockito.anyString()))
				.thenReturn(hakukohdeOids);

		Mockito.when(hakuAppHakemusOids.getHakemusOids(Mockito.anyString()))
				.thenReturn(Arrays.asList("hak1", "hak2"));

		String hakuOid = "h0";
		ValintalaskentaMuistissaRoute l = ProxyWithAnnotationHelper
				.createProxy(camelContext
						.getEndpoint("direct:valintalaskenta_muistissa"),
						ValintalaskentaMuistissaRoute.class);
		l.aktivoiValintalaskenta(
				ValintalaskentaCache.create(hakuOid, Arrays.<String> asList()),
				Arrays.<String> asList(), hakuOid);
	}

	@Bean(name = "valintalaskentaMuistissaValvomo")
	public ValvomoService<ValintalaskentaMuistissaProsessi> getValvomoServiceImpl() {
		return new ValvomoServiceImpl<ValintalaskentaMuistissaProsessi>();
	}

	@Bean
	public Valintalaskenta getValintalaskenta() {
		return Mockito.mock(Valintalaskenta.class);
	}

	@Bean
	public HakuAppHakemusOids getHakuAppHakemusOids() {
		return Mockito.mock(HakuAppHakemusOids.class);
	}

	@Bean
	public HakuAppHakemus getHakuAppHakemus() {
		return Mockito.mock(HakuAppHakemus.class);
	}

	@Bean
	public TarjonnanHakukohdeOids getTarjonnanHakukohdeOids() {
		return Mockito.mock(TarjonnanHakukohdeOids.class);
	}

	@Bean
	public Valintaperusteet getValintaperusteet() {
		return Mockito.mock(Valintaperusteet.class);
	}
}
