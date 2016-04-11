package fi.vm.sade.valinta.kooste.hakemukset;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class HakemuksetResourceTest {

    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResource hakemuksetValinnanvaiheResource = new HttpResource(root + "/hakemukset/valinnanvaihe");

    @Before
    public void startServer() {
        ValintakoeDTO v1 = new ValintakoeDTO();
        v1.setOid("1.2.3.4");
        v1.setSelvitettyTunniste("testikoe");
        v1.setKutsutaankoKaikki(false);
        ValintakoeDTO v2 = new ValintakoeDTO();
        v2.setOid("1.2.3.5");
        v2.setSelvitettyTunniste("testikoeKaikkiKutsutaan");
        v2.setKutsutaankoKaikki(true);
        MockValintaperusteetAsyncResource.setHakukohdeResult(
                Arrays.asList(new HakukohdeJaValintakoeDTO("1.2.246.562.5.28143628072", Arrays.asList(v1, v2))));
        MockValintalaskentaValintakoeAsyncResource.setResult(
                Arrays.asList(new ValintakoeOsallistuminenDTO(){{
                    setHakutoiveet(Arrays.asList(new HakutoiveDTO() {{
                        setHakukohdeOid("1.2.246.562.5.28143628072");
                    }}));
                    setHakemusOid("1.2.246.562.11.00000015082");
                }})
        );
        ValintaKoosteJetty.startShared();
    }

    @Ignore // AuthorityCheckService needs to be mocked in HakemuksetResourceTest
    @Test
    public void testHaeHakemukset() throws Exception {

        Mocks.reset();
        String listFull = IOUtils.toString(new ClassPathResource("listSingleApplication.json").getInputStream());
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
