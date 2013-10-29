package fi.vm.sade.valinta.kooste.test;

import javax.ws.rs.core.Response;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.service.valintatiedot.ValintatietoService;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.haku.HakemusProxy;
import fi.vm.sade.valinta.kooste.sijoittelu.proxy.SijoitteluIlmankoulutuspaikkaaProxy;
import fi.vm.sade.valinta.kooste.sijoittelu.proxy.SijoitteluKoulutuspaikallisetProxy;
import fi.vm.sade.valinta.kooste.tarjonta.TarjontaNimiProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.ViestintapalveluResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Kirjeet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.Osoitteet;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.ViestintapalveluHyvaksymiskirjeetProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.ViestintapalveluJalkiohjauskirjeetProxy;
import fi.vm.sade.valinta.kooste.viestintapalvelu.proxy.ViestintapalveluOsoitetarratProxy;

/**
 * 
 * @author Jussi Jartamo
 * 
 *         Testaa uudelleen yrityksia, eli epavarmaa verkkoyhteytta!
 */
@Configuration
@ContextConfiguration(classes = ViestintapalveluRetryTesti.class)
@PropertySource({ "classpath:META-INF/valintalaskentakoostepalvelu.properties", "classpath:test.properties" })
@ImportResource({ "classpath:test-auth-context.xml", "classpath:META-INF/spring/context/valintatieto-context.xml",
        "classpath:META-INF/spring/context/viestintapalvelu-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class ViestintapalveluRetryTesti {

    @Bean(name = "viestintapalveluClient")
    public ViestintapalveluResource getViestintapalveluResource() {
        return new ViestintapalveluResource() {
            final boolean[] fail = { true, true, true, true, true, false, true };
            volatile int counter = 0;

            public void failRandomly() {
                ++counter;
                if (fail[counter % fail.length])
                    throw new RuntimeException("satunnainen verkkovirhe!");
            }

            public Response haeHyvaksymiskirjeet(Kirjeet kirjeet) {
                failRandomly();
                return null;
            }

            public Response haeJalkiohjauskirjeet(Kirjeet kirjeet) {
                failRandomly();
                return null;
            }

            public Response haeOsoitetarrat(Osoitteet osoitteet) {
                failRandomly();
                return null;
            }
        };
    }

    @Bean
    public HakemusProxy getHakemusProxy() {
        return Mockito.mock(HakemusProxy.class);
    }

    @Bean
    public SijoitteluIlmankoulutuspaikkaaProxy getSijoitteluIlmankoulutuspaikkaaProxy() {
        return Mockito.mock(SijoitteluIlmankoulutuspaikkaaProxy.class);
    }

    @Bean
    public SijoitteluKoulutuspaikallisetProxy getSijoitteluKoulutusProxy() {
        return Mockito.mock(SijoitteluKoulutuspaikallisetProxy.class);
    }

    @Bean
    public TarjontaNimiProxy getTarjontaProxy() {
        return Mockito.mock(TarjontaNimiProxy.class);
    }

    @Bean
    public ValintatietoService getValintatietoService() {
        return Mockito.mock(ValintatietoService.class);
    }

    @Bean
    public ApplicationResource getApplicationResource() {
        return Mockito.mock(ApplicationResource.class);
    }

    @Autowired
    ViestintapalveluHyvaksymiskirjeetProxy hyvaksymiskirjeet;
    @Autowired
    ViestintapalveluJalkiohjauskirjeetProxy jalkiohjauskirjeet;
    @Autowired
    ViestintapalveluOsoitetarratProxy osoitetarrat;

    @Test
    public void testaaRetry() {
        for (int times = 0; times < 1; ++times) {
            hyvaksymiskirjeet.haeHyvaksymiskirjeet(null);
            jalkiohjauskirjeet.haeJalkiohjauskirjeet(null);
            osoitetarrat.haeOsoitetarrat(null);
        }
    }

}
