package fi.vm.sade.valinta.kooste.laskentakerralla;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl.TarjontaAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockAuthorityCheckService;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorSystem;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaStarter;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaKerrallaService;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaKerrallaResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaStatusExcelHandler;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRoute;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

@Configuration
public class LaskentaKerrallaContext {

    public LaskentaKerrallaContext() {
    }

    @Bean
    public ApplicationAsyncResource orderService() {
        return Mocks.applicationAsyncResource;
    }

    @Bean
    public ValintaperusteetAsyncResource valintaperusteetAsyncResource() {
        return Mocks.valintaperusteetAsyncResource;
    }

    @Bean
    public OhjausparametritAsyncResource ohjausparametritAsyncResource() {
        return Mocks.ohjausparametritAsyncResource;
    }

    @Bean
    public LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource() {
        return Mocks.laskentaSeurantaAsyncResource;
    }

    @Bean
    public ValintalaskentaAsyncResource valintalaskentaAsyncResource() {
        return Mocks.valintalaskentaAsyncResource;
    }

    @Bean
    public ValintalaskentaStatusExcelHandler valintalaskentaStatusExcelHandler() {
        return Mocks.valintalaskentaStatusExcelHandler;
    }

    @Bean
    public SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource() {
        return Mocks.suoritusrekisteriAsyncResource;
    }

    @Bean
    public ValintalaskentaKerrallaResource valintalaskentaKerrallaResource() {
        return new ValintalaskentaKerrallaResource();
    }

    @Bean
    public ValintalaskentaKerrallaService valintalaskentaKerrallaService() {
        return new ValintalaskentaKerrallaService();
    }

    @Bean
    public LaskentaActorSystem valintalaskentaKerrallaRoute() {
        return Mocks.laskentaActorSystem;
    }

    @Bean
    public OrganisaatioResource organisaatioResource() {
        return Mocks.organisaatioResource;
    }

    @Bean
    public TarjontaAsyncResource tarjontaAsyncResource() {
        return Mocks.tarjontaAsyncResource;
    }

    @Bean
    public AuthorityCheckService authorityCheckService() {
        return new MockAuthorityCheckService();
    }
}
