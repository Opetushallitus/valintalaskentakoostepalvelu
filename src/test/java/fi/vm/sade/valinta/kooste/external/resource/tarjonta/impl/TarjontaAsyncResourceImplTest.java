package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.ResultV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.kouta.KoutaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.kouta.dto.HakukohderyhmaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultTulos;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.*;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

public class TarjontaAsyncResourceImplTest {
  private static final String TARJONTA_URL =
      "http://test/tarjonta-service/rest/v1/hakukohde/search";
  private static final String TARJONTA_HAKU_URL = "http://test/tarjonta-service/rest/v1/haku/";
  private static final String KOUTA_URL = "http://test/kouta-internal/hakukohde/search/1.2.3.4";
  private static final String HKR_URL = "http://test/search/by-hakukohteet";

  private final HttpClient tarjontaClient = mock(HttpClient.class);
  private final RestCasClient koutaClient = mock(RestCasClient.class);
  private final RestCasClient hakukohderyhmapalveluClient = mock(RestCasClient.class);
  private final OhjausparametritAsyncResource ohjausparametritAsyncResource = mock(OhjausparametritAsyncResource.class);

  private final TarjontaAsyncResourceImpl tarjontaAsyncResource =
      new TarjontaAsyncResourceImpl(tarjontaClient, koutaClient, hakukohderyhmapalveluClient, ohjausparametritAsyncResource);

  private final UrlConfiguration urlConfiguration =
      new UrlConfiguration() {
        @Override
        public String url(String key, Object... params) {
          switch (key) {
            case "tarjonta-service.hakukohde.search":
              return TARJONTA_URL;
            case "kouta-internal.hakukohde.search":
              return KOUTA_URL;
            case "hakukohderyhmapalvelu.hakukohderyhma.search-by-hakukohteet":
              return HKR_URL;
            case "tarjonta-service.haku.hakuoid":
              return TARJONTA_HAKU_URL;
            default:
              return null;
          }
        }
      };

  @Before
  public void init() throws IOException {
    ReflectionTestUtils.setField(tarjontaAsyncResource, "urlConfiguration", urlConfiguration);
    setMocks();
  }

  private void setMocks() {
    when(tarjontaClient.getJson(
            eq(TARJONTA_URL), any(Duration.class), eq(new TypeToken<ResultSearch>() {}.getType())))
        .thenReturn(
            CompletableFuture.completedFuture(new ResultSearch(new ResultTulos(emptyList()))));
    Mockito.mock(KoutaHakukohde.class);
    KoutaHakukohde hk1 = Mockito.mock(KoutaHakukohde.class);
    KoutaHakukohde hk2 = Mockito.mock(KoutaHakukohde.class);
    ReflectionTestUtils.setField(hk1, "oid", "1.2.246.562.20.1");
    ReflectionTestUtils.setField(hk2, "oid", "1.2.246.562.20.2");
    when(koutaClient.get(
            eq(KOUTA_URL), eq(new TypeToken<Set<KoutaHakukohde>>() {}), any(), anyInt()))
        .thenReturn(CompletableFuture.completedFuture(Set.of(hk1, hk2)));

    HakukohderyhmaHakukohde hkr1 =
        new HakukohderyhmaHakukohde(
            "1.2.246.562.20.1", Arrays.asList("1.2.246.562.28.1", "1.2.246.562.28.2"));

    HakukohderyhmaHakukohde hkr2 =
        new HakukohderyhmaHakukohde(
            "1.2.246.562.20.2", Arrays.asList("1.2.246.562.28.1", "1.2.246.562.28.3"));

    when(hakukohderyhmapalveluClient.post(
            eq(HKR_URL),
            eq(new TypeToken<List<HakukohderyhmaHakukohde>>() {}),
            or(
                eq(Arrays.asList("1.2.246.562.20.2", "1.2.246.562.20.1")),
                eq(Arrays.asList("1.2.246.562.20.1", "1.2.246.562.20.2"))),
            anyMap(),
            anyInt()))
        .thenReturn(CompletableFuture.completedFuture(Arrays.asList(hkr1, hkr2)));
  }

  @Test
  public void testHakukohdeRyhmasForHakukohdes() {
    Map<String, List<String>> expected = new HashMap<>();
    expected.put("1.2.246.562.20.1", Arrays.asList("1.2.246.562.28.1", "1.2.246.562.28.2"));
    expected.put("1.2.246.562.20.2", Arrays.asList("1.2.246.562.28.1", "1.2.246.562.28.3"));

    CompletableFuture<Map<String, List<String>>> actual =
        tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes("1.2.246.562.29.00000000000000000800");

    assertEquals(expected, actual.join());
  }

  @Test
  public void testHakukohdeRyhmasForHakukohdesParsesCorrectly() {
    String json =
        "{\"result\": {\"tulokset\": [{\"tulokset\": [{\"oid\": \"oid1\",\"ryhmaliitokset\": [{\"ryhmaOid\": \"ryhmaoid1\"},{\"ryhmaOid\": \"ryhmaoid2\"},{\"ryhmaOid\": \"ryhmaoid3\"}]},{\"oid\": \"oid2\",\"ryhmaliitokset\": []}]}],\"tuloksia\": 1}}";

    ResultSearch a =
        TarjontaAsyncResourceImpl.getGson()
            .fromJson(json, new TypeToken<ResultSearch>() {}.getType());
    Map<String, List<String>> res = TarjontaAsyncResourceImpl.resultSearchToHakukohdeRyhmaMap(a);
    assertEquals(2, res.size());
    assertEquals(3, res.get("oid1").size());
    assertTrue(res.get("oid1").contains("ryhmaoid1"));
  }

  @Test
  public void testMissingRyhma() {
    String json =
        "{\"result\": {\"tulokset\": [{\"tulokset\": [{\"oid\": \"oid1\"}, {\"oid\": \"oid2\"}]}]}}";

    ResultSearch a =
        TarjontaAsyncResourceImpl.getGson()
            .fromJson(json, new TypeToken<ResultSearch>() {}.getType());
    Map<String, List<String>> res = TarjontaAsyncResourceImpl.resultSearchToHakukohdeRyhmaMap(a);
    assertEquals(2, res.size());
    assertEquals(0, res.get("oid1").size());
    assertEquals(0, res.get("oid2").size());
  }

  @Test
  public void testError() {
    String json =
        "{\"accessRights\":{},\"result\":{\"tulokset\":[],\"tuloksia\":0},\"status\":\"OK\"}";

    ResultSearch a =
        TarjontaAsyncResourceImpl.getGson()
            .fromJson(json, new TypeToken<ResultSearch>() {}.getType());
    Map<String, List<String>> res = TarjontaAsyncResourceImpl.resultSearchToHakukohdeRyhmaMap(a);
    assertEquals(0, res.size());
  }

  @Test
  public void doesNotBreakFromNullResponseWhenFetchingHaku() {
    when(tarjontaClient.getJson(eq(TARJONTA_HAKU_URL), any(Duration.class), any()))
        .thenReturn(CompletableFuture.completedFuture(new ResultV1RDTO<HakuV1RDTO>(null)));
    CompletableFuture<Haku> response = tarjontaAsyncResource.haeHaku("1.2.3.4");
    assertNotNull(response);
  }
}
