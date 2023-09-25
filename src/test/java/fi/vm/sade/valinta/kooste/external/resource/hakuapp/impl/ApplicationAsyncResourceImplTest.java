package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.viestintapalvelu.RestCasClient;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.sharedutils.http.HttpResource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

public class ApplicationAsyncResourceImplTest {
  private final HttpResource wrappedHttpResource = mock(HttpResource.class);
  private final UrlConfiguration mockUrlConfiguration = mock(UrlConfiguration.class);
  private final Hakemus hakemus1 =
      new Hakemus(null, null, null, null, null, "hakemus1", "ACTIVE", "person1");
  private final Hakemus hakemus2 =
      new Hakemus(null, null, null, null, null, "hakemus2", "ACTIVE", "person2");
  private final Hakemus hakemus3 =
      new Hakemus(null, null, null, null, null, "hakemus3", "INCOMPLETE", "person3");
  private final String urlToApplicationsList = "/url/to/applications/list";
  private final String urlToApplicationsListFull = "/url/to/applications/list/full";
  private final RestCasClient mockClient = mock(RestCasClient.class);
  private ApplicationAsyncResourceImpl applicationAsyncResource =
      new ApplicationAsyncResourceImpl(mockClient, mockUrlConfiguration);

  @Test
  public void stateParameterIsNotAddedWhenFetchingApplicationsByHakemusOidsWithoutKeysList() {
    ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
    when(mockUrlConfiguration.url(eq("haku-app.applications.list"), queryCaptor.capture()))
        .thenReturn(urlToApplicationsList);
    ArrayList<String> hakemusOids = Lists.newArrayList("hakemus1Oid", "hakemus2Oid");
    when(mockClient.post(eq(urlToApplicationsList), any(), eq(hakemusOids), anyMap(), anyInt()))
        .thenReturn(CompletableFuture.completedFuture(Lists.newArrayList(hakemus1, hakemus2)));

    List<HakemusWrapper> applications =
        applicationAsyncResource
            .getApplicationsByHakemusOids(hakemusOids)
            .timeout(1, SECONDS)
            .blockingFirst();

    assertNull(queryCaptor.getValue().get("state"));
    assertEquals(1, queryCaptor.getValue().size());
    assertEquals(ApplicationAsyncResource.DEFAULT_ROW_LIMIT, queryCaptor.getValue().get("rows"));
    assertTrue(
        EqualsBuilder.reflectionEquals(
            new HakuappHakemusWrapper(hakemus1),
            applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
    assertTrue(
        EqualsBuilder.reflectionEquals(
            new HakuappHakemusWrapper(hakemus2),
            applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
  }

  @Test
  public void
      passiveApplicationsAreFilteredOutWhenFetchingApplicationsByHakemusOidsWithoutKeysList() {
    hakemus2.setState("PASSIVE");

    ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
    when(mockUrlConfiguration.url(eq("haku-app.applications.list"), queryCaptor.capture()))
        .thenReturn(urlToApplicationsList);
    ArrayList<String> hakemusOids = Lists.newArrayList("hakemus1Oid", "hakemus2Oid", "hakemus3Oid");

    when(mockClient.post(eq(urlToApplicationsList), any(), eq(hakemusOids), anyMap(), anyInt()))
        .thenReturn(
            CompletableFuture.completedFuture(Lists.newArrayList(hakemus1, hakemus2, hakemus3)));

    List<HakemusWrapper> applications =
        applicationAsyncResource
            .getApplicationsByHakemusOids(
                Arrays.asList("hakemus1Oid", "hakemus2Oid", "hakemus3Oid"))
            .timeout(1, SECONDS)
            .blockingFirst();

    assertEquals(ApplicationAsyncResource.DEFAULT_ROW_LIMIT, queryCaptor.getValue().get("rows"));
    assertEquals(2, applications.size());
    assertTrue(
        EqualsBuilder.reflectionEquals(
            new HakuappHakemusWrapper(hakemus1),
            applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
    assertTrue(
        EqualsBuilder.reflectionEquals(
            new HakuappHakemusWrapper(hakemus3),
            applications.stream().filter(h -> h.getOid().equals("hakemus3")).findFirst().get()));
  }

  @Test
  public void stateParameterIsAddedWhenFetchingApplicationsByHakemusOidsWithKeysList()
      throws InterruptedException, ExecutionException, TimeoutException {
    hakemus2.setState("ACTIVE");

    ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
    when(mockUrlConfiguration.url(eq("haku-app.applications.list"), queryCaptor.capture()))
        .thenReturn(urlToApplicationsList);
    ArrayList<String> hakemusOids = Lists.newArrayList("hakemus1Oid", "hakemus2Oid");

    when(mockClient.post(eq(urlToApplicationsList), any(), eq(hakemusOids), anyMap(), anyInt()))
        .thenReturn(CompletableFuture.completedFuture(Lists.newArrayList(hakemus1, hakemus2)));

    List<String> keys = Collections.singletonList("hakemusOid");
    List<HakemusWrapper> applications =
        applicationAsyncResource
            .getApplicationsByhakemusOidsInParts(
                "hakuOid", Arrays.asList("hakemus1Oid", "hakemus2Oid"), keys)
            .get(1, SECONDS);

    assertEquals(keys, queryCaptor.getValue().get("keys"));
    assertEquals("hakuOid", queryCaptor.getValue().get("asIds"));
    assertEquals(ApplicationAsyncResource.DEFAULT_ROW_LIMIT, queryCaptor.getValue().get("rows"));
    assertEquals(ApplicationAsyncResource.DEFAULT_STATES, queryCaptor.getValue().get("state"));
    assertTrue(
        EqualsBuilder.reflectionEquals(
            new HakuappHakemusWrapper(hakemus1),
            applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
    assertTrue(
        EqualsBuilder.reflectionEquals(
            new HakuappHakemusWrapper(hakemus2),
            applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
  }

  @Test
  public void appStateParameterIsAddedWhenFetchingApplicationsByHakemusAndHakukohdeOids()
      throws InterruptedException, ExecutionException, TimeoutException {
    String hakuOid = "1.2.3.4.5.6";
    String hakukohdeOid = "7.8.9.0";
    String eventualUrl = "http://this.would.be.the.real.URL.with.parameters.example.com/?foo=bar";
    when(mockUrlConfiguration.url(eq("haku-app.applications.listfull"), any(Map.class)))
        .thenReturn(eventualUrl);

    when(mockClient.get(anyString(), any(), any(), anyInt()))
        .thenAnswer(
            (Answer<CompletableFuture<List<Hakemus>>>)
                invocation -> {
                  assertEquals(eventualUrl, invocation.getArgument(0));
                  return CompletableFuture.completedFuture(Arrays.asList(hakemus1, hakemus2));
                });

    List<HakemusWrapper> applications =
        applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid).get(1L, SECONDS);

    assertTrue(
        EqualsBuilder.reflectionEquals(
            new HakuappHakemusWrapper(hakemus1),
            applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
    assertTrue(
        EqualsBuilder.reflectionEquals(
            new HakuappHakemusWrapper(hakemus2),
            applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
  }
}
