package fi.vm.sade.valinta.kooste.laskentakerralla;


import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorSystem;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaStarter;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaKerrallaHandler;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaKerrallaResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaStatusExcelHandler;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import org.junit.BeforeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public class LaskentaKerrallaBase {
    static ValintalaskentaKerrallaRouteValvomo valintalaskentaKerrallaRouteValvomo = mock(ValintalaskentaKerrallaRouteValvomo.class);
    static ApplicationAsyncResource applicationAsyncResource = mock(ApplicationAsyncResource.class);
    static ValintaperusteetAsyncResource valintaperusteetAsyncResource = mock(ValintaperusteetAsyncResource.class);
    static OhjausparametritAsyncResource ohjausparametritAsyncResource = mock(OhjausparametritAsyncResource.class);
    static LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource = mock(LaskentaSeurantaAsyncResource.class);
    static ValintalaskentaAsyncResource valintalaskentaAsyncResource = mock(ValintalaskentaAsyncResource.class);
    static SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = mock(SuoritusrekisteriAsyncResource.class);
    static ValintalaskentaStatusExcelHandler valintalaskentaStatusExcelHandler = mock(ValintalaskentaStatusExcelHandler.class);
    static LaskentaActorSystem laskentaActorSystem = spy(new LaskentaActorSystem(laskentaSeurantaAsyncResource, new LaskentaStarter(ohjausparametritAsyncResource,valintaperusteetAsyncResource,laskentaSeurantaAsyncResource),new LaskentaActorFactory(
            valintalaskentaAsyncResource,
            applicationAsyncResource,
            valintaperusteetAsyncResource,
            laskentaSeurantaAsyncResource,
            suoritusrekisteriAsyncResource
            ), 8));

    @Autowired
    ValintalaskentaKerrallaResource valintalaskentaKerralla;

    @BeforeClass
    public static void resetMocks() {
        reset(valintalaskentaKerrallaRouteValvomo);
        reset(applicationAsyncResource);
        reset(valintaperusteetAsyncResource);
        reset(ohjausparametritAsyncResource);
        reset(laskentaSeurantaAsyncResource);
        reset(valintalaskentaAsyncResource);
        reset(suoritusrekisteriAsyncResource);
    }

    @Configuration
    static class ContextConfiguration {
        @Bean
        public ApplicationAsyncResource orderService() {
            return applicationAsyncResource;
        }
        @Bean
        public ValintalaskentaKerrallaRouteValvomo valintalaskentaKerrallaRouteValvomo() {
            return valintalaskentaKerrallaRouteValvomo;
        }
        @Bean
        public ValintaperusteetAsyncResource valintaperusteetAsyncResource() {
            return valintaperusteetAsyncResource;
        }
        @Bean
        public OhjausparametritAsyncResource ohjausparametritAsyncResource() {
            return ohjausparametritAsyncResource;
        }
        @Bean
        public LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource() {
            return laskentaSeurantaAsyncResource;
        }
        @Bean
        public ValintalaskentaAsyncResource valintalaskentaAsyncResource() {
            return valintalaskentaAsyncResource;
        }
        @Bean
        public ValintalaskentaStatusExcelHandler valintalaskentaStatusExcelHandler() {
            return valintalaskentaStatusExcelHandler;
        }
        @Bean
        public SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource() {
            return suoritusrekisteriAsyncResource;
        }
        @Bean
        public ValintalaskentaKerrallaResource valintalaskentaKerrallaResource() {
            return new ValintalaskentaKerrallaResource();
        }
        @Bean
        public ValintalaskentaKerrallaHandler valintalaskentaKerrallaService() {
            return new ValintalaskentaKerrallaHandler();
        }
        @Bean
        public ValintalaskentaKerrallaRoute valintalaskentaKerrallaRoute() {
            return laskentaActorSystem;
        }
    }
}
