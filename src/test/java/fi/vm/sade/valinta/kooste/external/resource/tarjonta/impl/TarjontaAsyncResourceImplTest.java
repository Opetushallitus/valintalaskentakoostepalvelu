package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TarjontaAsyncResourceImplTest {

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
