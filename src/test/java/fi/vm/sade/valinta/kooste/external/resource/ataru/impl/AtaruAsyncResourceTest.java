package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.KansalaisuusDto;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.mocks.MockAtaruAsyncResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

public class AtaruAsyncResourceTest {
  private final String hakemusOid1 = "1.2.246.562.11.00000000000000000063";
  private final String hakemusOid2 = "1.2.246.562.11.00000000000000000064";
  private final String hakemusOid3 = "1.2.246.562.11.00000000000000000065";

  private final RestCasClient casClient = mock(RestCasClient.class);
  private final OppijanumerorekisteriAsyncResource mockOnr =
      mock(OppijanumerorekisteriAsyncResource.class);
  private final KoodistoCachedAsyncResource mockKoodisto = mock(KoodistoCachedAsyncResource.class);
  private final AtaruAsyncResourceImpl ataruAsyncResource =
      new AtaruAsyncResourceImpl(casClient, mockOnr, mockKoodisto);
  private final String applicationsUrl = "/url/to/applications";

  private final UrlConfiguration urlConfiguration =
      new UrlConfiguration() {
        @Override
        public String url(String key, Object... params) {
          return applicationsUrl;
        }
      };

  @BeforeEach
  public void setMockInsideResourceUnderTest() {
    ReflectionTestUtils.setField(ataruAsyncResource, "urlConfiguration", urlConfiguration);
  }

  @Test
  public void getAtaruApplicationsWithPersonAndISOCountryOfResidence()
      throws InterruptedException, ExecutionException, TimeoutException {
    Koodi suomiKoodi = new Koodi();
    suomiKoodi.setKoodiArvo("FIN");
    Koodi saintMartinKoodi = new Koodi();
    saintMartinKoodi.setKoodiArvo("MAF");

    HenkiloPerustietoDto onrHenkilo = new HenkiloPerustietoDto();
    onrHenkilo.setOidHenkilo("1.2.246.562.24.86368188549");
    onrHenkilo.setEtunimet("Feliks Esaias");
    onrHenkilo.setSukunimi("Pakarinen");

    HenkiloPerustietoDto onrHenkilo2 = new HenkiloPerustietoDto();
    onrHenkilo2.setOidHenkilo("1.2.246.562.24.86368188550");
    onrHenkilo2.setEtunimet("Frank Esaias");
    onrHenkilo2.setSukunimi("Pakarinen");

    HenkiloPerustietoDto onrHenkilo3 = new HenkiloPerustietoDto();
    onrHenkilo3.setOidHenkilo("1.2.246.562.24.86368188551");
    onrHenkilo3.setEtunimet("Pekka Esaias");
    onrHenkilo3.setSukunimi("Pakarinen");

    KansalaisuusDto kansalaisuus = new KansalaisuusDto();
    kansalaisuus.setKansalaisuusKoodi("246");
    onrHenkilo.setKansalaisuus(Sets.newHashSet(kansalaisuus));
    onrHenkilo2.setKansalaisuus(Sets.newHashSet(kansalaisuus));
    onrHenkilo3.setKansalaisuus(Sets.newHashSet(kansalaisuus));

    when(casClient.post(
            eq(applicationsUrl),
            eq(new TypeToken<List<AtaruHakemus>>() {}),
            eq(Lists.newArrayList(hakemusOid1, hakemusOid2, hakemusOid3)),
            anyMap(),
            any(Integer.class)))
        .thenReturn(
            CompletableFuture.completedFuture(MockAtaruAsyncResource.getAtaruHakemukset(null)));

    when(mockKoodisto.maatjavaltiot2ToMaatjavaltiot1(eq("maatjavaltiot2_246")))
        .thenReturn(CompletableFuture.completedFuture(suomiKoodi));
    when(mockKoodisto.maatjavaltiot2ToMaatjavaltiot1(eq("maatjavaltiot2_663")))
        .thenReturn(CompletableFuture.completedFuture(saintMartinKoodi));

    Map<String, HenkiloPerustietoDto> henkiloResponse = new HashMap<>();
    henkiloResponse.put("1.2.246.562.24.86368188549", onrHenkilo);
    henkiloResponse.put("1.2.246.562.24.86368188550", onrHenkilo2);
    henkiloResponse.put("1.2.246.562.24.86368188551", onrHenkilo3);
    when(mockOnr.haeHenkilot(
            eq(
                Lists.newArrayList(
                    "1.2.246.562.24.86368188549",
                    "1.2.246.562.24.86368188550",
                    "1.2.246.562.24.86368188551"))))
        .thenReturn(CompletableFuture.completedFuture(henkiloResponse));

    List<HakemusWrapper> applications =
        ataruAsyncResource
            .getApplicationsByOids(Lists.newArrayList(hakemusOid1, hakemusOid2, hakemusOid3))
            .get(1, SECONDS);
    assertEquals(3, applications.size());
    assertEquals("FIN", applications.get(0).getAsuinmaa());
    assertEquals("MAF", applications.get(1).getAsuinmaa());
    assertEquals("Pekka", applications.get(2).getEtunimi());
    assertEquals("FIN", applications.get(0).getKansalaisuus());
  }
}
