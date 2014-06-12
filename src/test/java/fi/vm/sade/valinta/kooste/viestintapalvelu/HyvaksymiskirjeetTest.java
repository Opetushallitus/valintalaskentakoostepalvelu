package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.route.TarjontaNimiRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeRouteImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.KirjeetHakukohdeCache;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.ViestintapalveluConfig;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@Import({ HyvaksymiskirjeetKomponentti.class, HyvaksymiskirjeRouteImpl.class })
@ContextConfiguration(classes = { HyvaksymiskirjeetTest.class,
		KoostepalveluContext.CamelConfig.class, ViestintapalveluConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class HyvaksymiskirjeetTest {

	@Autowired
	private HyvaksymiskirjeRoute hyvaksymiskirjeRoute;

	// @Test(expected = HakemuspalveluException.class)
	@Test
	public void testaaHyvaksymiskirjeetReitti() {
		DokumenttiProsessi p = new DokumenttiProsessi("", "", "", null);
		hyvaksymiskirjeRoute.hyvaksymiskirjeetAktivointi(p, "1", "t", "f", "",
				"", Arrays.asList("j"), "2", 3L, null);
	}

	@Bean
	public KirjeetHakukohdeCache getKirjeetHakukohdeCache() {
		return new KirjeetHakukohdeCache();
	}

	@Bean
	public ApplicationResource getHaeHakemusKomponentti() {
		return Mockito.mock(ApplicationResource.class);
	}

	@Bean
	public ViestintapalveluResource getViestintapalveluResource() {
		return Mockito.mock(ViestintapalveluResource.class);
	}

	@Bean
	public HaeHakukohdeNimiTarjonnaltaKomponentti getHaeHakukohdeNimiTarjonnaltaKomponentti() {
		return Mockito.mock(HaeHakukohdeNimiTarjonnaltaKomponentti.class);
	}

	@Bean
	public SijoitteluKoulutuspaikkallisetKomponentti getSijoitteluKoulutuspaikallisetProxy() {
		return Mockito.mock(SijoitteluKoulutuspaikkallisetKomponentti.class);
	}

	@Bean
	public TarjontaNimiRoute getTarjontaNimiProxy() {
		return Mockito.mock(TarjontaNimiRoute.class);
	}

	@Bean
	public HaeOsoiteKomponentti getHaeOsoiteKomponentti() {
		return Mockito.mock(HaeOsoiteKomponentti.class);
	}

	@Bean(name = "dokumenttipalveluRestClient")
	public DokumenttiResource getDokumenttiResource() {
		return Mockito.mock(DokumenttiResource.class);
	}
}
