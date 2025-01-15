package fi.vm.sade.valinta.kooste.laskentakerralla;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuusAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockOppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.actor.KoskiService;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;

public class Mocks {

  static OrganisaatioAsyncResource organisaatioAsyncResource =
      mock(OrganisaatioAsyncResource.class);
  static ApplicationAsyncResource applicationAsyncResource = mock(ApplicationAsyncResource.class);
  static AtaruAsyncResource ataruAsyncResource = mock(AtaruAsyncResource.class);
  static HarkinnanvaraisuusAsyncResource harkinnanvaraisuusAsyncResource =
      mock(HarkinnanvaraisuusAsyncResource.class);
  static ValintaperusteetAsyncResource valintaperusteetAsyncResource =
      mock(ValintaperusteetAsyncResource.class);
  static OhjausparametritAsyncResource ohjausparametritAsyncResource =
      mock(OhjausparametritAsyncResource.class);
  static ValintalaskentaAsyncResource valintalaskentaAsyncResource =
      mock(ValintalaskentaAsyncResource.class);
  static SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource =
      mock(SuoritusrekisteriAsyncResource.class);
  static TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
  static ValintapisteAsyncResource valintapisteAsyncResource =
      mock(ValintapisteAsyncResource.class);
  static KoskiService koskiService = mock(KoskiService.class);
  static HakemuksetConverterUtil hakemuksetConverterUtil =
      new HakemuksetConverterUtil("9999-12-31", "9999-12-31", harkinnanvaraisuusAsyncResource);
  static OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource =
      new MockOppijanumerorekisteriAsyncResource();

  public static void resetMocks() {
    reset(organisaatioAsyncResource);
    reset(applicationAsyncResource);
    reset(ataruAsyncResource);
    reset(valintaperusteetAsyncResource);
    reset(ohjausparametritAsyncResource);
    reset(valintalaskentaAsyncResource);
    reset(suoritusrekisteriAsyncResource);
    reset(tarjontaAsyncResource);
    reset(valintapisteAsyncResource);
  }
}
