package fi.vm.sade.valinta.kooste.laskentakerralla;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorSystem;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaStarter;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaStatusExcelHandler;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import org.junit.BeforeClass;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

public class Mocks {
    static ValintalaskentaKerrallaRouteValvomo valintalaskentaKerrallaRouteValvomo = mock(ValintalaskentaKerrallaRouteValvomo.class);
    static ApplicationAsyncResource applicationAsyncResource = mock(ApplicationAsyncResource.class);
    static ValintaperusteetAsyncResource valintaperusteetAsyncResource = mock(ValintaperusteetAsyncResource.class);
    static OhjausparametritAsyncResource ohjausparametritAsyncResource = mock(OhjausparametritAsyncResource.class);
    static LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource = mock(LaskentaSeurantaAsyncResource.class);
    static ValintalaskentaAsyncResource valintalaskentaAsyncResource = mock(ValintalaskentaAsyncResource.class);
    static SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = mock(SuoritusrekisteriAsyncResource.class);
    static TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
    static ValintalaskentaStatusExcelHandler valintalaskentaStatusExcelHandler = mock(ValintalaskentaStatusExcelHandler.class);
    static LaskentaActorSystem laskentaActorSystem = spy(new LaskentaActorSystem(laskentaSeurantaAsyncResource, new LaskentaStarter(ohjausparametritAsyncResource,valintaperusteetAsyncResource,laskentaSeurantaAsyncResource, tarjontaAsyncResource),new LaskentaActorFactory(
            valintalaskentaAsyncResource,
            applicationAsyncResource,
            valintaperusteetAsyncResource,
            laskentaSeurantaAsyncResource,
            suoritusrekisteriAsyncResource
    ), 8));

    public static void resetMocks() {
        reset(valintalaskentaKerrallaRouteValvomo);
        reset(applicationAsyncResource);
        reset(valintaperusteetAsyncResource);
        reset(ohjausparametritAsyncResource);
        reset(laskentaSeurantaAsyncResource);
        reset(valintalaskentaAsyncResource);
        reset(suoritusrekisteriAsyncResource);
        reset(tarjontaAsyncResource);
        reset(valintalaskentaStatusExcelHandler);
        reset(laskentaActorSystem);
    }

}
