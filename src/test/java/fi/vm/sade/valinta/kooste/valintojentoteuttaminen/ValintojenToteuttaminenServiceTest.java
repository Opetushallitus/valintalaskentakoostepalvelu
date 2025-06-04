package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeKoosteTietoDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.impl.OhjausparametritAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.impl.ValintalaskentaAsyncResourceImpl;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.impl.ValintaperusteetAsyncResourceImpl;
import fi.vm.sade.valintalaskenta.domain.valinta.HakukohdeLaskentaTehty;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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

  private final OhjausparametritAsyncResourceImpl ohjausparametritAsyncResource =
      mock(OhjausparametritAsyncResourceImpl.class);

  private final ValintojenToteuttaminenService service =
      new ValintojenToteuttaminenServiceImpl(
          valintaperusteetAsyncResource,
          valintalaskentaAsyncResource,
          ohjausparametritAsyncResource);

  private static final String HAKU_OID = "123.123.123.123";
  private static final String HAKUKOHDE_OID_1 = "123.123.123.111";
  private static final String HAKUKOHDE_OID_2 = "123.123.123.222";

  private static Date getDate() {
    try {
      return new SimpleDateFormat("dd.MM.yyyy HH:mm").parse("31.01.2020 00:00");
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      return new Date();
    }
  }

  @Before
  public void setUp() {
    Date date = getDate();
    HakukohdeKoosteTietoDTO hakukohdeKooste =
        new HakukohdeKoosteTietoDTO(HAKUKOHDE_OID_2, true, date);

    when(valintalaskentaAsyncResource.hakukohteidenLaskennanTila(eq(HAKU_OID)))
        .thenReturn(
            CompletableFuture.completedFuture(
                List.of(
                    new HakukohdeLaskentaTehty(HAKUKOHDE_OID_1, date),
                    new HakukohdeLaskentaTehty(HAKUKOHDE_OID_2, date))));
    when(valintalaskentaAsyncResource.hakukohteidenLaskennanTila(eq(HAKU_OID)))
        .thenReturn(
            CompletableFuture.completedFuture(
                List.of(new HakukohdeLaskentaTehty(HAKUKOHDE_OID_1, date))));

    when(ohjausparametritAsyncResource.haeHaunOhjausparametrit(eq(HAKU_OID)))
        .thenReturn(CompletableFuture.completedFuture(new ParametritDTO()));

    when(valintaperusteetAsyncResource.haunHakukohdeTiedot(eq(HAKU_OID)))
        .thenReturn(CompletableFuture.completedFuture(List.of(hakukohdeKooste)));
  }

  @Test
  public void fetchesValintatiedotHakukohteittain() {
    Map<String, HakukohteenValintatiedot> result =
        service.valintatiedotHakukohteittain(HAKU_OID).join();
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_1, false, true, null),
        result.get(HAKUKOHDE_OID_1));
    assertEquals(
        new HakukohteenValintatiedot(HAKUKOHDE_OID_2, true, false, getDate()),
        result.get(HAKUKOHDE_OID_2));
  }
}
