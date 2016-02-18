package fi.vm.sade.valinta.kooste.hakemukset;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HakemuksetResourceTest {

    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource hakemuksetValinnanvaiheResource = new HttpResource(root + "/hakemukset/valinnanvaihe");

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void testHaeHakemukset() throws Exception {
        Mocks.reset();
        String listFull = IOUtils.toString(new ClassPathResource("listfull.json").getInputStream());
        List<Hakemus> hakemukset  = HttpResource.GSON.fromJson(listFull, new TypeToken<List<Hakemus>>() {}.getType());

        MockApplicationAsyncResource.setResult(hakemukset);
        MockApplicationAsyncResource.setResultByOid(hakemukset);

        Response r = hakemuksetValinnanvaiheResource.getWebClient()
                .query("hakuOid", "")
                .query("valinnanvaiheOid", "")
                .get();

        assertEquals(200, r.getStatus());

        String responseAsString = r.readEntity(String.class);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(responseAsString);
        String prettyJsonString = gson.toJson(je);

        System.out.println(prettyJsonString);
    }

}
