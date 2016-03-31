package fi.vm.sade.valinta.kooste.hakemukset;

import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import org.junit.Before;
import org.junit.Test;

public class HakemuksetResourceTest {

    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource hakemuksetValinnanvaiheResource = new HttpResource(root + "/hakemukset/valinnanvaihe");

    @Before
    public void startServer() {
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void testHaeHakemukset() throws Exception {
        /*
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
        */
    }

}
