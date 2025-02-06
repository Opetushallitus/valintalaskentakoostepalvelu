package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.ViestintapalveluAsyncResource;
import javax.annotation.PostConstruct;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * @author Jussi Jartamo
 */
@Profile("mockresources")
@Service
public class Mocks {

  private static Mocks MOCKS;

  @Autowired private ViestintapalveluAsyncResource viestintapalveluAsyncResource;
  @Autowired private KoodistoAsyncResource koodistoAsyncResource;
  @Autowired private OhjausparametritAsyncResource ohjausparametritAsyncResource;
  @Autowired private ValintapisteAsyncResource valintapisteAsyncResource;

  @PostConstruct
  public void init() {
    MOCKS = this;
  }

  public static ViestintapalveluAsyncResource getViestintapalveluAsyncResource() {
    return MOCKS.viestintapalveluAsyncResource;
  }

  public static OhjausparametritAsyncResource getOhjausparametritAsyncResource() {
    return MOCKS.ohjausparametritAsyncResource;
  }

  public static ValintapisteAsyncResource getValintapisteAsyncResource() {
    return MOCKS.valintapisteAsyncResource;
  }

  public static KoodistoAsyncResource getKoodistoAsyncResource() {
    return MOCKS.koodistoAsyncResource;
  }

  public static void reset() {
    Mockito.reset(
        getValintapisteAsyncResource(),
        getViestintapalveluAsyncResource(),
        getKoodistoAsyncResource());
  }
}
