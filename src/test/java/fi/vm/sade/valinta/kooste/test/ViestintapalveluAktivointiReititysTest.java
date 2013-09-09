package fi.vm.sade.valinta.kooste.test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.OsoitetarratAktivointiProxy;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * @author Jussi Jartamo
 */
@RunWith(Enclosed.class)
public class ViestintapalveluAktivointiReititysTest {

    private static final String HYVAKSYMISKIRJE_BATCH_JSON = "viestintapalvelu/data/hyvaksymiskirjebatch.json";
    private static final String JALKIOHJAUSKIRJE_BATCH_JSON = "viestintapalvelu/data/jalkiohjauskirjebatch.json";
    private static final String ADDRESSLABEL_BATCH_JSON = "viestintapalvelu/data/addresslabelbatch.json";

    /**
     * @author Jussi Jartamo
     * @Ignore Testi on ignoroitava koska ei voida olettaa Viestintapalvelun
     * olemassa oloa Bamboo-buildin yhteydessa.
     */
    @Ignore
    @Configuration
    @PropertySource("classpath:META-INF/valintalaskentakoostepalvelu.properties")
    @ContextConfiguration(classes = KunYhteydenottoViestintapalveluun.class)
    @ImportResource({"classpath:META-INF/spring/context/viestintapalvelu-context.xml"})
    public static class KunYhteydenottoViestintapalveluun extends AbstractJUnit4SpringContextTests {

        @Autowired
        OsoitetarratAktivointiProxy addressLabelBatchProxy;


        @Bean
        public ValintatietoService getValintatietoService() {
            return Mockito.mock(ValintatietoService.class);
        }


        @Test
        public void osoitteidenValitysToimiiOikeinJaSaadaanPalautetta() throws IOException {
            assertTrue(URI.create(addressLabelBatchProxy.osoitetarratAktivointi(
                    Resources.toString(Resources.getResource(ADDRESSLABEL_BATCH_JSON), Charsets.UTF_8),
                    Arrays.asList(""))) != null);
        }
        /*
         * @Autowired HyvaksymiskirjeBatchAktivointiProxy
         * hyvaksymiskirjeBatchProxy;
         * 
         * @Test public void
         * hoitaaHyvaksymiskirjeenUudelleenohjauksenDokumenttiResurssiinOikein()
         * throws IOException { assertTrue(URI.create(hyvaksymiskirjeBatchProxy.
         * hyvaksymiskirjeBatchAktivointi(Resources.toString(
         * Resources.getResource(HYVAKSYMISKIRJE_BATCH_JSON), Charsets.UTF_8)))
         * != null); }
         * 
         * @Autowired JalkiohjauskirjeBatchAktivointiProxy
         * jalkiohjauskirjeBatchProxy;
         * 
         * @Test public void
         * hoitaaJalkiohjauskirjeenUudelleenohjauksenDokumenttiResurssiinOikein
         * () throws IOException {
         * assertTrue(URI.create(jalkiohjauskirjeBatchProxy
         * .jalkiohjauskirjeBatchAktivoi(Resources.toString(
         * Resources.getResource(JALKIOHJAUSKIRJE_BATCH_JSON), Charsets.UTF_8)))
         * != null); }
         */
    }

}
