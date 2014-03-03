package fi.vm.sade.valinta.kooste.viestintapalvelu;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.sijoittelu.tulos.resource.SijoitteluResource;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.exception.SijoittelupalveluException;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluIlmankoulutuspaikkaaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohdeNimiTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.route.TarjontaNimiRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.JalkiohjauskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.JalkiohjauskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.JalkiohjauskirjeRouteImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.ViestintapalveluConfig;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Ignore
@Configuration
@Import({ JalkiohjauskirjeetKomponentti.class, JalkiohjauskirjeRouteImpl.class })
@ContextConfiguration(classes = { JalkiohjauskirjeetTest.class,
		KoostepalveluContext.CamelConfig.class, ViestintapalveluConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class JalkiohjauskirjeetTest {

	@Autowired
	private JalkiohjauskirjeRoute jalkiohjauskirjeRoute;

	@Test(expected = SijoittelupalveluException.class)
	public void testaaJalkiohjauskirjeReitti() {
		DokumenttiProsessi p = new DokumenttiProsessi("", "", "", null);
		jalkiohjauskirjeRoute.jalkiohjauskirjeetAktivoi(p, null, "1", null);
	}

	@Bean
	public SijoitteluResource getSijoitteluResource() {
		return Mockito.mock(SijoitteluResource.class);
	}

	@Bean
	public HaeHakemusKomponentti getHaeHakemusKomponentti() {
		return Mockito.mock(HaeHakemusKomponentti.class);
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
	public SijoitteluIlmankoulutuspaikkaaKomponentti getSijoitteluIlmankoulutuspaikkaaProxy() {
		return Mockito.mock(SijoitteluIlmankoulutuspaikkaaKomponentti.class);
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
