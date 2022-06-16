package fi.vm.sade.valinta.kooste.laskentakerralla;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockOppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.tarjonta.api.OrganisaatioResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.KoskiService;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorFactory;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaActorSystem;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.LaskentaStarter;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaStatusExcelHandler;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;

public class Mocks {
  static OrganisaatioResource organisaatioResource = mock(OrganisaatioResource.class);
  static ValintalaskentaKerrallaRouteValvomo valintalaskentaKerrallaRouteValvomo = mock(
      ValintalaskentaKerrallaRouteValvomo.class);
  static ApplicationAsyncResource applicationAsyncResource = mock(ApplicationAsyncResource.class);
  static AtaruAsyncResource ataruAsyncResource = mock(AtaruAsyncResource.class);
  static HarkinnanvaraisuusAsyncResource harkinnanvaraisuusAsyncResource = mock(
      HarkinnanvaraisuusAsyncResource.class);
  static ValintaperusteetAsyncResource valintaperusteetAsyncResource = mock(ValintaperusteetAsyncResource.class);
  static OhjausparametritAsyncResource ohjausparametritAsyncResource = mock(OhjausparametritAsyncResource.class);
  static LaskentaSeurantaAsyncResource laskentaSeurantaAsyncResource = mock(LaskentaSeurantaAsyncResource.class);
  static ValintalaskentaAsyncResource valintalaskentaAsyncResource = mock(ValintalaskentaAsyncResource.class);
  static SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource = mock(SuoritusrekisteriAsyncResource.class);
  static TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
  static ValintalaskentaStatusExcelHandler valintalaskentaStatusExcelHandler = mock(
      ValintalaskentaStatusExcelHandler.class);
  static ValintapisteAsyncResource valintapisteAsyncResource = mock(ValintapisteAsyncResource.class);
  static KoskiService koskiService = mock(KoskiService.class);
  static HakemuksetConverterUtil hakemuksetConverterUtil = new HakemuksetConverterUtil("9999-12-31",
      harkinnanvaraisuusAsyncResource);
  static OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource = new MockOppijanumerorekisteriAsyncResource();
  static LaskentaActorSystem laskentaActorSystem = spy(new LaskentaActorSystem(laskentaSeurantaAsyncResource,
      new LaskentaStarter(ohjausparametritAsyncResource, valintaperusteetAsyncResource,
          laskentaSeurantaAsyncResource, tarjontaAsyncResource),
      new LaskentaActorFactory(5, valintalaskentaAsyncResource, applicationAsyncResource, ataruAsyncResource,
          valintaperusteetAsyncResource, laskentaSeurantaAsyncResource, suoritusrekisteriAsyncResource,
          tarjontaAsyncResource, valintapisteAsyncResource, koskiService, hakemuksetConverterUtil,
          oppijanumerorekisteriAsyncResource),
      8));

  public static void resetMocks() {
    reset(valintalaskentaKerrallaRouteValvomo);
    reset(applicationAsyncResource);
    reset(ataruAsyncResource);
    reset(valintaperusteetAsyncResource);
    reset(ohjausparametritAsyncResource);
    reset(laskentaSeurantaAsyncResource);
    reset(valintalaskentaAsyncResource);
    reset(suoritusrekisteriAsyncResource);
    reset(tarjontaAsyncResource);
    reset(valintapisteAsyncResource);
    reset(valintalaskentaStatusExcelHandler);
    reset(laskentaActorSystem);
  }
}
