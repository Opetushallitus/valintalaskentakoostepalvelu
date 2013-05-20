package fi.vm.sade.valinta.kooste.test;

import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.schema.HakukohdeImportTyyppi;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;
import fi.vm.sade.valinta.kooste.hakuimport.HakuImportAktivointiResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * User: wuoti
 * Date: 20.5.2013
 * Time: 13.27
 */
@Configuration
@ContextConfiguration(classes = HakuImportKoosteReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource("classpath:META-INF/spring/context/hakuimport-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class HakuImportKoosteReititysTest {

    @Autowired
    private HakuImportAktivointiResource hakuImportAktivointiResource;

    @Bean
    public ValintaperusteService getValintaperusteServiceMock() {
        return mock(ValintaperusteService.class);
    }

    private static final String HAKU_OID = "hakuoid1";

    private static final String[] HAKUKOHDE_OIDS = {"0000-2tg2wgg-dfgsdh", "0000-252rtgwg5-dfgsdh",
            "0000-bwe45gb54g-dfgsdh", "0000-wgv34hgw4h5-dfgsdh", "0000-24h5w2rt4y24-dfgsdh", "0000-gfb54y5436-dfgsdh"};

    @Bean
    public TarjontaPublicService getTarjontaPublicServiceMock() {
        TarjontaPublicService tarjontaService = mock(TarjontaPublicService.class);
        TarjontaTyyppi tarjonta = new TarjontaTyyppi();
        for (String oid : HAKUKOHDE_OIDS) {
            HakukohdeTyyppi hakukohde = new HakukohdeTyyppi();
            hakukohde.setOid(oid);
            tarjonta.getHakukohde().add(hakukohde);
        }
        when(tarjontaService.haeTarjonta(eq(HAKU_OID))).thenReturn(tarjonta);
        return tarjontaService;
    }

    @Autowired
    private ValintaperusteService valintaperusteService;

    @Test
    public void testImportHaku() {
        hakuImportAktivointiResource.aktivoiHakuImport(HAKU_OID);
        ArgumentCaptor<HakukohdeImportTyyppi> argCaptor = ArgumentCaptor.forClass(HakukohdeImportTyyppi.class);
        verify(valintaperusteService, times(HAKUKOHDE_OIDS.length)).tuoHakukohde(argCaptor.capture());

        // Tsekataan, että valintaperusteserviceä kutsuttiin kaikille hakukohde oideille
        outer:
        for (String oid : HAKUKOHDE_OIDS) {
            for (HakukohdeImportTyyppi t : argCaptor.getAllValues()) {
                if (oid.equals(t.getHakukohdeOid())) {
                    assertEquals(HAKU_OID, t.getHakuOid());
                    continue outer;
                }
            }

            fail();
        }
    }
}
