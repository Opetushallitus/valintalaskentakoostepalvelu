package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.HakukohderyhmaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.KoutaHakukohde;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultTulos;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.*;
import org.springframework.test.util.ReflectionTestUtils;

public class TarjontaAsyncResourceImplTest {
  private static final String TARJONTA_URL =
      "http://test/tarjonta-service/rest/v1/hakukohde/search";
  private static final String KOUTA_URL = "http://test/kouta-internal/hakukohde/search";
  private static final String HKR_URL = "http://test/search/by-hakukohteet";

  private final HttpClient tarjontaClient = mock(HttpClient.class);
  private final HttpClient koutaClient = mock(HttpClient.class);
  private final HttpClient hakukohderyhmapalveluClient = mock(HttpClient.class);

  private final TarjontaAsyncResourceImpl tarjontaAsyncResource =
      new TarjontaAsyncResourceImpl(tarjontaClient, koutaClient, hakukohderyhmapalveluClient);

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

  public void setMocks() {
    when(tarjontaClient.getJson(
            eq(TARJONTA_URL), any(Duration.class), eq(new TypeToken<ResultSearch>() {}.getType())))
        .thenReturn(
            CompletableFuture.completedFuture(new ResultSearch(new ResultTulos(emptyList()))));

    KoutaHakukohde hk1 = new KoutaHakukohde("1.2.246.562.20.1");
    KoutaHakukohde hk2 = new KoutaHakukohde("1.2.246.562.20.2");

    when(koutaClient.getJson(
            eq(KOUTA_URL),
            any(Duration.class),
            eq(new TypeToken<Set<KoutaHakukohde>>() {}.getType())))
        .thenReturn(CompletableFuture.completedFuture(Set.of(hk1, hk2)));

    HakukohderyhmaHakukohde hkr1 =
        new HakukohderyhmaHakukohde(
            "1.2.246.562.20.1", Arrays.asList("1.2.246.562.28.1", "1.2.246.562.28.2"));

    HakukohderyhmaHakukohde hkr2 =
        new HakukohderyhmaHakukohde(
            "1.2.246.562.20.2", Arrays.asList("1.2.246.562.28.1", "1.2.246.562.28.3"));

    when(hakukohderyhmapalveluClient.postJson(
            eq(HKR_URL),
            any(Duration.class),
            or(
                eq(Arrays.asList("1.2.246.562.20.2", "1.2.246.562.20.1")),
                eq(Arrays.asList("1.2.246.562.20.1", "1.2.246.562.20.2"))),
            eq(new TypeToken<List<String>>() {}.getType()),
            eq(new TypeToken<List<HakukohderyhmaHakukohde>>() {}.getType())))
        .thenReturn(CompletableFuture.completedFuture(Arrays.asList(hkr1, hkr2)));
  }

  @Test
  public void testHakukohdeRyhmasForHakukohdes() {
    Map<String, List<String>> expected = new HashMap<>();
    expected.put("1.2.246.562.20.1", Arrays.asList("1.2.246.562.28.1", "1.2.246.562.28.2"));
    expected.put("1.2.246.562.20.2", Arrays.asList("1.2.246.562.28.1", "1.2.246.562.28.3"));

    CompletableFuture<Map<String, List<String>>> actual =
        tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes("1.2.246.562.10.1");

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
}
