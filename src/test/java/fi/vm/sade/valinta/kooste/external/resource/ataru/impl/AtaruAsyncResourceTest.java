package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import fi.vm.sade.valinta.http.HttpResource;
import fi.vm.sade.valinta.kooste.cas.CasKoosteInterceptor;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.mocks.MockAtaruAsyncResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import rx.Observable;

import javax.ws.rs.client.Entity;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AtaruAsyncResourceTest {
    private final HttpResource wrappedHttpResource = mock(HttpResource.class);
    private final String hakemusOid1 = "1.2.246.562.11.00000000000000000063";
    private final String hakemusOid2 = "1.2.246.562.11.00000000000000000064";
    private final String hakemusOid3 = "1.2.246.562.11.00000000000000000065";

    private final OppijanumerorekisteriAsyncResource mockOnr = mock(OppijanumerorekisteriAsyncResource.class);
    private final KoodistoCachedAsyncResource mockKoodisto = mock(KoodistoCachedAsyncResource.class);
    private final AtaruAsyncResourceImpl ataruAsyncResource = new AtaruAsyncResourceImpl(mock(CasKoosteInterceptor.class), mockOnr, mockKoodisto);
    private final String applicationsUrl = "/url/to/applications";

    private final UrlConfiguration urlConfiguration = new UrlConfiguration() {
        @Override
        public String url(String key, Object... params) {
            return applicationsUrl;
        }
    };

    @Before
    public void setMockInsideResourceUnderTest() {
        ReflectionTestUtils.setField(ataruAsyncResource, "wrappedHttpResource", wrappedHttpResource);
        ReflectionTestUtils.setField(ataruAsyncResource, "urlConfiguration", urlConfiguration);
    }

    @Test
    public void getAtaruApplicationsWithPersonAndISOCountryOfResidence() {
        Koodi suomiKoodi = new Koodi();
        suomiKoodi.setKoodiArvo("FIN");
        Koodi saintMartinKoodi = new Koodi();
        saintMartinKoodi.setKoodiArvo("MAF");

        HenkiloPerustietoDto onrHenkilo = new HenkiloPerustietoDto();
        onrHenkilo.setOidHenkilo("1.2.246.562.24.86368188549");
        onrHenkilo.setEtunimet("Feliks Esaias");
        onrHenkilo.setSukunimi("Pakarinen");

        when(wrappedHttpResource.gson()).thenReturn(HttpResource.DEFAULT_GSON);

        when(wrappedHttpResource.postAsObservableLazily(
                eq(applicationsUrl),
                eq(new TypeToken<List<AtaruHakemus>>() {}.getType()),
                eq(Entity.entity(HttpResource.DEFAULT_GSON.toJson(Lists.newArrayList(hakemusOid1, hakemusOid2, hakemusOid3)), APPLICATION_JSON)),
                any()))
                .thenAnswer((Answer<Observable<List<AtaruHakemus>>>) invocation -> Observable.just(MockAtaruAsyncResource.getAtaruHakemukset(null)));

        when(mockKoodisto.haeRinnasteinenKoodiAsync(eq("maatjavaltiot2_246"))).thenReturn(Observable.just(suomiKoodi));
        when(mockKoodisto.haeRinnasteinenKoodiAsync(eq("maatjavaltiot2_663"))).thenReturn(Observable.just(saintMartinKoodi));

        when(mockOnr.haeHenkilot(Collections.singletonList("1.2.246.562.24.86368188549"))).thenReturn(Observable.just(Collections.singletonList(onrHenkilo)));

        List<HakemusWrapper> applications = ataruAsyncResource.getApplicationsByOids(Lists.newArrayList(hakemusOid1, hakemusOid2, hakemusOid3))
                .timeout(1, SECONDS).toBlocking().first();
        assertEquals(3, applications.size());
        assertEquals("FIN", applications.get(0).getAsuinmaa());
        assertEquals("MAF", applications.get(1).getAsuinmaa());
        assertEquals("Feliks", applications.get(2).getEtunimi());
    }
}
