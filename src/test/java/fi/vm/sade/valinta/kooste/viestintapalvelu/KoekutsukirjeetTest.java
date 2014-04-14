package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.KoekutsukirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.KoekutsukirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KoekutsukirjeRouteImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.ViestintapalveluConfig;
import fi.vm.sade.valintalaskenta.tulos.resource.ValintakoeResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@Import({ KoekutsukirjeetKomponentti.class, KoekutsukirjeRouteImpl.class })
@ContextConfiguration(classes = { KoekutsukirjeetTest.class,
		KoostepalveluContext.CamelConfig.class, ViestintapalveluConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class KoekutsukirjeetTest {

	@Autowired
	private KoekutsukirjeRoute koekutsukirjeRoute;

	@Test
	public void testaaKoekutsukirjeetReitti() throws InterruptedException {
		String hakukohdeOid = "";
		String letterBodyText = "";
		DokumenttiProsessi prosessi = new DokumenttiProsessi("", "", "",
				Collections.<String> emptyList());
		prosessi.getPoikkeukset().add(new Poikkeus("", "", ""));
		koekutsukirjeRoute.koekutsukirjeetAktivointi(prosessi,
				Collections.<String> emptyList(), hakukohdeOid,
				Collections.<String> emptyList(), letterBodyText, null);

	}

	@Bean(name = "dokumenttipalveluRestClient")
	public DokumenttiResource getDokumenttiResource() {
		return Mockito.mock(DokumenttiResource.class);
	}

	@Bean
	public ViestintapalveluResource getViestintapalveluResource() {
		return Mockito.mock(ViestintapalveluResource.class);
	}

	@Bean
	public ValintakoeResource getValintakoeResource() {
		return Mockito.mock(ValintakoeResource.class);
	}

	@Bean
	public HaeHakukohdeNimiTarjonnaltaKomponentti getHaeHakukohdeNimiTarjonnaltaKomponentti() {
		return Mockito.mock(HaeHakukohdeNimiTarjonnaltaKomponentti.class);
	}

	@Bean
	public KoodiService getKoodiService() {
		return Mockito.mock(KoodiService.class);
	}

	@Bean
	public HakukohdeResource getTarjontaResource() {
		return Mockito.mock(HakukohdeResource.class);
	}

	@Bean
	public ApplicationResource getApplicationResource() {
		return Mockito.mock(ApplicationResource.class);
	}

	@Bean
	public HaeOsoiteKomponentti getHaeOsoiteKomponentti() {
		return Mockito.mock(HaeOsoiteKomponentti.class);
	}

}
