package fi.vm.sade.valinta.kooste.kela;

import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import javax.annotation.Resource;

import org.apache.camel.CamelContext;
import org.apache.camel.component.mock.MockEndpoint;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService;
import fi.vm.sade.valinta.kooste.kela.dto.KelaProsessi;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaFtpRouteImpl;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;
import fi.vm.sade.valinta.kooste.valvomo.dto.ProsessiJaStatus;
import fi.vm.sade.valinta.kooste.valvomo.service.ValvomoService;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@ContextConfiguration(classes = { KoostepalveluContext.CamelConfig.class, KelaFtpRouteTest.class, KelaRouteConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class KelaFtpRouteTest {

    private static final Logger LOG = LoggerFactory.getLogger(KelaFtpRouteTest.class);
    private static final String FTP_MOCK = "mock:ftpMock";
    private static final String FTP_CONFIG = "retainFirst=1";

    @Bean
    public KelaFtpRouteImpl getKelaRouteImpl() {
        /**
         * Ylikirjoitetaan kela-ftp endpoint logitusreitilla yksikkotestia
         * varten!
         */
        return new KelaFtpRouteImpl(FTP_MOCK, FTP_CONFIG);
    }

    @Bean
    public SendMessageToDocumentService getSendMessageToDocumentService() {
        return new SendMessageToDocumentService();
    }

    @Bean
    public DokumenttiResource mockDokumenttiResource() {
        return mock(DokumenttiResource.class);
    }

    @Autowired
    private KelaFtpRoute kelaFtpRoute;
    @Autowired
    private DokumenttiResource dokumenttiResource;
    @Autowired
    private CamelContext context;
    @Resource(name = "kelaValvomo")
    private ValvomoService<KelaProsessi> kelaValvomo;
    @Autowired
    private KelaFtpRouteImpl ftpRouteImpl;

    @Test
    public void testKelaFtpSiirto() {

        String dokumenttiId = "dokumenttiId";
        Mockito.when(dokumenttiResource.lataa(Mockito.anyString())).thenReturn(
                new ByteArrayInputStream(dokumenttiId.getBytes()));

        kelaFtpRoute.aloitaKelaSiirto(dokumenttiId);

        Collection<ProsessiJaStatus<KelaProsessi>> prosessit = kelaValvomo.getUusimmatProsessitJaStatukset();
        // START JA FAILURE Prosessit
        Assert.assertTrue(prosessit.size() == 2);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (ProsessiJaStatus<KelaProsessi> p : prosessit) {
            LOG.info("{}", gson.toJson(p));
        }

        MockEndpoint resultEndpoint = context.getEndpoint(ftpRouteImpl.getFtpKelaSiirto(), MockEndpoint.class);
        resultEndpoint.assertExchangeReceived(0).getIn(ByteArrayInputStream.class);
        Mockito.verify(dokumenttiResource).lataa(Mockito.eq(dokumenttiId));
    }

}
