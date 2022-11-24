package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.Sijoittelu;
import fi.vm.sade.valinta.kooste.sijoittelu.route.SijoitteluAktivointiRoute;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

/** @author Jussi Jartamo */
public class SijoitteluRouteTest {

  private SijoitteleAsyncResource sijoitteluResource =
      (hakuOid, successCallback, failureCallback) -> successCallback.accept("OK");

  private SijoitteluAktivointiRoute sijoitteluAktivointiRoute =
      new SijoitteluRouteImpl(sijoitteluResource);

  @Test
  public void testaaReitti() {
    Sijoittelu sijoittelu = new Sijoittelu(StringUtils.EMPTY);
    sijoitteluAktivointiRoute.aktivoiSijoittelu(sijoittelu);
    Assert.assertEquals(true, sijoittelu.isValmis());
  }
}
