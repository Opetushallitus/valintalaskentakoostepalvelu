package fi.vm.sade.valinta.kooste.external.resource.hakuapp.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.HttpResource;
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
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import rx.Observable;

import javax.ws.rs.client.Entity;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class ApplicationAsyncResourceImplTest {
    private final HttpResource wrappedHttpResource = mock(HttpResource.class);
    private final UrlConfiguration urlConfiguration = new UrlConfiguration() {
        @Override
        public String url(String key, Object... params) {
            if ("haku-app.applications.list".equals(key)) {
                return urlToApplicationsList;
            }
            if ("haku-app.applications.listfull".equals(key)) {
                return urlToApplicationsListFull;
            }
            else throw new IllegalArgumentException("Don't know URL with key '" + key + "'");
        }
    };
    private final Hakemus hakemus1 = new Hakemus(null, null, null, null, null, "hakemus1", "ACTIVE", "person1");
    private final Hakemus hakemus2 = new Hakemus(null, null, null, null, null, "hakemus2", "ACTIVE", "person2");
    private final Hakemus hakemus3 = new Hakemus(null, null, null, null, null, "hakemus3", "INCOMPLETE", "person3");
    private final String urlToApplicationsList = "/url/to/applications/list";
    private final String urlToApplicationsListFull = "/url/to/applications/list/full";
    private ApplicationAsyncResourceImpl applicationAsyncResource = new ApplicationAsyncResourceImpl(mock(CasKoosteInterceptor.class));

    @Before
    public void setMockInsideResourceUnderTest() {
        ReflectionTestUtils.setField(applicationAsyncResource, "wrappedHttpResource", wrappedHttpResource);
        ReflectionTestUtils.setField(applicationAsyncResource, "urlConfiguration", urlConfiguration);
    }

    @Test
    public void stateParameterIsNotAddedWhenFetchingApplicationsByHakemusOidsWithoutKeysList() {
        when(wrappedHttpResource.postAsObservableLazily(eq(urlToApplicationsList),
            eq(new TypeToken<List<Hakemus>>() {}.getType()),
            eq(Entity.entity(Lists.newArrayList("hakemus1Oid", "hakemus2Oid"), APPLICATION_JSON_TYPE)),
            Mockito.any()))
            .thenAnswer((Answer<Observable<List<Hakemus>>>) invocation -> {
                Function<WebClient,WebClient> webClientModifier = invocation.getArgumentAt(3, Function.class);
                WebClient webClient = mock(WebClient.class);
                webClientModifier.apply(webClient);
                verify(webClient).accept(APPLICATION_JSON_TYPE);
                verify(webClient).query("rows", ApplicationAsyncResource.DEFAULT_ROW_LIMIT);
                Mockito.verifyNoMoreInteractions(webClient);
                return Observable.just(Arrays.asList(hakemus1, hakemus2));
            });
        List<HakemusWrapper> applications = applicationAsyncResource.getApplicationsByHakemusOids(Arrays.asList("hakemus1Oid", "hakemus2Oid")).timeout(1, SECONDS).toBlocking().first();

        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus1), applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus2), applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
    }

    @Test
    public void passiveApplicationsAreFilteredOutWhenFetchingApplicationsByHakemusOidsWithoutKeysList() {
        hakemus2.setState("PASSIVE");
        when(wrappedHttpResource.postAsObservableLazily(eq(urlToApplicationsList),
            eq(new TypeToken<List<Hakemus>>() {}.getType()),
            eq(Entity.entity(Lists.newArrayList("hakemus1Oid", "hakemus2Oid", "hakemus3Oid"), APPLICATION_JSON_TYPE)),
            Mockito.any()))
            .thenAnswer((Answer<Observable<List<Hakemus>>>) invocation -> {
                Function<WebClient,WebClient> webClientModifier = invocation.getArgumentAt(3, Function.class);
                WebClient webClient = mock(WebClient.class);
                webClientModifier.apply(webClient);
                verify(webClient).accept(APPLICATION_JSON_TYPE);
                verify(webClient).query("rows", ApplicationAsyncResource.DEFAULT_ROW_LIMIT);
                Mockito.verifyNoMoreInteractions(webClient);
                return Observable.just(Arrays.asList(hakemus1, hakemus2, hakemus3));
            });
        List<HakemusWrapper> applications = applicationAsyncResource.getApplicationsByHakemusOids(
                Arrays.asList("hakemus1Oid", "hakemus2Oid", "hakemus3Oid")).timeout(1, SECONDS).toBlocking().first();

        assertEquals(2, applications.size());
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus1), applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus3), applications.stream().filter(h -> h.getOid().equals("hakemus3")).findFirst().get()));
    }

    @Test
    public void stateParameterIsAddedWhenFetchingApplicationsByHakemusOidsWithKeysList() {
        hakemus2.setState("ACTIVE");
        when(wrappedHttpResource.postAsObservableLazily(eq(urlToApplicationsList),
            eq(new TypeToken<List<Hakemus>>() {}.getType()),
            eq(Entity.entity(Lists.newArrayList("hakemus1Oid", "hakemus2Oid"), APPLICATION_JSON_TYPE)),
            Mockito.any()))
            .thenAnswer((Answer<Observable<List<Hakemus>>>) invocation -> {
                Function<WebClient,WebClient> webClientModifier = invocation.getArgumentAt(3, Function.class);
                WebClient webClient = mock(WebClient.class);
                webClientModifier.apply(webClient);
                verify(webClient).accept(APPLICATION_JSON_TYPE);
                verify(webClient).query("keys", "hakemusOid");
                verify(webClient).query("asIds", "hakuOid");
                verify(webClient).query("rows", ApplicationAsyncResource.DEFAULT_ROW_LIMIT);
                verify(webClient).query("state", ApplicationAsyncResource.DEFAULT_STATES.get(0), ApplicationAsyncResource.DEFAULT_STATES.get(1));
                Mockito.verifyNoMoreInteractions(webClient);
                return Observable.just(Arrays.asList(hakemus1, hakemus2));
            });
        List<HakemusWrapper> applications = applicationAsyncResource.getApplicationsByhakemusOidsInParts("hakuOid",
                Arrays.asList("hakemus1Oid", "hakemus2Oid"),
                Collections.singletonList("hakemusOid")).timeout(1, SECONDS).toBlocking().first();

        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus1), applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus2), applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
    }

    @Test
    public void appStateParameterIsAddedWhenFetchingApplicationsByHakemusAndHakukohdeOids() {
        String hakuOid = "1.2.3.4.5.6";
        String hakukohdeOid = "7.8.9.0";
        WebClient webClient = mock(WebClient.class);
        when(webClient.query(any(), any())).thenReturn(webClient);
        when(wrappedHttpResource.getAsObservableLazily(eq(urlToApplicationsListFull),
            eq(new TypeToken<List<Hakemus>>() {}.getType()),
            Mockito.any(Function.class)))
            .thenAnswer((Answer<Observable<List<Hakemus>>>) invocation -> {
                Function<WebClient,WebClient> webClientModifier = invocation.getArgumentAt(2, Function.class);
                webClientModifier.apply(webClient);
                verify(webClient).query("appState", ApplicationAsyncResource.DEFAULT_STATES.get(0), ApplicationAsyncResource.DEFAULT_STATES.get(1));
                verify(webClient).query("rows", ApplicationAsyncResource.DEFAULT_ROW_LIMIT);
                verify(webClient).query("asId", hakuOid);
                verify(webClient).query(eq("aoOid"), anyVararg());
                verify(webClient).getCurrentURI();
                Mockito.verifyNoMoreInteractions(webClient);
                return Observable.just(Arrays.asList(hakemus1, hakemus2));
            });
        List<HakemusWrapper> applications = applicationAsyncResource.getApplicationsByOid(hakuOid, hakukohdeOid).timeout(1, SECONDS).toBlocking().first();

        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus1), applications.stream().filter(h -> h.getOid().equals("hakemus1")).findFirst().get()));
        assertTrue(EqualsBuilder.reflectionEquals(new HakuappHakemusWrapper(hakemus2), applications.stream().filter(h -> h.getOid().equals("hakemus2")).findFirst().get()));
    }
}
