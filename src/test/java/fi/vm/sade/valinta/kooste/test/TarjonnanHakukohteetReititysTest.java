package fi.vm.sade.valinta.kooste.test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

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
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.TarjontaTila;
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;
import fi.vm.sade.valinta.kooste.tarjonta.TarjontaHakukohteetAktivointiResource;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@ContextConfiguration(classes = TarjonnanHakukohteetReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource({ "classpath:META-INF/spring/context/valintakoe-context.xml",
        "classpath:META-INF/spring/context/valintalaskenta-context.xml", "test-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TarjonnanHakukohteetReititysTest {

    @Autowired
    private TarjontaHakukohteetAktivointiResource tarjontaResource;

    @Bean
    public HakemusService getHakemusServiceMock() {
        return mock(HakemusService.class);
    }

    @Bean
    public ValintalaskentaService getValintalaskentaServiceMock() {
        return mock(ValintalaskentaService.class);
    }

    @Bean
    public ValintaperusteService getValintaperusteServiceMock() {
        return mock(ValintaperusteService.class);
    }

    private static String[] HAKUKOHDE_OIDS = { "0000-2tg2wgg-dfgsdh", "0000-252rtgwg5-dfgsdh",
            "0000-bwe45gb54g-dfgsdh", "0000-wgv34hgw4h5-dfgsdh", "0000-24h5w2rt4y24-dfgsdh", "0000-gfb54y5436-dfgsdh" };

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
        when(tarjontaService.haeTarjonta(anyString())).thenReturn(tarjonta);
        return tarjontaService;
    }

    @Autowired
    private HakemusService hakemusService;

    @Test
    public void testTarjonnanHakukohteetReititin() {
        // aktivoidaan jollain satunnaisella oidilla tarjonta palvelu hakemaan
        // kaikki hakukohteet
        tarjontaResource.aktivoiValintalaskenta(UUID.randomUUID().toString());
        // verifioidaan että hakemuspalvelua on kutsuttu ainakin kerran
        // jokaisella oidilla
        for (String oid : HAKUKOHDE_OIDS) {
            verify(hakemusService, atLeastOnce()).haeHakemukset(eq(Arrays.asList(oid)));
        }
    }
}
