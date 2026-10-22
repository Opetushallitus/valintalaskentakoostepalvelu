package fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus;

import static fi.vm.sade.valinta.kooste.Integraatiopalvelimet.mockToReturnJsonAndCheckBody;
import static jakarta.ws.rs.HttpMethod.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.Integraatiopalvelimet;
import fi.vm.sade.valinta.kooste.MockOpintopolkuCasAuthenticationFilter;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakemuksenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.external.resource.harkinnanvaraisuus.dto.HakutoiveenHarkinnanvaraisuus;
import fi.vm.sade.valinta.kooste.testapp.MockServicesApp;
import fi.vm.sade.valinta.sharedutils.http.HttpResourceBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

public class HarkinnanvaraisuusResourceTest {
  private static final String HAKEMUS_OID = "1.2.246.562.11.00000000001";
  private static final String HENKILO_OID = "1.2.246.562.24.00000000001";
  private static final String HAKUKOHDE_OID = "1.2.246.562.20.00000000001";
  private static final String SUORITUSPALVELU_PATH =
      "/suorituspalvelu/api/v1/valinnat/harkinnanvaraisuus";

  @BeforeEach
  public void init() {
    MockServicesApp.start();
    MockOpintopolkuCasAuthenticationFilter.setRolesToReturnInFakeAuthentication(
        "ROLE_APP_HAKEMUS_READ_UPDATE");
  }

  @AfterEach
  public void reset() {
    Integraatiopalvelimet.mockServer.reset();
  }

  @Test
  public void hakemuksilleValittaaSuorituspalvelunHarkinnanvaraisuustiedot() throws Exception {
    HakemuksenHarkinnanvaraisuus supanTulos =
        new HakemuksenHarkinnanvaraisuus(
            HAKEMUS_OID,
            List.of(
                new HakutoiveenHarkinnanvaraisuus(
                    HAKUKOHDE_OID, HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET)));
    supanTulos.setHenkiloOid(HENKILO_OID);
    mockToReturnJsonAndCheckBody(
        POST,
        SUORITUSPALVELU_PATH,
        List.of(supanTulos),
        "\\{\"hakemusOids\":\\[\"" + HAKEMUS_OID.replace(".", "\\.") + "\"\\]\\}");

    Response response = hakemuksille(List.of(HAKEMUS_OID));

    assertEquals(200, response.getStatus());
    List<HakemuksenHarkinnanvaraisuus> tulos =
        new Gson()
            .fromJson(
                IOUtils.toString((InputStream) response.getEntity(), "UTF-8"),
                new TypeToken<List<HakemuksenHarkinnanvaraisuus>>() {}.getType());
    assertEquals(1, tulos.size());
    assertEquals(HAKEMUS_OID, tulos.get(0).getHakemusOid());
    assertEquals(HENKILO_OID, tulos.get(0).getHenkiloOid());
    assertEquals(1, tulos.get(0).getHakutoiveet().size());
    assertEquals(HAKUKOHDE_OID, tulos.get(0).getHakutoiveet().get(0).getHakukohdeOid());
    assertEquals(
        HarkinnanvaraisuudenSyy.ATARU_OPPIMISVAIKEUDET,
        tulos.get(0).getHakutoiveet().get(0).getHarkinnanvaraisuudenSyy());
  }

  @Test
  public void hakemuksilleEiKutsuSuorituspalveluaTyhjallaListalla() throws Exception {
    Response response = hakemuksille(List.of());

    assertEquals(200, response.getStatus());
    assertEquals("[]", IOUtils.toString((InputStream) response.getEntity(), "UTF-8"));
    assertEquals(0, suorituspalveluunTehdytKutsut());
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 403, 500})
  public void hakemuksillePalauttaaVirheenKunSuorituspalvelunKutsuEpaonnistuu(int statusKoodi) {
    Integraatiopalvelimet.mockServer
        .when(HttpRequest.request().withMethod(POST).withPath(SUORITUSPALVELU_PATH))
        .respond(
            HttpResponse.response()
                .withStatusCode(statusKoodi)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"virheet\":[\"Harkinnanvaraisuustietojen haku epäonnistui\"]}"));

    Response response = hakemuksille(List.of(HAKEMUS_OID));

    assertEquals(500, response.getStatus());
    assertEquals(1, suorituspalveluunTehdytKutsut());
  }

  private int suorituspalveluunTehdytKutsut() {
    return Integraatiopalvelimet.mockServer.retrieveRecordedRequests(
            HttpRequest.request().withPath(SUORITUSPALVELU_PATH))
        .length;
  }

  private Response hakemuksille(List<String> hakemusOids) {
    return new HttpResourceBuilder(getClass().getName())
        .address(MockServicesApp.resourcesAddress + "/harkinnanvaraisuus/hakemuksille")
        .buildExposingWebClientDangerously()
        .getWebClient()
        .post(Entity.json(hakemusOids));
  }
}
