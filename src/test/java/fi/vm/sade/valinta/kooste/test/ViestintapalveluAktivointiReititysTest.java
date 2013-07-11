package fi.vm.sade.valinta.kooste.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

import fi.vm.sade.valinta.kooste.viestintapalvelu.HyvaksymiskirjeBatchAktivointiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.JalkiohjauskirjeBatchAktivointiProxy;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@RunWith(Enclosed.class)
public class ViestintapalveluAktivointiReititysTest {

    private static final String HYVAKSYMISKIRJE_BATCH_JSON = "viestintapalvelu/data/hyvaksymiskirjebatch.json";
    private static final String JALKIOHJAUSKIRJE_BATCH_JSON = "viestintapalvelu/data/jalkiohjauskirjebatch.json";

    /**
     * 
     * @author Jussi Jartamo
     * 
     * @Ignore Testi on ignoroitava koska ei voida olettaa Viestintapalvelun
     *         olemassa oloa Bamboo-buildin yhteydessa.
     */
    @Ignore
    @Configuration
    @PropertySource("classpath:META-INF/valintalaskentakoostepalvelu.properties")
    @ContextConfiguration(classes = KunYhteydenottoViestintapalveluun.class)
    @ImportResource({ "classpath:META-INF/spring/context/viestintapalvelu-context.xml" })
    public static class KunYhteydenottoViestintapalveluun extends AbstractJUnit4SpringContextTests {

        @Autowired
        HyvaksymiskirjeBatchAktivointiProxy hyvaksymiskirjeBatchProxy;

        @Test
        public void hoitaaHyvaksymiskirjeenUudelleenohjauksenDokumenttiResurssiinOikein() throws IOException {
            assertTrue(hyvaksymiskirjeBatchProxy.hyvaksymiskirjeBatchAktivointi(Resources.toString(
                    Resources.getResource(HYVAKSYMISKIRJE_BATCH_JSON), Charsets.UTF_8)).length > 0);
        }

        @Autowired
        JalkiohjauskirjeBatchAktivointiProxy jalkiohjauskirjeBatchProxy;

        @Test
        public void hoitaaJalkiohjauskirjeenUudelleenohjauksenDokumenttiResurssiinOikein() throws IOException {
            assertTrue(jalkiohjauskirjeBatchProxy.jalkiohjauskirjeBatchAktivoi(Resources.toString(
                    Resources.getResource(JALKIOHJAUSKIRJE_BATCH_JSON), Charsets.UTF_8)).length > 0);
        }

    }

}
