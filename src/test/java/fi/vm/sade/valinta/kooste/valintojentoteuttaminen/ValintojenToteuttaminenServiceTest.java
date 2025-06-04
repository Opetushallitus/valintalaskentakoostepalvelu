package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl.ValintalaskentaAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl.ValintaperusteetAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HaunHakukohdeTulosTiedotRajaimille;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.TulosTiedotHakukohdeRajaimille;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl.ValintaTulosServiceAsyncResourceImpl;
import fi.vm.sade.valintalaskenta.domain.valinta.HakukohdeLaskentaTehty;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.junit.Before;
import org.junit.Test;

public class ValintojenToteuttaminenServiceTest {

  private final ValintalaskentaAsyncResource valintalaskentaAsyncResource =
      mock(ValintalaskentaAsyncResourceImpl.class);

  private final ValintaperusteetAsyncResourceImpl valintaperusteetAsyncResource =
      mock(ValintaperusteetAsyncResourceImpl.class);

  private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource =
      mock(ValintaTulosServiceAsyncResourceImpl.class);

  private final ValintojenToteuttaminenService service =
      new ValintojenToteuttaminenServiceImpl(
          valintaperusteetAsyncResource,
          valintalaskentaAsyncResource,
          valintaTulosServiceAsyncResource);

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
    when(valintaTulosServiceAsyncResource.getHaunHakukohdeTiedot(eq(HAKU_OID)))
        .thenReturn(
            CompletableFuture.completedFuture(
                new HaunHakukohdeTulosTiedotRajaimille(HAKU_OID, Set.of())));
  }

  @Test
  public void hakeeValintatiedotHakukohteittain() {
    Map<String, HakukohteenValintatiedot> result =
        service.valintatiedotHakukohteittain(HAKU_OID).join();
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_1, false, true, false, false),
        result.get(HAKUKOHDE_OID_1));
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_2, true, false, false, false),
        result.get(HAKUKOHDE_OID_2));
  }

  @Test
  public void hakeeValintatiedotHakukohteittainTulosTiedoilla() {
    final String hakukohdeOid3 = "123.123.123.333";
    when(valintaTulosServiceAsyncResource.getHaunHakukohdeTiedot(eq(HAKU_OID)))
        .thenReturn(
            CompletableFuture.completedFuture(
                new HaunHakukohdeTulosTiedotRajaimille(
                    HAKU_OID,
                    Set.of(
                        new TulosTiedotHakukohdeRajaimille(HAKUKOHDE_OID_2, true, false),
                        new TulosTiedotHakukohdeRajaimille(hakukohdeOid3, true, true)))));
    Map<String, HakukohteenValintatiedot> result =
        service.valintatiedotHakukohteittain(HAKU_OID).join();
    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_1, false, true, false, false),
        result.get(HAKUKOHDE_OID_1));
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_2, true, false, true, false),
        result.get(HAKUKOHDE_OID_2));
    assertEquals(
        new HakukohteenValintatiedot(hakukohdeOid3, false, false, true, true),
        result.get(hakukohdeOid3));
  }
}
