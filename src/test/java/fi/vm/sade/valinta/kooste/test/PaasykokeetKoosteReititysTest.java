package fi.vm.sade.valinta.kooste.test;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.valinta.kooste.paasykokeet.ValintakoelaskentaAktivointiResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@ContextConfiguration(classes = PaasykokeetKoosteReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource({ "classpath:META-INF/spring/context/valintakoe-context.xml",
        "classpath:META-INF/spring/context/valintalaskenta-context.xml", "test-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class PaasykokeetKoosteReititysTest {

    private static final String HAKUKOHDEOID = "hakukohde0";

    private static HakemusTyyppi HAKEMUS = new HakemusTyyppi();

    @Bean
    public HakemusService getHakemusServiceMock() {
        HakemusService hakemusService = mock(HakemusService.class);
        when(hakemusService.haeHakemukset(anyListOf(String.class))).thenReturn(Arrays.asList(HAKEMUS));
        return hakemusService;
    }

    @Bean
    public ValintaperusteService getValintaperusteServiceMock() {
        return mock(ValintaperusteService.class);
    }

    @Bean
    public ValintalaskentaService getValintalaskentaService() {
        return mock(ValintalaskentaService.class);
    }

    @Bean
    public TarjontaPublicService getTarjontaPublicServiceMock() {
        return mock(TarjontaPublicService.class);
    }

    @Autowired
    private ValintakoelaskentaAktivointiResource valintakoelaskentaResource;

    @Autowired
    private HakemusService hakemusService;

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    @Test
    public void testValintakoelaskentaKooste() {
        // syötetään tietty OID
        valintakoelaskentaResource.aktivoiValintalaskenta(HAKUKOHDEOID);
        // varmennetaan että reitti välittää syötetyn OID:n hakemuspalvelulle
        verify(hakemusService, only()).haeHakemukset(eq(Arrays.asList(HAKUKOHDEOID)));
        // varmennetaan että hakemuspalvelun palauttama hakemus päätyy
        // laskentapalvelulle
        verify(valintalaskentaService, only()).valintakokeet(eq(HAKEMUS), anyListOf(ValintaperusteetTyyppi.class));
    }
}
