package fi.vm.sade.valinta.kooste.viestintapalvelu;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.valinta.kooste.KoostepalveluContext;
import fi.vm.sade.valinta.kooste.exception.HakemuspalveluException;
import fi.vm.sade.valinta.kooste.sijoittelu.proxy.SijoitteluKoulutuspaikallisetProxy;
import fi.vm.sade.valinta.kooste.tarjonta.TarjontaNimiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HaeOsoiteKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.HyvaksymiskirjeetKomponentti;
import fi.vm.sade.valinta.kooste.viestintapalvelu.resource.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.HyvaksymiskirjeRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.HyvaksymiskirjeRouteImpl;
import fi.vm.sade.valinta.kooste.viestintapalvelu.route.impl.ViestintapalveluConfig;

/**
 * 
 * @author Jussi Jartamo
 * 
 */
@Configuration
@Import({ HyvaksymiskirjeetKomponentti.class, HyvaksymiskirjeRouteImpl.class })
@ContextConfiguration(classes = { HyvaksymiskirjeetTest.class, KoostepalveluContext.CamelConfig.class,
        ViestintapalveluConfig.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class HyvaksymiskirjeetTest {

    @Autowired
    private HyvaksymiskirjeRoute hyvaksymiskirjeRoute;

    @Test(expected = HakemuspalveluException.class)
    public void testaaHyvaksymiskirjeetReitti() {
        hyvaksymiskirjeRoute.hyvaksymiskirjeetAktivointi("1", "2", 3L);
    }

    @Bean
    public ViestintapalveluResource getViestintapalveluResource() {
        return Mockito.mock(ViestintapalveluResource.class);
    }

    @Bean
    public SijoitteluKoulutuspaikallisetProxy getSijoitteluKoulutuspaikallisetProxy() {
        return Mockito.mock(SijoitteluKoulutuspaikallisetProxy.class);
    }

    @Bean
    public TarjontaNimiProxy getTarjontaNimiProxy() {
        return Mockito.mock(TarjontaNimiProxy.class);
    }

    @Bean
    public HaeOsoiteKomponentti getHaeOsoiteKomponentti() {
        return Mockito.mock(HaeOsoiteKomponentti.class);
    }

}
