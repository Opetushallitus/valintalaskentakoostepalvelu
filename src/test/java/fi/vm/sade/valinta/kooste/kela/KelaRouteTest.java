package fi.vm.sade.valinta.kooste.kela;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.tarjonta.service.resources.HakuResource;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.hakemus.komponentti.HaeHakemusKomponentti;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaHakijaRiviKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteImpl;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKaikkiPaikanVastaanottaneet;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.HaeHakuTarjonnaltaKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.LinjakoodiKomponentti;
import fi.vm.sade.valinta.kooste.tarjonta.komponentti.OrganisaatioKomponentti;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

@Configuration
@Import(KelaRouteImpl.class)
@ContextConfiguration(classes = { KoostepalveluContext.CamelConfig.class, KelaRouteTest.class, KelaRouteConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
// @DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class KelaRouteTest {

    private static final Logger LOG = LoggerFactory.getLogger(KelaRouteTest.class);

    @Bean
    public HaeHakemusKomponentti getHaeHakemusKomponentti() {
        return mock(HaeHakemusKomponentti.class);
    }

    @Bean
    public KelaHakijaRiviKomponenttiImpl mockKelaHakijaKomponentti() throws Exception {
        //
        // TKUVAYHVA t = new
        // TKUVAYHVA.Builder().setAjankohtaSyksy(true).setEtunimet("J").setSukunimi("J")
        // .setHenkilotunnus("021293-915F").setLinjakoodi("0000").setOppilaitos("0000")
        // .setPoimintapaivamaara(new Date()).setLukuvuosi(new
        // Date()).setValintapaivamaara(new Date()).build();
        //
        // KelaHakijaRiviKomponenttiImpl hakija =
        // mock(KelaHakijaRiviKomponenttiImpl.class);
        // when(hakija.luo(Mockito.notNull(HakijaDTO.class),
        // Mockito.notNull(Date.class), Mockito.notNull(Date.class)))
        // .thenReturn(Arrays.asList(new TKUVAYHVA()));
        return mock(KelaHakijaRiviKomponenttiImpl.class);
    }

    @Bean
    public HakuResource getHakuResource() {
        return mock(HakuResource.class);
    }

    @Bean
    public HaeHakuTarjonnaltaKomponentti getHaeHakuTarjonnaltaKomponentti() {
        return mock(HaeHakuTarjonnaltaKomponentti.class);
    }

    @Bean(name = "dokumenttipalveluRestClient")
    public DokumenttiResource mockDokumenttiResource() {
        return mock(DokumenttiResource.class);
    }

    @Bean
    public KelaDokumentinLuontiKomponenttiImpl getKelaDokumentinLuontiKomponentti() {
        return mock(KelaDokumentinLuontiKomponenttiImpl.class);
    }

    @Bean
    public SijoitteluKaikkiPaikanVastaanottaneet mockSijoitteluKaikkiPaikanVastaanottaneet() {
        return mock(SijoitteluKaikkiPaikanVastaanottaneet.class);
    }

    @Bean
    public LinjakoodiKomponentti getLinjakoodiKomponentti() {
        return mock(LinjakoodiKomponentti.class);
    }

    @Bean
    public OrganisaatioResource getOrganisaatioResource() {
        return mock(OrganisaatioResource.class);
    }

    @Bean
    public OrganisaatioKomponentti getOrganisaatioKomponentti() {
        return mock(OrganisaatioKomponentti.class);
    }

    @Bean
    public HakukohdeResource getHakukohdeResource() {
        return mock(HakukohdeResource.class);
    }

    @Autowired
    private KelaRoute kelaSiirtoDokumentinLuonti;

    @Autowired
    private SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet;

    @Autowired
    private KelaHakijaRiviKomponenttiImpl kelaHakijaKomponentti;

    @Resource(name = "kelaValvomo")
    private ValvomoService<KelaProsessi> kelaValvomo;

    @Autowired
    private KelaDokumentinLuontiKomponenttiImpl kelaDokumentinLuontiKomponentti;

    // VAKIO TESTI MUUTTUJIA
    private final HakijaDTO virheellinen = new HakijaDTO();
    private final HakijaDTO ok = new HakijaDTO();
    private final Date lukuvuosi = new DateTime(2013, 11, 1, 1, 1).toDate();
    private final Date poimintapaivamaara = new Date();
    private final String hakuOid = "hakuOid";
    private final String organisaationNimi = "organisaationNimi";

    @Before
    public void setupMocks() throws Exception {

        when(kelaHakijaKomponentti.luo(Mockito.eq(virheellinen), any(Date.class), any(Date.class))).thenThrow(
                new RuntimeException("Should fails once!"));

        when(kelaHakijaKomponentti.luo(Mockito.eq(ok), any(Date.class), any(Date.class))).thenReturn(
                Arrays.asList(new TKUVAYHVA(), new TKUVAYHVA()));
    }

    final String okAineistonNimi = "testaaEttaOikeinSuoritetustaReitistaSeuraaDokumentti";

    @Test
    public void testaaEttaOikeinSuoritetustaReitistaSeuraaDokumentti() throws Exception {
        // yksiloidaan kutsu aineistonnimella etta yksikkotestit voidaan
        // suorittaa moniajoisesti ilman etta mockit hajoo

        /**
         * pelkkia ok hakijoita
         */
        when(sijoitteluVastaanottaneet.vastaanottaneet(anyString())).thenReturn(
                Arrays.asList(ok, ok, ok, ok, ok, ok, ok, ok, ok, ok, ok, ok, ok, ok, ok));

        kelaSiirtoDokumentinLuonti.aloitaKelaLuonti(hakuOid, lukuvuosi, poimintapaivamaara, okAineistonNimi,
                organisaationNimi);

        // Mockito.verify(kelaDokumentinLuontiKomponentti,
        // Mockito.only()).luo(Mockito.anyCollectionOf(TKUVAYHVA.class),
        // // Mockito.eq(okAineistonNimi)
        // anyString(), anyString());
    }

    final String virheAineistonNimi = "testaaEtteiVirheellisestaReitityksestaSeuraaVirheellistaDokumenttia";

    @Test
    public void testaaEtteiVirheellisestaReitityksestaSeuraaVirheellistaDokumenttia() throws Exception {
        // yksiloidaan kutsu aineistonnimella etta yksikkotestit voidaan
        // suorittaa moniajoisesti ilman etta mockit hajoo

        /**
         * virheellisia ja ok hakijoita sekaisin
         */
        when(sijoitteluVastaanottaneet.vastaanottaneet(anyString())).thenReturn(
                Arrays.asList(ok, ok, ok, virheellinen, ok, ok, ok));

        kelaSiirtoDokumentinLuonti.aloitaKelaLuonti(hakuOid, lukuvuosi, poimintapaivamaara, virheAineistonNimi,
                organisaationNimi);

        Mockito.verify(kelaDokumentinLuontiKomponentti, Mockito.never()).luo(Mockito.anyCollectionOf(TKUVAYHVA.class),
                Mockito.eq(virheAineistonNimi), anyString());
    }

}
