package fi.vm.sade.valinta.kooste.kela;

import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.dokumenttipalvelu.resource.DokumenttiResource;
import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.dokumenttipalvelu.SendMessageToDocumentService;
import fi.vm.sade.valinta.kooste.kela.route.KelaFtpRoute;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaFtpRouteImpl;
import fi.vm.sade.valinta.kooste.kela.route.impl.KelaRouteConfig;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@ContextConfiguration(classes = { KoostepalveluContext.CamelConfig.class, KelaRouteTest.class, KelaRouteConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class KelaFtpRouteTest {

    @Bean
    public KelaFtpRouteImpl getKelaRouteImpl() {
        /**
         * Ylikirjoitetaan kela-ftp endpoint logitusreitilla yksikkotestia
         * varten!
         */
        return new KelaFtpRouteImpl("log:fi.vm.sade.valinta.kooste.kela", "level=INFO");
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

    @Test
    public void testKelaFtpSiirto() {

    }

}
