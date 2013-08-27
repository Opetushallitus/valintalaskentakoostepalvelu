package fi.vm.sade.valinta.kooste.test;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.TavallinenValinnanVaiheTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.TarjontaTila;
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
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
 * User: wuoti Date: 27.5.2013 Time: 9.30
 */
@Configuration
@ContextConfiguration(classes = HaunValintalaskentaReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource({ "classpath:META-INF/spring/context/valintalaskenta-context.xml", "test-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class HaunValintalaskentaReititysTest {

    private static final Logger LOG = LoggerFactory.getLogger(HakutoiveetKoosteReititysTest.class);

    private static final String[] HAKUKOHDE_OIDS = { "hakukohdeoid1", "hakukohdeoid2", "hakukohdeoid3",
            "hakukohdeoid4", "hakukohdeoid5", "hakukohdeoid6s" };

    private static final String HAKUOID = "hakuoid";

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

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    @Autowired
    private HakemusService hakemusService;

    @Autowired
    private ValintalaskentaAktivointiResource valintalaskentaResource;

    @Test
    public void testLaskentaKooste() {
        valintalaskentaResource.aktivoiHaunValintalaskenta(HAKUOID);
        // verify that hakemusservice was indeed called with REST argument!

        for (String hakukohdeoid : HAKUKOHDE_OIDS) {
            verify(hakemusService, atLeastOnce()).haeHakemukset(eq(Arrays.asList(hakukohdeoid)));
        }

        // verify that the route ended calling valintalaskentaservice!
        verify(valintalaskentaService, times(HAKUKOHDE_OIDS.length)).laske(anyListOf(HakemusTyyppi.class),
                anyListOf(ValintaperusteetTyyppi.class));
    }
}
