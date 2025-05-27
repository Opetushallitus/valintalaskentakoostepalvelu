package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.dto.HakukohdeLaskentaTehty;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl.ValintalaskentaAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl.ValintaperusteetAsyncResourceImpl;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;

public class ValintojenToteuttaminenServiceTest {

  private final ValintalaskentaAsyncResource valintalaskentaAsyncResource =
      mock(ValintalaskentaAsyncResourceImpl.class);

  private final ValintaperusteetAsyncResourceImpl valintaperusteetAsyncResource =
      mock(ValintaperusteetAsyncResourceImpl.class);

  private final ValintojenToteuttaminenService service =
      new ValintojenToteuttaminenServiceImpl(
          valintaperusteetAsyncResource, valintalaskentaAsyncResource);

  private static final String HAKU_OID = "123.123.123.123";
  private static final String HAKUKOHDE_OID_1 = "123.123.123.111";
  private static final String HAKUKOHDE_OID_2 = "123.123.123.222";

  @Before
  public void setUp() {
    HakukohdeViiteDTO dto = new HakukohdeViiteDTO();
    dto.setOid(HAKUKOHDE_OID_2);
    when(valintalaskentaAsyncResource.hakukohteidenLaskennanTila(eq(HAKU_OID)))
        .thenReturn(
            CompletableFuture.completedFuture(
                List.of(new HakukohdeLaskentaTehty(HAKUKOHDE_OID_1, new Date()))));
    when(valintaperusteetAsyncResource.haunHakukohteetF(eq(HAKU_OID), eq(true)))
        .thenReturn(CompletableFuture.completedFuture(List.of(dto)));
  }

  @Test
  public void fetchesValintatiedotHakukohteittain() {
    Map<String, HakukohteenValintatiedot> result =
        service.valintatiedotHakukohteittain(HAKU_OID).join();
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_1, false, true), result.get(HAKUKOHDE_OID_1));
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_2, true, false), result.get(HAKUKOHDE_OID_2));
  }
}
