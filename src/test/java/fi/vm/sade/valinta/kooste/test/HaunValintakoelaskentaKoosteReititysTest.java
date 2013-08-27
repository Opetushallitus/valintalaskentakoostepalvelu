package fi.vm.sade.valinta.kooste.test;

import fi.vm.sade.service.hakemus.HakemusService;
import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.hakemus.schema.HakukohdeTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.types.TarjontaTila;
import fi.vm.sade.tarjonta.service.types.TarjontaTyyppi;
import fi.vm.sade.valinta.kooste.paasykokeet.HaunValintakoelaskentaAktivointiProxy;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Jussi Jartamo
 */
@Configuration
@ContextConfiguration(classes = HaunValintakoelaskentaKoosteReititysTest.class)
@PropertySource("classpath:test.properties")
@ImportResource({"classpath:META-INF/spring/context/valintakoe-context.xml",
        "classpath:META-INF/spring/context/valintalaskenta-context.xml", "test-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class HaunValintakoelaskentaKoosteReititysTest {

    @Autowired
    private HaunValintakoelaskentaAktivointiProxy haunValintakoelaskentaAktivointiProxy;

    @Bean
    public HakemusService getHakemusServiceMock() {
        HakemusService hakemusService = mock(HakemusService.class);

        for (String hakukohde : HAKUKOHDE_OIDS) {
            List<HakemusTyyppi> hakemukset = new ArrayList<HakemusTyyppi>();
            for (int i = 1; i <= 2; ++i) {
                HakemusTyyppi h = new HakemusTyyppi();
                h.setHakemusOid(hakukohde + i);
                h.setHakijanEtunimi("etu");
                h.setHakijanSukunimi("suku");
                h.setHakijaOid("oid" + i);

                HakukohdeTyyppi hk = new HakukohdeTyyppi();
                hk.setPrioriteetti(1);
                hk.setHakukohdeOid(hakukohde);
                h.getHakutoive().add(hk);
                hakemukset.add(h);
            }

            when(hakemusService.haeHakemukset(eq(Arrays.asList(hakukohde)))).thenReturn(hakemukset);
        }

        return hakemusService;
    }

    @Bean
    public ValintalaskentaService getValintalaskentaServiceMock() {
        return mock(ValintalaskentaService.class);
    }

    @Bean
    public ValintaperusteService getValintaperusteServiceMock() {
        return mock(ValintaperusteService.class);
    }

    @Bean
    public ParametriService getParametriService() {
        return mock(ParametriService.class);
    }

    private static String[] HAKUKOHDE_OIDS = {"0000-2tg2wgg-dfgsdh", "0000-252rtgwg5-dfgsdh",
            "0000-bwe45gb54g-dfgsdh", "0000-wgv34hgw4h5-dfgsdh", "0000-24h5w2rt4y24-dfgsdh", "0000-gfb54y5436-dfgsdh"};

    private static int HAKEMUSTEN_LKM = HAKUKOHDE_OIDS.length * 2;

    @Bean
    public TarjontaPublicService getTarjontaPublicServiceMock() {
        TarjontaPublicService tarjontaService = mock(TarjontaPublicService.class);
        TarjontaTyyppi tarjonta = new TarjontaTyyppi();
        for (String oid : HAKUKOHDE_OIDS) {
            fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi hakukohde =
                    new fi.vm.sade.tarjonta.service.types.HakukohdeTyyppi();
            hakukohde.setOid(oid);
            hakukohde.setHakukohteenTila(TarjontaTila.JULKAISTU);
            tarjonta.getHakukohde().add(hakukohde);
        }
        when(tarjontaService.haeTarjonta(anyString())).thenReturn(tarjonta);
        return tarjontaService;
    }

    @Autowired
    private HakemusService hakemusService;

    @Autowired
    private ValintaperusteService valintaperusteService;

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    @Test
    public void testTarjonnanHakukohteetReititin() {
        // aktivoidaan jollain satunnaisella oidilla tarjonta palvelu hakemaan
        // kaikki hakukohteet
        haunValintakoelaskentaAktivointiProxy.aktivoiValintakoelaskenta(UUID.randomUUID().toString());
        // verifioidaan että hakemuspalvelua on kutsuttu ainakin kerran
        // jokaisella oidilla
        for (String oid : HAKUKOHDE_OIDS) {
            verify(hakemusService, atLeastOnce()).haeHakemukset(eq(Arrays.asList(oid)));
            verify(valintaperusteService, times(HAKEMUSTEN_LKM)).haeValintaperusteet(anyList());
            verify(valintalaskentaService, times(HAKEMUSTEN_LKM)).valintakokeet(any(HakemusTyyppi.class), anyList());
        }

    }
}
