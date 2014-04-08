package fi.vm.sade.valinta.kooste.kela;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.koodisto.service.KoodiService;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.tarjonta.service.resources.HakuResource;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.tarjonta.service.resources.dto.HakuDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.HaunTyyppiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.OppilaitosKomponentti;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteImpl;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKaikkiPaikanVastaanottaneet;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;

@Configuration
@Import(KelaRouteImpl.class)
@ContextConfiguration(classes = { KoostepalveluContext.CamelConfig.class,
		KelaRouteTest.class, KelaRouteConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
// @DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class KelaRouteTest {

	private static final Logger LOG = LoggerFactory
			.getLogger(KelaRouteTest.class);

	@Bean
	public ApplicationResource getHaeHakemusKomponentti() {
		return mock(ApplicationResource.class);
	}

	@Bean
	public KelaHakijaRiviKomponenttiImpl mockKelaHakijaKomponentti()
			throws Exception {
		return mock(KelaHakijaRiviKomponenttiImpl.class);
	}

	@Bean
	public KoodiService getKoodiService() {
		return mock(KoodiService.class);
	}

	@Bean
	public HakuResource getHakuResource() {
		return mock(HakuResource.class);
	}

	@Bean
	public HaeHakuTarjonnaltaKomponentti getHaeHakuTarjonnaltaKomponentti() {
		return mock(HaeHakuTarjonnaltaKomponentti.class);
	}

	@Bean
	public DokumenttiResource mockDokumenttiResource() {
		return mock(DokumenttiResource.class);
	}

	@Bean
	public KelaDokumentinLuontiKomponenttiImpl getKelaDokumentinLuontiKomponentti() {
		return mock(KelaDokumentinLuontiKomponenttiImpl.class);
	}

	@Bean
	public SijoitteluKaikkiPaikanVastaanottaneet mockSijoitteluKaikkiPaikanVastaanottaneet() {
		return mock(SijoitteluKaikkiPaikanVastaanottaneet.class);
	}

	@Bean
	public LinjakoodiKomponentti getLinjakoodiKomponentti() {
		return mock(LinjakoodiKomponentti.class);
	}

	@Bean
	public OrganisaatioResource getOrganisaatioResource() {
		return mock(OrganisaatioResource.class);
	}

	@Bean
	public OppilaitosKomponentti getOppilaitosKomponentti() {
		return mock(OppilaitosKomponentti.class);
	}

	@Bean
	public HakukohdeResource getHakukohdeResource() {
		return mock(HakukohdeResource.class);
	}

	@Bean
	public HaunTyyppiKomponentti getHaunTyyppiKomponentti() {
		return mock(HaunTyyppiKomponentti.class);
	}

	@Autowired
	private KelaRoute kelaSiirtoDokumentinLuonti;

	@Autowired
	private SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet;

	@Autowired
	private KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti;

	@Autowired
	private KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti;

	@Autowired
	private HakuResource hakuResource;

	// VAKIO TESTI MUUTTUJIA
	private final HakijaDTO ok = new HakijaDTO();
	private final String organisaationNimi = "organisaationNimi";
	private final String aineistonNimi = "testaaEttaOikeinSuoritetustaReitistaSeuraaDokumentti";

	@Test
	public void testaaEttaOikeinSuoritetustaReitistaSeuraaDokumentti()
			throws Exception {

		String haku1 = "hakuoid1";
		String haku2 = "hakuoid2";

		when(sijoitteluVastaanottaneet.vastaanottaneet(anyString()))
				.thenReturn(
						Arrays.asList(ok, ok, ok, ok, ok, ok, ok, ok, ok, ok,
								ok, ok, ok, ok, ok));

		HakuDTO dto1 = new HakuDTO();
		HakuDTO dto2 = new HakuDTO();

		when(hakuResource.getByOID(Mockito.eq(haku1))).thenReturn(dto1);
		when(hakuResource.getByOID(Mockito.eq(haku2))).thenReturn(dto2);

		Collection<String> hakuOids = Arrays.asList(haku1, haku2);
		KelaProsessi prosessi;
		kelaSiirtoDokumentinLuonti.aloitaKelaLuonti(
				prosessi = new KelaProsessi("", hakuOids), hakuOids,
				aineistonNimi, organisaationNimi, null);

	}

}
