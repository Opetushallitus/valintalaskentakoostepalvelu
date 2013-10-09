package fi.vm.sade.valinta.kooste.test;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fi.vm.sade.service.hakemus.schema.HakemusTyyppi;
import fi.vm.sade.service.valintalaskenta.ValintalaskentaService;
import fi.vm.sade.service.valintaperusteet.ValintaperusteService;
import fi.vm.sade.service.valintaperusteet.messages.HakuparametritTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.TavallinenValinnanVaiheTyyppi;
import fi.vm.sade.service.valintaperusteet.schema.ValintaperusteetTyyppi;
import fi.vm.sade.tarjonta.service.TarjontaPublicService;
import fi.vm.sade.tarjonta.service.resources.HakukohdeResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.ApplicationResource;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.HakemusList;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.SuppeaHakemus;
import fi.vm.sade.valinta.kooste.parametrit.service.ParametriService;
import fi.vm.sade.valinta.kooste.valintalaskenta.ValintalaskentaAktivointiResource;

/**
 * User: wuoti Date: 9.9.2013 Time: 10.16
 */
@Configuration
@ContextConfiguration(classes = HakukohteenValintalaskentaKoosteReititysTest.class)
@PropertySource({ "classpath:META-INF/valintalaskentakoostepalvelu.properties", "classpath:test.properties" })
@ImportResource({ "classpath:META-INF/spring/context/haku-context.xml",
        "classpath:META-INF/spring/context/valintalaskenta-context.xml", "test-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class HakukohteenValintalaskentaKoosteReititysTest {
    private static final Logger LOG = LoggerFactory.getLogger(HakukohteenValintalaskentaKoosteReititysTest.class);
    private static final String HAKUOID = "hakuoid";

    private static final String HAKEMUSOID = "hakemus0";
    private static final String HAKUKOHDEOID = "hakukohde0";
    private static final Integer VALINNANVAIHE = 6;

    @Bean
    public HakukohdeResource getHakukohdeResource() {
        return mock(HakukohdeResource.class);
    }

    @Bean
    public ApplicationResource getApplicationResourceMock() {
        ApplicationResource mock = mock(ApplicationResource.class);

        HakemusList hlist = new HakemusList();
        hlist.setTotalCount(1);
        SuppeaHakemus hk = new SuppeaHakemus();
        hk.setOid(HAKEMUSOID);
        hlist.getResults().add(hk);

        when(
                mock.findApplications(anyString(), anyList(), anyString(), anyString(), anyString(), eq(HAKUKOHDEOID),
                        anyInt(), anyInt())).thenReturn(hlist);

        Hakemus hakemus = new Hakemus();
        hakemus.setOid(HAKEMUSOID);
        when(mock.getApplicationByOid(eq(HAKEMUSOID))).thenReturn(hakemus);

        return mock;
    }

    @Bean
    public ValintaperusteService getValintaperusteServiceMock() {
        ValintaperusteService valintaperusteMock = mock(ValintaperusteService.class);
        ValintaperusteetTyyppi vtyyppi = new ValintaperusteetTyyppi();
        vtyyppi.setHakukohdeOid(HAKUKOHDEOID);
        TavallinenValinnanVaiheTyyppi vaihe = new TavallinenValinnanVaiheTyyppi();
        vaihe.setValinnanVaiheJarjestysluku(VALINNANVAIHE);

        when(valintaperusteMock.haeValintaperusteet(anyListOf(HakuparametritTyyppi.class))).thenReturn(
                Arrays.asList(vtyyppi));
        return valintaperusteMock;
    }

    @Bean
    public ValintalaskentaService getValintalaskentaService() {
        return mock(ValintalaskentaService.class);
    }

    @Bean
    public ParametriService getParametriService() {
        ParametriService parametriService = mock(ParametriService.class);
        when(parametriService.valintalaskentaEnabled(HAKUOID)).thenReturn(true);
        return parametriService;
    }

    @Bean(name = "tarjontaServiceClientAsAdmin")
    public TarjontaPublicService getTarjontaPublicServiceMock() {
        return mock(TarjontaPublicService.class);
    }

    @Autowired
    private ValintalaskentaService valintalaskentaService;

    @Autowired
    private ApplicationResource applicationResourceMock;

    @Autowired
    private ValintalaskentaAktivointiResource valintalaskentaResource;

    @Test
    public void testLaskentaKooste() {
        SecurityContextHolder.getContext().setAuthentication(mock(Authentication.class));
        valintalaskentaResource.aktivoiHakukohteenValintalaskenta(HAKUKOHDEOID, VALINNANVAIHE);
        // verify that hakemusservice was indeed called with REST argument!
        verify(applicationResourceMock, times(1)).findApplications(anyString(), anyList(), anyString(), anyString(),
                anyString(), eq(HAKUKOHDEOID), anyInt(), anyInt());
        verify(applicationResourceMock, times(1)).getApplicationByOid(eq(HAKEMUSOID));
        // verify that the route ended calling valintalaskentaservice!
        verify(valintalaskentaService, times(1)).laske(anyListOf(HakemusTyyppi.class),
                anyListOf(ValintaperusteetTyyppi.class));
    }

}
