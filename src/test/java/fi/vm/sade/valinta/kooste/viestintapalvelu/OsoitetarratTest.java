package fi.vm.sade.valinta.kooste.viestintapalvelu;

import java.util.Arrays;
import java.util.Collections;

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
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.exception.ViestintapalveluException;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluKoulutuspaikallisetRoute;
import fi.vm.sade.valinta.kooste.valintatieto.komponentti.ValintatietoHakukohteelleKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoite;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
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
@ContextConfiguration(classes = { OsoitetarratTest.class, KoostepalveluContext.CamelConfig.class,
        ViestintapalveluConfig.class })
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
        HakemusOsallistuminenTyyppi h0 = createValintatieto("1", Osallistuminen.EI_OSALLISTU);
        HakemusOsallistuminenTyyppi h1 = createValintatieto("2", Osallistuminen.OSALLISTUU);

        Mockito.when(
                valintatietoHakukohteelleKomponentti.valintatiedotHakukohteelle(Mockito.anyListOf(String.class),
                        Mockito.anyString())).thenReturn(Arrays.asList(h0, h1));

        Osoite o = Mockito.mock(Osoite.class);

        Mockito.when(osoiteKomponentti.haeOsoite(Mockito.anyString())).thenReturn(o);

        osoitetarratRoute.osoitetarratAktivointi("h0", Arrays.asList("v0", "v1"));

    }

    @Test(expected = ViestintapalveluException.class)
    public void testaaFailaakoOikein() {
        Mockito.when(
                valintatietoHakukohteelleKomponentti.valintatiedotHakukohteelle(Mockito.anyListOf(String.class),
                        Mockito.anyString())).thenReturn(Collections.<HakemusOsallistuminenTyyppi> emptyList());

        osoitetarratRoute.osoitetarratAktivointi("h0", Arrays.asList("v0", "v1"));

    }

    private HakemusOsallistuminenTyyppi createValintatieto(String oid, Osallistuminen o) {
        HakemusOsallistuminenTyyppi h = new HakemusOsallistuminenTyyppi();
        h.setHakemusOid(oid);
        ValintakoeOsallistuminenTyyppi o0 = new ValintakoeOsallistuminenTyyppi();
        o0.setOsallistuminen(o);
        h.getOsallistumiset().add(o0);
        return h;
    }

    @Bean
    public SijoitteluKoulutuspaikallisetRoute getSijoitteluKoulutuspaikallisetProxy() {
        return Mockito.mock(SijoitteluKoulutuspaikallisetRoute.class);
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
    public HaeHakemusKomponentti getHaeHakemusKomponentti() {
        return Mockito.mock(HaeHakemusKomponentti.class);
    }

    @Bean
    public HaeOsoiteKomponentti getHaeOsoiteKomponentti() {
        return Mockito.mock(HaeOsoiteKomponentti.class);
    }
}
