package fi.vm.sade.valinta.kooste.external.resource.suorituspalvelu.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.HarkinnanvaraisuudenSyy;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakutoiveenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.testapp.MockServicesApp;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SuorituspalveluAsyncResourceImplTest {
  private static final String URL =
      "^http://127.0.0.1:\\d+/suorituspalvelu/api/v1/valinnat/harkinnanvaraisuus$";

  private final RestCasClient casClient = mock(RestCasClient.class);
  private final SuorituspalveluAsyncResourceImpl suorituspalvelu =
      new SuorituspalveluAsyncResourceImpl(casClient);

  @BeforeEach
  public void setUp() {
    reset(casClient);
    MockServicesApp.start();
  }

  @Test
  public void haeHarkinnanvaraisuudetPalauttaaSuorituspalvelunTiedot() throws Exception {
    HakemuksenHarkinnanvaraisuus harkinnanvaraisuus =
        new HakemuksenHarkinnanvaraisuus(
            "1.2.246.562.11.00000000001",
            List.of(
                new HakutoiveenHarkinnanvaraisuus(
                    "1.2.246.562.20.00000000001", HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET)));
    harkinnanvaraisuus.setHenkiloOid("1.2.246.562.24.00000000001");
    when(casClient.post(any(), any(), any(), any(), anyInt()))
        .thenReturn(CompletableFuture.completedFuture(List.of(harkinnanvaraisuus)));

    List<HakemuksenHarkinnanvaraisuus> tulos =
        suorituspalvelu.haeHarkinnanvaraisuudet(List.of("1.2.246.562.11.00000000001")).get();

    assertEquals(1, tulos.size());
    assertSame(harkinnanvaraisuus, tulos.get(0));
    verify(casClient)
        .post(
            matches(URL),
            eq(new TypeToken<List<HakemuksenHarkinnanvaraisuus>>() {}),
            eq(Map.of("hakemusOids", List.of("1.2.246.562.11.00000000001"))),
            eq(Map.of()),
            eq(20 * 60 * 1000));
  }

  @Test
  public void haeHarkinnanvaraisuudetEiKutsuSuorituspalveluaTyhjallaListalla() throws Exception {
    assertEquals(List.of(), suorituspalvelu.haeHarkinnanvaraisuudet(List.of()).get());
    verifyNoInteractions(casClient);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void haeHarkinnanvaraisuudetJakaaHaunEnintaan5000HakemuksenOsiin() throws Exception {
    List<String> hakemusOids =
        IntStream.range(0, SuorituspalveluAsyncResourceImpl.HAKEMUKSIA_KERRALLAAN + 1)
            .mapToObj(i -> String.format("1.2.246.562.11.%011d", i))
            .collect(Collectors.toList());
    when(casClient.post(any(), any(), any(), any(), anyInt()))
        .thenAnswer(
            invocation -> {
              Map<String, List<String>> body = invocation.getArgument(2);
              return CompletableFuture.completedFuture(
                  body.get("hakemusOids").stream()
                      .map(oid -> new HakemuksenHarkinnanvaraisuus(oid, List.of()))
                      .collect(Collectors.toList()));
            });

    List<HakemuksenHarkinnanvaraisuus> tulos =
        suorituspalvelu.haeHarkinnanvaraisuudet(hakemusOids).get();

    assertEquals(
        hakemusOids, tulos.stream().map(HakemuksenHarkinnanvaraisuus::getHakemusOid).toList());
    ArgumentCaptor<Object> bodies = ArgumentCaptor.forClass(Object.class);
    verify(casClient, times(2)).post(matches(URL), any(), bodies.capture(), any(), anyInt());
    assertEquals(
        List.of(SuorituspalveluAsyncResourceImpl.HAKEMUKSIA_KERRALLAAN, 1),
        bodies.getAllValues().stream()
            .map(body -> ((Map<String, List<String>>) body).get("hakemusOids").size())
            .toList());
  }
}
