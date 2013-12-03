package fi.vm.sade.valinta.kooste.kela;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.vm.sade.rajapinnat.kela.tkuva.data.TKUVAYHVA;
import fi.vm.sade.sijoittelu.tulos.dto.raportointi.HakijaDTO;
import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.komponentti.KelaDokumentinLuontiKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.KelaHakijaRiviKomponentti;
import fi.vm.sade.valinta.kooste.kela.komponentti.impl.KelaDokumentinLuontiKomponenttiImpl;
import fi.vm.sade.valinta.kooste.kela.route.KelaRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteImpl;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteUtils.PrepareKelaProcessDescription;
import fi.vm.sade.valinta.kooste.sijoittelu.komponentti.SijoitteluKaikkiPaikanVastaanottaneet;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus.Status;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

@Configuration
@ContextConfiguration(classes = { KoostepalveluContext.CamelConfig.class, KelaRouteTest.class, KelaRouteConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class KelaRouteTest {

    private static final Logger LOG = LoggerFactory.getLogger(KelaRouteTest.class);

    @Bean
    public KelaRouteImpl getKelaRouteImpl(PrepareKelaProcessDescription processFactory) {
        return new KelaRouteImpl(processFactory);
    }

    @Bean
    public PrepareKelaProcessDescription getPrepareKelaProcessDescription() {
        return mock(PrepareKelaProcessDescription.class);
    }

    @Bean
    public KelaHakijaRiviKomponentti mockKelaHakijaKomponentti() {

        TKUVAYHVA t = new TKUVAYHVA.Builder().setAjankohtaSyksy(true).setEtunimet("J").setSukunimi("J")
                .setHenkilotunnus("021293-915F").setLinjakoodi("0000").setOppilaitos("0000")
                .setPoimintapaivamaara(new Date()).setLukuvuosi(new Date()).setValintapaivamaara(new Date()).build();

        KelaHakijaRiviKomponentti hakija = mock(KelaHakijaRiviKomponentti.class);
        when(hakija.luo(Mockito.notNull(HakijaDTO.class), Mockito.notNull(Date.class), Mockito.notNull(Date.class)))
                .thenReturn(t);
        return hakija;
    }

    @Bean
    public SendMessageToDocumentService getSendMessageToDocumentService() {
        return new SendMessageToDocumentService();
    }

    @Bean
    public KelaDokumentinLuontiKomponentti getKelaDokumentinLuontiKomponentti() {
        return new KelaDokumentinLuontiKomponenttiImpl();
    }

    @Bean
    public DokumenttiResource mockDokumenttiResource() {
        return mock(DokumenttiResource.class);
    }

    @Bean
    public SijoitteluKaikkiPaikanVastaanottaneet mockSijoitteluKaikkiPaikanVastaanottaneet() {
        return mock(SijoitteluKaikkiPaikanVastaanottaneet.class);
    }

    @Autowired
    private KelaRoute kelaSiirtoDokumentinLuonti;

    @Autowired
    private SijoitteluKaikkiPaikanVastaanottaneet sijoitteluVastaanottaneet;

    @Autowired
    private KelaHakijaRiviKomponentti kelaHakijaKomponentti;

    @Resource(name = "kelaValvomo")
    private ValvomoService<KelaProsessi> kelaValvomo;

    @Autowired
    private PrepareKelaProcessDescription processFactory;

    @Test
    public void testKelaRouteGetsAllVariables() {
        KelaProsessi prosessi = new KelaProsessi("", "", "");
        when(processFactory.prepareProcess(anyString(), anyString(), anyString())).thenReturn(prosessi);
        when(sijoitteluVastaanottaneet.vastaanottaneet(anyString())).thenReturn(
                Arrays.asList(new HakijaDTO(), new HakijaDTO(), new HakijaDTO(), new HakijaDTO(), new HakijaDTO()));

        // Authentication auth = mock(Authentication.class);
        Date lukuvuosi = new DateTime(2013, 11, 1, 1, 1).toDate();
        Date poimintapaivamaara = new Date();
        String hakuOid = "hakuOid";
        String aineistonNimi = "aineistonNimi";
        String organisaationNimi = "organisaationNimi";

        //
        // Aloitetaan dokumentin luonti
        //
        kelaSiirtoDokumentinLuonti.aloitaKelaLuonti(hakuOid, lukuvuosi, poimintapaivamaara, aineistonNimi,
                organisaationNimi);

        verify(sijoitteluVastaanottaneet, Mockito.only()).vastaanottaneet(eq(hakuOid));

        verify(kelaHakijaKomponentti, times(5)).luo(Mockito.notNull(HakijaDTO.class), eq(lukuvuosi),
                eq(poimintapaivamaara));

        List<ProsessiJaStatus<KelaProsessi>> prosessit = haeProsessit(prosessi.getId());

        // START JA FINISH Prosessit
        Assert.assertTrue(prosessit.size() == 2);

        Assert.assertEquals(Status.STARTED, prosessit.get(1).getStatus());
        Assert.assertEquals(Status.FINISHED, prosessit.get(0).getStatus());

    }

    private List<ProsessiJaStatus<KelaProsessi>> haeProsessit(final String id) {
        return Lists.newArrayList(Collections2.filter(kelaValvomo.getUusimmatProsessitJaStatukset(),
                new Predicate<ProsessiJaStatus<KelaProsessi>>() {
                    public boolean apply(ProsessiJaStatus<KelaProsessi> p) {
                        return id.equals(p.getProsessi().getId());
                    }
                }));

    }

    @Test
    public void testFailingSijoittelu() {
        KelaProsessi prosessi = new KelaProsessi("", "", "");
        when(processFactory.prepareProcess(anyString(), anyString(), anyString())).thenReturn(prosessi);

        when(sijoitteluVastaanottaneet.vastaanottaneet(anyString())).thenThrow(new RuntimeException());

        kelaSiirtoDokumentinLuonti.aloitaKelaLuonti("", new Date(), new Date(), "", "");

        List<ProsessiJaStatus<KelaProsessi>> prosessit = haeProsessit(prosessi.getId());
        // START JA FAILURE Prosessit
        Assert.assertTrue(prosessit.size() == 2);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (ProsessiJaStatus<KelaProsessi> p : prosessit) {
            LOG.info("{}", gson.toJson(p));
        }
        Assert.assertEquals(Status.STARTED, prosessit.get(1).getStatus());
        Assert.assertEquals(Status.FAILED, prosessit.get(0).getStatus());
    }
}
