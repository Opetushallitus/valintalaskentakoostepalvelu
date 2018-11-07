package fi.vm.sade.valinta.kooste.hakemukset;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import fi.vm.sade.valinta.kooste.ValintaKoosteJetty;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.mocks.MockApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.MockValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.mocks.Mocks;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.HakutoiveDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class HakemuksetResourceTest {

    final String root = "http://localhost:" + ValintaKoosteJetty.port + "/valintalaskentakoostepalvelu/resources";
    final HttpResourceBuilder.WebClientExposingHttpResource hakemuksetValinnanvaiheResource = new HttpResourceBuilder()
            .address(root + "/hakemukset/valinnanvaihe")
            .buildExposingWebClientDangerously();

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
        MockValintaperusteetAsyncResource.setHakukohteetValinnanvaiheelleResult(Sets.newHashSet("1.2.246.562.5.28143628072"));
        ValintaperusteDTO v3 = new ValintaperusteDTO();
        v3.setTunniste("tunniste");
        HakukohdeJaValintaperusteDTO v4  = new HakukohdeJaValintaperusteDTO("1.2.246.562.5.28143628072", Lists.newArrayList(v3));
        MockValintaperusteetAsyncResource.setHakukohdeValintaperusteResult(Lists.newArrayList(v4));
        ValintaKoosteJetty.startShared();
    }

    @Test
    public void testHaeHakemukset() throws Exception {

        Mocks.reset();
        String listFull = IOUtils.toString(new ClassPathResource("listSingleApplication.json").getInputStream());
        List<Hakemus> hakemukset  = hakemuksetValinnanvaiheResource.gson().fromJson(listFull, new TypeToken<List<Hakemus>>() {}.getType());
        List<HakemusWrapper> wrappers = hakemukset.stream().map(HakuappHakemusWrapper::new).collect(Collectors.toList());
        MockApplicationAsyncResource.setResult(wrappers);
        MockApplicationAsyncResource.setResultByOid(wrappers);

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
        assertEquals(1, je.getAsJsonArray().size());
        assertEquals("tunniste", je.getAsJsonArray().get(0).getAsJsonObject()
                        .get("hakukohteet").getAsJsonArray().get(0).getAsJsonObject()
                         .get("valintakokeet").getAsJsonArray().get(0).getAsJsonObject()
                         .get("tunniste").getAsString());
    }

    @Test
    public void josHakijaryhmiaEiLoydyPalautetaanBadRequestSelityksenKanssa() throws IOException {
        Mocks.reset();
        MockValintaperusteetAsyncResource.setHakukohteetValinnanvaiheelleResult(Collections.emptySet());
        String hakuOid = "1.2.3";
        String valinnanvaiheOid = "4.5.6";
        Response r = hakemuksetValinnanvaiheResource.getWebClient()
                .query("hakuOid", hakuOid)
                .query("valinnanvaiheOid", valinnanvaiheOid)
                .get();

        assertEquals(HttpStatus.SC_BAD_REQUEST, r.getStatus());
        assertThat(IOUtils.toString((InputStream) r.getEntity(), "UTF-8"),
            containsString(String.format(
                "Ei löytynyt yhtään hakukohdeoidia valintaryhmien perusteella haun %s valinnanvaiheelle %s",
                hakuOid,
                valinnanvaiheOid)));
    }
}
