package fi.vm.sade.valinta.kooste.valintalaskenta;

import fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl.ApplicationAsyncResourceImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * @author Jussi Jartamo @Ignore lokaaliin testaukseen. CAS virheen todentamiseen.
 */
@Disabled
public class AsyncErrorTest {

  @Test
  public void v() throws InterruptedException {
    ApplicationContext context = null;
    ApplicationAsyncResourceImpl a = new ApplicationAsyncResourceImpl(null);
    String hakuOid = "1.2.246.562.5.2013080813081926341927";
    String hakukohdeOid = "1.2.246.562.5.25812040993";

    a.getApplicationOids(hakuOid, hakukohdeOid)
        .subscribe(
            r -> {
              // NOP
              int i = 1 + 1;
            },
            e -> {
              // NOP
              int i = 1 + 1;
            });
    Thread.sleep(5000L);
  }
}
