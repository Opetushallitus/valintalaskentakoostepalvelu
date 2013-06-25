package fi.vm.sade.valinta.kooste.test;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.TavallinenValinnanVaiheTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.valinta.kooste.valintalaskenta.ValintalaskentaAktivointiResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Jussi Jartamo
 *         <p/>
 *         Testaa että Camel-reitit menee läpi ja että kutsun viimeinen palvelu
 *         saa oikeat arvot reititysketjun läpi
 */
@Configuration
@ContextConfiguration(classes = HakutoiveetKoosteReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource({"classpath:META-INF/spring/context/valintalaskenta-context.xml", "test-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class HakutoiveetKoosteReititysTest {

    private static final Logger LOG = LoggerFactory.getLogger(HakutoiveetKoosteReititysTest.class);

    private static final String HAKEMUSOID = "hakemus0";
    private static final String HAKUKOHDEOID = "hakukohde0";
    private static final Integer VALINNANVAIHE = 6;

    @Bean
    public HakemusService getHakemusServiceMock() {
        HakemusService hakemusMock = mock(HakemusService.class);
        HakemusTyyppi htyyppi = new HakemusTyyppi();
        htyyppi.setHakemusOid(HAKEMUSOID);
        when(hakemusMock.haeHakemukset(anyListOf(String.class))).thenReturn(Arrays.asList(htyyppi));
        return hakemusMock;
    }

    @Bean
    public ValintaperusteService getValintaperusteServiceMock() {
        ValintaperusteService valintaperusteMock = mock(ValintaperusteService.class);
        ValintaperusteetTyyppi vtyyppi = new ValintaperusteetTyyppi();
        vtyyppi.setHakukohdeOid(HAKUKOHDEOID);
        TavallinenValinnanVaiheTyyppi vaihe = new TavallinenValinnanVaiheTyyppi();
        vaihe.setValinnanVaiheJarjestysluku(VALINNANVAIHE);

        when(valintaperusteMock.haeValintaperusteet(anyListOf(HakuparametritTyyppi.class))).thenReturn(
                Arrays.asList(vtyyppi));
        return valintaperusteMock;
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
    private ValintalaskentaService valintalaskentaService;

    @Autowired
    private HakemusService hakemusService;

    @Autowired
    private ValintalaskentaAktivointiResource valintalaskentaResource;

    @Test
    public void testLaskentaKooste() {
        valintalaskentaResource.aktivoiHakukohteenValintalaskenta(HAKUKOHDEOID, VALINNANVAIHE);
        // verify that hakemusservice was indeed called with REST argument!
        verify(hakemusService, atLeastOnce()).haeHakemukset(eq(Arrays.asList(HAKUKOHDEOID)));
        // verify that the route ended calling valintalaskentaservice!
        verify(valintalaskentaService, atLeastOnce()).laske(anyListOf(HakemusTyyppi.class),
                anyListOf(ValintaperusteetTyyppi.class));
    }

}
