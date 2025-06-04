package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeKoosteTietoDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl.ValintalaskentaAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl.ValintaperusteetAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HaunHakukohdeTulosTiedotRajaimille;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.TulosTiedotHakukohdeRajaimille;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.impl.ValintaTulosServiceAsyncResourceImpl;
import fi.vm.sade.valintalaskenta.domain.valinta.HakukohdeLaskentaTehty;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");

  @Before
  public void setUp() throws ParseException {
    Date varasijatayttoPaattyy = dateFormat.parse("31.01.2020 00:00");
    HakukohdeKoosteTietoDTO hakukohdeKooste =
        new HakukohdeKoosteTietoDTO(HAKUKOHDE_OID_2, true, varasijatayttoPaattyy);

    when(valintalaskentaAsyncResource.hakukohteidenLaskennanTila(eq(HAKU_OID)))
        .thenReturn(
            CompletableFuture.completedFuture(
                List.of(new HakukohdeLaskentaTehty(HAKUKOHDE_OID_1, new Date()))));

    when(valintaTulosServiceAsyncResource.getHaunHakukohdeTiedot(eq(HAKU_OID)))
        .thenReturn(
            CompletableFuture.completedFuture(
                new HaunHakukohdeTulosTiedotRajaimille(HAKU_OID, Set.of())));

    when(valintaperusteetAsyncResource.haunHakukohdeTiedot(eq(HAKU_OID)))
        .thenReturn(CompletableFuture.completedFuture(List.of(hakukohdeKooste)));
  }

  @Test
  public void hakeeValintatiedotHakukohteittain() throws ParseException {
    Map<String, HakukohteenValintatiedot> result =
        service.valintatiedotHakukohteittain(HAKU_OID).join();
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_1, null, true, null, false, false),
        result.get(HAKUKOHDE_OID_1));
    assertEquals(
        new HakukohteenValintatiedot(
            HAKUKOHDE_OID_2, true, false, dateFormat.parse("31.01.2020 00:00"), false, false),
        result.get(HAKUKOHDE_OID_2));
  }

  @Test
  public void hakeeValintatiedotHakukohteittainTulosTiedoilla() throws ParseException {
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
        new HakukohteenValintatiedot(HAKUKOHDE_OID_1, null, true, null, false, false),
        result.get(HAKUKOHDE_OID_1));
    assertEquals(
        new HakukohteenValintatiedot(
            HAKUKOHDE_OID_2, true, false, dateFormat.parse("31.01.2020 00:00"), true, false),
        result.get(HAKUKOHDE_OID_2));
    assertEquals(
        new HakukohteenValintatiedot(hakukohdeOid3, null, false, null, true, true),
        result.get(hakukohdeOid3));
  }
}
