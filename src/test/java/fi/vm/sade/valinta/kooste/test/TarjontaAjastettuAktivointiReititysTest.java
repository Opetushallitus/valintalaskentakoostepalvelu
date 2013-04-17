package fi.vm.sade.valinta.kooste.test;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.camel.CamelContext;
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
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;
import fi.vm.sade.valinta.kooste.tarjonta.TarjontaHakuTiedotPalvelu;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Testaa CRON-ajastetun reitittimen laukaisua. TODO: Alustava toteutus.
 *         CRON-ajastettua valintakoelaskennan käynnistystä ei ole vielä
 *         speksattu (16.4.2013)
 */
@Configuration
@ContextConfiguration(classes = TarjontaAjastettuAktivointiReititysTest.class)
@PropertySource({ "classpath:test.properties", "classpath:tarjonta-ajastin.properties" })
// Jos kontekstien järjestyksen muuttaa niin camel voi heittää poikkeusta kun
// kutsuttu reitti ei ole ehtinyt vielä lataantua. Yksikkötesti toimii
// timeouttien ansiosta vaikka latausjärjestys olisi epäoptimaalinen.
@ImportResource({ "classpath:META-INF/spring/context/valintakoe-context.xml",
        "classpath:META-INF/spring/context/valintalaskenta-context.xml",
        "classpath:META-INF/spring/context/tarjonta-ajastus-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TarjontaAjastettuAktivointiReititysTest {

    private final String HAKU_OID = "hakuoid";

    @Bean(name = "tarjontaTiedotPalvelu")
    public TarjontaHakuTiedotPalvelu getTarjontaHakuTiedotPalvelu() {
        TarjontaHakuTiedotPalvelu tarjontaPalvelu = mock(TarjontaHakuTiedotPalvelu.class);
        // anyString()
        when(tarjontaPalvelu.haeHakuOid()).thenReturn(HAKU_OID);
        return tarjontaPalvelu;
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
            tarjonta.getHakukohde().add(hakukohde);
        }
        when(tarjontaService.haeTarjonta(anyString())).thenReturn(tarjonta);
        return tarjontaService;
    }

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

    @Autowired
    private HakemusService hakemusService;
    @Autowired
    private CamelContext[] contexts;

    @Test
    public void testTarjonnanReititin() throws Exception {
        for (String oid : HAKUKOHDE_OIDS) {
            verify(hakemusService, timeout(10000).atLeastOnce()).haeHakemukset(eq(Arrays.asList(oid)));
        }
    }
}
