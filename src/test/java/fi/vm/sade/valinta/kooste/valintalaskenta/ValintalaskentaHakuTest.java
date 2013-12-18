package fi.vm.sade.valinta.kooste.valintalaskenta;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakukohteenHakemuksetKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakukohteetTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.HaeValintaperusteetKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.komponentti.SuoritaLaskentaKomponentti;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.HaunValintalaskentaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaConfig;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.impl.ValintalaskentaRouteImpl;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@Import({ ValintalaskentaRouteImpl.class, SuoritaLaskentaKomponentti.class })
@ContextConfiguration(classes = { ValintalaskentaHakuTest.class, KoostepalveluContext.CamelConfig.class,
        ValintalaskentaConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class ValintalaskentaHakuTest {

    @Autowired
    private HaunValintalaskentaRoute valintalaskentaRoute;

    @Autowired
    private HaeHakukohteetTarjonnaltaKomponentti haeHakukohteetTarjonnaltaKomponentti;
    @Autowired
    private HaeHakukohteenHakemuksetKomponentti haeHakukohteenHakemuksetKomponentti;

    @Autowired
    private HaeHakemusKomponentti haeHakemusKomponentti;

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    private SuppeaHakemus create() {
        SuppeaHakemus s = new SuppeaHakemus();
        s.setFirstNames("f");
        s.setLastName("l");
        s.setOid("suppeaoid1");
        s.setPersonOid("personoid1");
        s.setSsn("ssn");
        s.setState("state");
        return s;
    }

    @Test
    public void testaaPalvelutToimiiReitti() {
        HakukohdeTyyppi h = new HakukohdeTyyppi();
        h.setOid("hakukohdeOid1");
        SuppeaHakemus s = create();

        Hakemus hakemus = new Hakemus();
        hakemus.setOid("hakemusoid1");

        Mockito.reset(haeHakukohteetTarjonnaltaKomponentti, haeHakukohteenHakemuksetKomponentti, haeHakemusKomponentti);
        Mockito.when(haeHakukohteetTarjonnaltaKomponentti.haeHakukohteetTarjonnalta(Mockito.anyString()))
        //
                .thenReturn(Arrays.asList(h));

        Mockito.when(haeHakukohteenHakemuksetKomponentti.haeHakukohteenHakemukset(Mockito.anyString()))
        //
                .thenReturn(Arrays.asList(s));

        Mockito.when(haeHakemusKomponentti.haeHakemus(Mockito.anyString()))
        //
                .thenReturn(hakemus);

        valintalaskentaRoute.aktivoiValintalaskenta("h0");

        Mockito.verify(valintalaskentaService, Mockito.atLeastOnce()).laske(Mockito.anyListOf(HakemusTyyppi.class),
                Mockito.isNull(List.class));

    }

    public void testaaHakuPalveluEpaonnistuuSatunnaisestiReitti() {

    }

    @Test
    public void testaaTarjontaEpaonnistuuSatunnaisestiReitti() {
        Mockito.reset(haeHakukohteetTarjonnaltaKomponentti, haeHakukohteenHakemuksetKomponentti);

        Mockito.when(haeHakukohteetTarjonnaltaKomponentti.haeHakukohteetTarjonnalta(Mockito.anyString()))
        //
                .thenThrow(new RuntimeException("412: Autentikaatio tai vastaava meni rikki!"));
        valintalaskentaRoute.aktivoiValintalaskenta("h0");

        HakukohdeTyyppi h = new HakukohdeTyyppi();
        h.setOid("hakukohdeOid1");

        Mockito.reset(haeHakukohteetTarjonnaltaKomponentti, haeHakukohteenHakemuksetKomponentti, haeHakemusKomponentti);

        Mockito.when(haeHakukohteetTarjonnaltaKomponentti.haeHakukohteetTarjonnalta(Mockito.anyString()))
        //
                .thenReturn(Arrays.asList(h, h, h));

        SuppeaHakemus s = create();
        Mockito.when(haeHakukohteenHakemuksetKomponentti.haeHakukohteenHakemukset(Mockito.anyString()))
        //
                .thenReturn(Arrays.asList(s));

        Mockito.when(haeHakemusKomponentti.haeHakemus(Mockito.anyString()))
        //
                .thenThrow(new RuntimeException("Hakemuksen haku epäonnistui!"));

        valintalaskentaRoute.aktivoiValintalaskenta("h0");

        Mockito.verify(haeHakemusKomponentti, Mockito.atLeastOnce()).haeHakemus(Mockito.anyString());
    }

    public void testaaValintalaskentaEpaonnistuuSatunnaisestiReitti() {

    }

    @Bean
    public ValintalaskentaService getValintalaskentaService() {
        return Mockito.mock(ValintalaskentaService.class);
    }

    @Bean
    public HaeHakemusKomponentti getHaeHakemusKomponentti() {
        return Mockito.mock(HaeHakemusKomponentti.class);
    }

    @Bean
    public HaeHakukohteenHakemuksetKomponentti getHaeHakukohteenHakemuksetKomponentti() {
        return Mockito.mock(HaeHakukohteenHakemuksetKomponentti.class);
    }

    @Bean
    public HaeValintaperusteetKomponentti getHaeValintaperusteetKomponentti() {
        return Mockito.mock(HaeValintaperusteetKomponentti.class);
    }

    @Bean
    public HaeHakukohteetTarjonnaltaKomponentti getHaeHakukohteetTarjonnaltaKomponentti() {
        return Mockito.mock(HaeHakukohteetTarjonnaltaKomponentti.class);
    }
}
