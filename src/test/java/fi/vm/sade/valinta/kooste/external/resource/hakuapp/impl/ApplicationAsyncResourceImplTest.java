package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.sharedutils.http.HttpResource;
import fi.vm.sade.valinta.kooste.cas.CasKoosteInterceptor;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import io.reactivex.Observable;

import javax.ws.rs.client.Entity;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyVararg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ApplicationAsyncResourceImplTest {
    private final HttpResource wrappedHttpResource = mock(HttpResource.class);
    private final UrlConfiguration mockUrlConfiguration = mock(UrlConfiguration.class);
    private final Hakemus hakemus1 = new Hakemus(null, null, null, null, null, "hakemus1", "ACTIVE", "person1");
    private final Hakemus hakemus2 = new Hakemus(null, null, null, null, null, "hakemus2", "ACTIVE", "person2");
    private final Hakemus hakemus3 = new Hakemus(null, null, null, null, null, "hakemus3", "INCOMPLETE", "person3");
    private final String urlToApplicationsList = "/url/to/applications/list";
    private final String urlToApplicationsListFull = "/url/to/applications/list/full";
    private final HttpClient mockClient = mock(HttpClient.class);
    private ApplicationAsyncResourceImpl applicationAsyncResource = new ApplicationAsyncResourceImpl(mockClient, mock(CasKoosteInterceptor.class));

    @Before
    public void setMockInsideResourceUnderTest() {
        ReflectionTestUtils.setField(applicationAsyncResource, "wrappedHttpResource", wrappedHttpResource);
        ReflectionTestUtils.setField(applicationAsyncResource, "urlConfiguration", mockUrlConfiguration);
    }

    @Test
    public void stateParameterIsNotAddedWhenFetchingApplicationsByHakemusOidsWithoutKeysList() {
        ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        when(mockUrlConfiguration.url(
                eq("haku-app.applications.list"),
                queryCaptor.capture()
        )).thenReturn(urlToApplicationsList);
        ArrayList<String> hakemusOids = Lists.newArrayList("hakemus1Oid", "hakemus2Oid");
        when(mockClient.postJson(
                eq(urlToApplicationsList),
                any(Duration.class),
                eq(hakemusOids),
                any(),
                any()
        )).thenReturn(CompletableFuture.completedFuture(Lists.newArrayList(hakemus1, hakemus2)));
        List<HakemusWrapper> applications = applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids).timeout(1, SECONDS).blockingFirst();

        assertNull(queryCaptor.getValue().get("state"));
        assertEquals(1, queryCaptor.getValue().size());
        assertEquals(ApplicationAsyncResource.DEFAULT_ROW_LIMIT, queryCaptor.getValue().get("rows"));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus1), applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus2), applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
    }

    @Test
    public void passiveApplicationsAreFilteredOutWhenFetchingApplicationsByHakemusOidsWithoutKeysList() {
        hakemus2.setState("PASSIVE");

        ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        when(mockUrlConfiguration.url(
                eq("haku-app.applications.list"),
                queryCaptor.capture()
        )).thenReturn(urlToApplicationsList);
        ArrayList<String> hakemusOids = Lists.newArrayList("hakemus1Oid", "hakemus2Oid", "hakemus3Oid");
        when(mockClient.postJson(
                eq(urlToApplicationsList),
                any(Duration.class),
                eq(hakemusOids),
                any(),
                any()
        )).thenReturn(CompletableFuture.completedFuture(Lists.newArrayList(hakemus1, hakemus2, hakemus3)));
        List<HakemusWrapper> applications = applicationAsyncResource.getApplicationsByHakemusOids(
                Arrays.asList("hakemus1Oid", "hakemus2Oid", "hakemus3Oid")).timeout(1, SECONDS).blockingFirst();

        assertEquals(ApplicationAsyncResource.DEFAULT_ROW_LIMIT, queryCaptor.getValue().get("rows"));
        assertEquals(2, applications.size());
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus1), applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus3), applications.stream().filter(h -> h.getOid().equals("hakemus3")).findFirst().get()));
    }

    @Test
    public void stateParameterIsAddedWhenFetchingApplicationsByHakemusOidsWithKeysList() throws InterruptedException, ExecutionException, TimeoutException {
        hakemus2.setState("ACTIVE");

        ArgumentCaptor<Map<String, Object>> queryCaptor = ArgumentCaptor.forClass(Map.class);
        when(mockUrlConfiguration.url(
                eq("haku-app.applications.list"),
                queryCaptor.capture()
        )).thenReturn(urlToApplicationsList);
        ArrayList<String> hakemusOids = Lists.newArrayList("hakemus1Oid", "hakemus2Oid");
        when(mockClient.postJson(
                eq(urlToApplicationsList),
                any(Duration.class),
                eq(hakemusOids),
                any(),
                any()
        )).thenReturn(CompletableFuture.completedFuture(Lists.newArrayList(hakemus1, hakemus2)));
        List<String> keys = Collections.singletonList("hakemusOid");
        List<HakemusWrapper> applications = applicationAsyncResource.getApplicationsByhakemusOidsInParts("hakuOid",
                Arrays.asList("hakemus1Oid", "hakemus2Oid"),
                keys).get(1, SECONDS);

        assertEquals(keys, queryCaptor.getValue().get("keys"));
        assertEquals("hakuOid", queryCaptor.getValue().get("asIds"));
        assertEquals(ApplicationAsyncResource.DEFAULT_ROW_LIMIT, queryCaptor.getValue().get("rows"));
        assertEquals(ApplicationAsyncResource.DEFAULT_STATES, queryCaptor.getValue().get("state"));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus1), applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus2), applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
    }

    @Test
    public void appStateParameterIsAddedWhenFetchingApplicationsByHakemusAndHakukohdeOids() {
        String hakuOid = "1.2.3.4.5.6";
        String hakukohdeOid = "7.8.9.0";
        when(mockUrlConfiguration.url(
                eq("haku-app.applications.listfull")
        )).thenReturn(urlToApplicationsListFull);
        WebClient webClient = mock(WebClient.class);
        when(webClient.query(any(), any())).thenReturn(webClient);
        when(wrappedHttpResource.getAsObservableLazily(eq(urlToApplicationsListFull),
            eq(new TypeToken<List<Hakemus>>() {}.getType()),
            Mockito.any(Function.class)))
            .thenAnswer((Answer<Observable<List<Hakemus>>>) invocation -> {
                Function<WebClient,WebClient> webClientModifier = invocation.getArgument(2);
                webClientModifier.apply(webClient);
                verify(webClient).query("appState", ApplicationAsyncResource.DEFAULT_STATES.get(0), ApplicationAsyncResource.DEFAULT_STATES.get(1));
                verify(webClient).query("rows", ApplicationAsyncResource.DEFAULT_ROW_LIMIT);
                verify(webClient).query("asId", hakuOid);
                verify(webClient).query(eq("aoOid"), anyVararg());
                verify(webClient).getCurrentURI();
                Mockito.verifyNoMoreInteractions(webClient);
                return Observable.just(Arrays.asList(hakemus1, hakemus2));
            });
        List<HakemusWrapper> applications = applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid).timeout(1, SECONDS).blockingFirst();

        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus1), applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus2), applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
    }
}
