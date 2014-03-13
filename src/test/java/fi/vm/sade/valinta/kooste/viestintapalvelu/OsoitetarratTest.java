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

import fi.vm.sade.service.valintatiedot.schema.HakemusOsallistuminenTyyppi;
import fi.vm.sade.service.valintatiedot.schema.Osallistuminen;
import fi.vm.sade.service.valintatiedot.schema.ValintakoeOsallistuminenTyyppi;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKoulutuspaikkallisetKomponentti;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.DokumenttiTyyppi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.OsoitetarratRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.OsoitetarratRouteImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.ViestintapalveluConfig;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@Import({ OsoitetarratRouteImpl.class })
@ContextConfiguration(classes = { OsoitetarratTest.class,
		KoostepalveluContext.CamelConfig.class, ViestintapalveluConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class OsoitetarratTest {

	@Autowired
	private ValintatietoHakukohteelleKomponentti valintatietoHakukohteelleKomponentti;

	@Autowired
	private OsoitetarratRoute osoitetarratRoute;

	@Autowired
	private HaeOsoiteKomponentti osoiteKomponentti;

	@Test
	public void testaaOsoitetarratReittiPaastaPaahan() {
		HakemusOsallistuminenTyyppi h0 = createValintatieto("1",
				Osallistuminen.EI_OSALLISTU);
		HakemusOsallistuminenTyyppi h1 = createValintatieto("2",
				Osallistuminen.OSALLISTUU);

		Mockito.when(
				valintatietoHakukohteelleKomponentti
						.valintatiedotHakukohteelle(
								Mockito.anyListOf(String.class),
								Mockito.anyString())).thenReturn(
				Arrays.asList(h0, h1));

		Osoite o = Mockito.mock(Osoite.class);

		Mockito.when(osoiteKomponentti.haeOsoite(Mockito.anyString()))
				.thenReturn(o);

		DokumenttiProsessi p = new DokumenttiProsessi("", "", "", null);
		osoitetarratRoute.osoitetarratAktivointi(DokumenttiTyyppi.HAKEMUKSILLE,
				p, null, "h0", Arrays.asList("v0", "v1"), null);

	}

	private HakemusOsallistuminenTyyppi createValintatieto(String oid,
			Osallistuminen o) {
		HakemusOsallistuminenTyyppi h = new HakemusOsallistuminenTyyppi();
		h.setHakemusOid(oid);
		ValintakoeOsallistuminenTyyppi o0 = new ValintakoeOsallistuminenTyyppi();
		o0.setOsallistuminen(o);
		h.getOsallistumiset().add(o0);
		return h;
	}

	@Bean(name = "dokumenttipalveluRestClient")
	public DokumenttiResource getDokumenttiResource() {
		return Mockito.mock(DokumenttiResource.class);
	}

	@Bean
	public SijoitteluKoulutuspaikkallisetKomponentti getSijoitteluKoulutuspaikallisetProxy() {
		return Mockito.mock(SijoitteluKoulutuspaikkallisetKomponentti.class);
	}

	@Bean
	public ApplicationResource getApplicationResource() {
		return Mockito.mock(ApplicationResource.class);
	}

	@Bean
	public ViestintapalveluResource getViestintapalveluResource() {
		return Mockito.mock(ViestintapalveluResource.class);
	}

	@Bean
	public ValintatietoHakukohteelleKomponentti getValintatietoHakukohteelleKomponentti() {
		return Mockito.mock(ValintatietoHakukohteelleKomponentti.class);
	}

	@Bean
	public HaeOsoiteKomponentti getHaeOsoiteKomponentti() {
		return Mockito.mock(HaeOsoiteKomponentti.class);
	}
}
