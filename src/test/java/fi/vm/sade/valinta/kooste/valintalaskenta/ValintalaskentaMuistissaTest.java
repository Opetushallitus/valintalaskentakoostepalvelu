package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.ProxyWithAnnotationHelper;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.TyoImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaCache;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.ValintalaskentaMuistissaProsessi;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaMuistissaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.HakuAppHakemus;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.HakuAppHakemusOids;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.Valintalaskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaMuistissaRouteImpl.Valintaperusteet;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaTila;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;
import fi.vm.sade.valinta.kooste.valvomo.service.impl.ValvomoServiceImpl;

@Configuration
@Import({ ValintalaskentaMuistissaRouteImpl.class })
@ContextConfiguration(classes = { ValintalaskentaMuistissaTest.class,
		KoostepalveluContext.CamelConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class ValintalaskentaMuistissaTest {
	private final int VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST = (int) TimeUnit.SECONDS
			.toMillis(8);
	@Autowired
	private CamelContext camelContext;
	@Autowired
	private HakuAppHakemusOids hakuAppHakemusOids;
	@Autowired
	private HakuAppHakemus hakuAppHakemus;
	@Autowired
	private Valintaperusteet valintaperusteet;
	@Autowired
	private Valintalaskenta valintalaskenta;
	@Autowired
	private ValvomoService<ValintalaskentaMuistissaProsessi> valvomo;

	@Value(ValintalaskentaMuistissaRoute.SEDA_VALINTALASKENTA_MUISTISSA)
	private String routeId;

	@Test
	public void testaaMaskaus() throws Exception {
		Hakemus hak1 = new Hakemus();
		hak1.setOid("hak1");
		Hakemus hak2 = new Hakemus();
		hak2.setOid("hak2");

		Mockito.when(hakuAppHakemusOids.getHakemusOids("h1")).thenReturn(
				Arrays.asList("hak1", "hak2"));

		Mockito.when(hakuAppHakemusOids.getHakemusOids("h2")).thenReturn(
				Arrays.asList("hak1", "hak2"));
		Mockito.when(hakuAppHakemusOids.getHakemusOids("h3")).thenReturn(
				Arrays.asList("hak1", "hak2"));
		Mockito.when(hakuAppHakemusOids.getHakemusOids("h4")).thenReturn(
				Collections.<String> emptyList());

		Mockito.when(hakuAppHakemus.getHakemus("hak1")).thenReturn(hak1);
		Mockito.when(hakuAppHakemus.getHakemus("hak2")).thenReturn(hak2);
		Mockito.when(valintaperusteet.getValintaperusteet("h1")).thenReturn(
				Arrays.asList(new ValintaperusteetTyyppi()));
		Mockito.when(valintaperusteet.getValintaperusteet("h2")).thenReturn(
				Collections.<ValintaperusteetTyyppi> emptyList());
		Mockito.when(valintaperusteet.getValintaperusteet("h3")).thenReturn(
				Arrays.asList(new ValintaperusteetTyyppi()));

		String hakuOid = "h0";
		ValintalaskentaMuistissaRoute l = ProxyWithAnnotationHelper
				.createProxy(camelContext.getEndpoint(routeId),
						ValintalaskentaMuistissaRoute.class);

		ValintalaskentaMuistissaProsessi prosessi;

		ValintalaskentaMuistissaProsessi p = new ValintalaskentaMuistissaProsessi(
				hakuOid);
		TyoImpl hakemuksetTyo = Mockito.spy(p.getHakemukset());
		TyoImpl hakukohteilleHakemuksetTyo = Mockito.spy(p
				.getHakukohteilleHakemukset());
		TyoImpl valintaperusteetTyo = Mockito.spy(p.getValintaperusteet());
		TyoImpl valintalaskentaTyo = Mockito.spy(p.getValintalaskenta());

		ValintalaskentaMuistissaProsessi oikeaProsessi = new ValintalaskentaMuistissaProsessi(
				hakuOid, valintalaskentaTyo, hakukohteilleHakemuksetTyo,
				hakemuksetTyo, valintaperusteetTyo);
		prosessi = Mockito.spy(oikeaProsessi);

		l.aktivoiValintalaskenta(
				prosessi,
				new ValintalaskentaCache(Arrays.asList("h1", "h2", "h3", "h4")),
				hakuOid, Mockito.mock(Authentication.class));

		/**
		 * Oletetaan kymmenessä sekunnissa kolme valintalaskentaa tai epäillään
		 * ongelmia. Jos koodi toimii niin oikea arvo tulee millisekunneissa.
		 * Jos koodissa on todellinen virhe se halutaan saada kiinni!
		 */
		Mockito.verify(
				hakemuksetTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(2)) // only()
				.inkrementoiKokonaismaaraa();
		// kaksi hakemusta haetaan
		Mockito.verify(
				hakemuksetTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(2)) // only()
				.tyoValmistui(Mockito.anyLong());

		Mockito.verify(
				hakukohteilleHakemuksetTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(1)) // only()
				.setKokonaismaara(Mockito.eq(4));
		Mockito.verify(
				valintaperusteetTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(1)) // only()
				.setKokonaismaara(Mockito.eq(4));
		// yksi ohitetaan
		Mockito.verify(
				valintaperusteetTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(2)) // only()
				.tyoOhitettu();
		// kolme valmistuu
		Mockito.verify(
				valintaperusteetTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(2)) // only()
				.tyoValmistui(Mockito.anyLong());

		Mockito.verify(
				valintalaskentaTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(1)) // only
				.setKokonaismaara(4);

		Mockito.verify(
				valintalaskentaTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(1)) // only
				.tyoOhitettu();

		// kolme tyota valmistuu
		Mockito.verify(
				valintalaskentaTyo,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(3)) // only
				.tyoValmistui(Mockito.anyLong());
		Mockito.verify(
				valintalaskenta,
				Mockito.timeout(VALINTALASKENTA_TAKES_TO_COMPLETE_AT_MOST)
						.times(3)).teeValintalaskenta(
				Mockito.anyListOf(HakemusTyyppi.class),
				Mockito.anyListOf(ValintaperusteetTyyppi.class));

	}

	@Bean(name = "valintalaskentaMuistissaValvomo")
	public ValvomoService<ValintalaskentaMuistissaProsessi> getValvomoServiceImpl() {
		return new ValvomoServiceImpl<ValintalaskentaMuistissaProsessi>();
	}

	@Bean
	public ValintalaskentaTila getValintalaskentaTila() {
		return new ValintalaskentaTila();
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
	public Valintaperusteet getValintaperusteet() {
		return Mockito.mock(Valintaperusteet.class);
	}

}
