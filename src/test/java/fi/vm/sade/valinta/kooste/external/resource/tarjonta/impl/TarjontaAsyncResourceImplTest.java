package fi.vm.sade.valinta.kooste.external.resource.tarjonta.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import com.google.common.reflect.TypeToken;

import fi.vm.sade.valinta.kooste.external.resource.tarjonta.dto.ResultSearch;
import org.junit.Test;
import io.reactivex.Observable;

import java.util.List;
import java.util.Map;

public class TarjontaAsyncResourceImplTest {

    @Test
    public void testHakukohdeRyhmasForHakukohdesParsesCorrectly(){
        String json = "{\"result\": {\"tulokset\": [{\"tulokset\": [{\"oid\": \"oid1\",\"ryhmaliitokset\": [{\"ryhmaOid\": \"ryhmaoid1\"},{\"ryhmaOid\": \"ryhmaoid2\"},{\"ryhmaOid\": \"ryhmaoid3\"}]},{\"oid\": \"oid2\",\"ryhmaliitokset\": []}]}],\"tuloksia\": 1}}";

        Observable<ResultSearch> a = Observable.just(TarjontaAsyncResourceImpl.getGson().fromJson(json, new TypeToken<ResultSearch>() {}.getType()));
        Observable<Map<String, List<String>>> observable = TarjontaAsyncResourceImpl.resultSearchToHakukohdeRyhmaMap(a);
        Map<String, List<String>> res = observable.timeout(10, SECONDS).blockingSingle();
        assertEquals(2, res.size());
        assertEquals(3, res.get("oid1").size());
        assertTrue(res.get("oid1").contains("ryhmaoid1"));
    }

    @Test
    public void testMissingRyhma(){
        String json = "{\"result\": {\"tulokset\": [{\"tulokset\": [{\"oid\": \"oid1\"}, {\"oid\": \"oid2\"}]}]}}";

        Observable<ResultSearch> a = Observable.just(TarjontaAsyncResourceImpl.getGson().fromJson(json, new TypeToken<ResultSearch>() {}.getType()));
        Observable<Map<String, List<String>>> observable = TarjontaAsyncResourceImpl.resultSearchToHakukohdeRyhmaMap(a);
        Map<String, List<String>> res = observable.timeout(10, SECONDS).blockingSingle();
        assertEquals(2, res.size());
        assertEquals(0, res.get("oid1").size());
        assertEquals(0, res.get("oid2").size());
    }

    @Test
    public void testError(){
        String json = "{\"accessRights\":{},\"result\":{\"tulokset\":[],\"tuloksia\":0},\"status\":\"OK\"}";

        Observable<ResultSearch> a = Observable.just(TarjontaAsyncResourceImpl.getGson().fromJson(json, new TypeToken<ResultSearch>() {}.getType()));
        Observable<Map<String, List<String>>> observable = TarjontaAsyncResourceImpl.resultSearchToHakukohdeRyhmaMap(a);
        Map<String, List<String>> res = observable.timeout(10, SECONDS).blockingSingle();
        assertEquals(0, res.size());
    }



}
