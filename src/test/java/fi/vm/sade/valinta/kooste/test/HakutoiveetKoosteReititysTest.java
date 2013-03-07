package fi.vm.sade.valinta.kooste.test;

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.valinta.kooste.paasykokeet.HakuPaasykokeetAktivointiResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.ValintalaskentaAktivointiResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Testaa että Camel-reitit menee läpi ja että kutsun viimeinen palvelu
 *         saa oikeat arvot reititysketjun läpi
 * 
 */
@Configuration
@ContextConfiguration(classes = HakutoiveetKoosteReititysTest.class)
@ImportResource("classpath:test-camel-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class HakutoiveetKoosteReititysTest {

    private static final Logger LOG = LoggerFactory.getLogger(HakutoiveetKoosteReititysTest.class);

    private static final String HAKEMUSOID = "hakemus0";
    private static final String HAKUTOIVEOID = "hakutoive0";
    private static final String HAKUKOHDEOID = "hakukohde0";
    private static final Integer VALINNANVAIHE = 6;

    @Autowired
    private HakuPaasykokeetAktivointiResource hakuPaasykokeetResource;

    @Autowired
    private ValintalaskentaAktivointiResource valintalaskentaResource;

    @Bean
    public HakemusService getHakemusServiceMock() {
        HakemusService hakemusMock = mock(HakemusService.class);
        // when(hakemusMock.haeHakutoiveet(anyString())).thenReturn(Collections.<HakutoiveTyyppi>
        // emptyList());
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
        vtyyppi.setValinnanVaiheJarjestysluku(VALINNANVAIHE);
        when(valintaperusteMock.haeValintaperusteet(anyListOf(HakuparametritTyyppi.class))).thenReturn(
                Arrays.asList(vtyyppi));
        return valintaperusteMock;
    }

    @Bean
    public ValintalaskentaService getValintalaskentaService() {
        return mock(ValintalaskentaService.class);
    }

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    @Test
    public void testHakutoiveetKooste() {
        Response r = hakuPaasykokeetResource.aktivoiPaasykokeidenHaku(HAKUTOIVEOID);
        LOG.debug("Dokumentin tavut {}", Arrays.toString((byte[]) r.getEntity()));
    }

    @Test
    public void testLaskentaKooste() {
        valintalaskentaResource.aktivoiValintalaskenta(HAKUKOHDEOID, VALINNANVAIHE);

        verify(valintalaskentaService, atLeastOnce()).laske(eq(HAKUKOHDEOID), eq(VALINNANVAIHE),
                anyListOf(HakemusTyyppi.class), anyListOf(ValintaperusteetTyyppi.class));
    }

}
