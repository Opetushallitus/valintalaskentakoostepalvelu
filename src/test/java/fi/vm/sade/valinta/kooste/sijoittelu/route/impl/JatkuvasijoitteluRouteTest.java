package fi.vm.sade.valinta.kooste.sijoittelu.route.impl;

import fi.vm.sade.valinta.kooste.external.resource.sijoittelu.SijoitteleAsyncResource;
import fi.vm.sade.valinta.kooste.sijoittelu.dto.DelayedSijoittelu;
import fi.vm.sade.valinta.seuranta.resource.SijoittelunSeurantaResource;
import fi.vm.sade.valinta.seuranta.sijoittelu.dto.SijoitteluDto;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/** @author jussija */
public class JatkuvasijoitteluRouteTest {
  private final DelayQueue<DelayedSijoittelu> jatkuvaSijoitteluDelayedQueue = new DelayQueue<>();
  private final SijoittelunSeurantaResource sijoittelunSeurantaResource =
      Mockito.mock(SijoittelunSeurantaResource.class);
  private final SijoitteleAsyncResource sijoitteluResource =
      Mockito.mock(SijoitteleAsyncResource.class);

  private final ConcurrentHashMap<String, Long> ajossaHakuOids =
      Mockito.spy(new ConcurrentHashMap<>());
  private final JatkuvaSijoitteluRouteImpl jatkuvaSijoitteluRouteImpl =
      new JatkuvaSijoitteluRouteImpl(
          false,
          5,
          sijoitteluResource,
          sijoittelunSeurantaResource,
          jatkuvaSijoitteluDelayedQueue,
          ajossaHakuOids);

  @Test
  public void testaaJatkuvaSijoitteluRouteSamaaHakuaEiLaitetaJonoonMoneenOtteeseen() {
    final String HK = "hk1";
    Mockito.reset(sijoittelunSeurantaResource, sijoitteluResource);
    SijoitteluDto s = new SijoitteluDto(HK, true, null, null, new Date(), 1);
    Mockito.when(sijoittelunSeurantaResource.hae()).thenReturn(Arrays.asList(s));
    for (int i = 0; i < 2; ++i) {
      jatkuvaSijoitteluRouteImpl.teeJatkuvaSijoittelu();

      Assert.assertFalse(jatkuvaSijoitteluRouteImpl.haeJonossaOlevatSijoittelut().isEmpty());
      Assert.assertTrue(jatkuvaSijoitteluRouteImpl.haeJonossaOlevatSijoittelut().size() == 1);
    }
  }

  @Test
  public void testaaJatkuvaSijoitteluRouteAjossaOlevaaHakuaEiLaitetaUudestaanJonoon() {
    final String HK = "hk1";
    Mockito.reset(sijoittelunSeurantaResource, sijoitteluResource);
    SijoitteluDto s = new SijoitteluDto(HK, true, null, null, new Date(), 1);
    Mockito.when(sijoittelunSeurantaResource.hae()).thenReturn(Arrays.asList(s));
    jatkuvaSijoitteluRouteImpl.teeJatkuvaSijoittelu();
    DelayedSijoittelu sijoittelu = jatkuvaSijoitteluDelayedQueue.poll();
    Assert.assertFalse("exchange oli null", sijoittelu == null);
    jatkuvaSijoitteluRouteImpl.kaynnistaJatkuvaSijoittelu(sijoittelu);

    Assert.assertTrue(
        "ei mennyt ajoon!",
        ajossaHakuOids.entrySet().stream()
            .filter(e -> HK.equals(e.getKey()))
            .distinct()
            .findFirst()
            .isPresent());
    jatkuvaSijoitteluRouteImpl.teeJatkuvaSijoittelu();
    Assert.assertTrue(
        "ei saa menna uudestaan tyojonoon koska oli ajossa",
        jatkuvaSijoitteluRouteImpl.haeJonossaOlevatSijoittelut().isEmpty());
  }
}
