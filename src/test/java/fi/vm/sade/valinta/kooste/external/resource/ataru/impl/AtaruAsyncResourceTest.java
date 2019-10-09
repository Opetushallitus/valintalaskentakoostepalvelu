package fi.vm.sade.valinta.kooste.external.resource.ataru.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.reflect.TypeToken;
import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.dto.Koodi;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.dto.KansalaisuusDto;
import fi.vm.sade.valinta.kooste.mocks.MockAtaruAsyncResource;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AtaruAsyncResourceTest {
    private final String hakemusOid1 = "1.2.246.562.11.00000000000000000063";
    private final String hakemusOid2 = "1.2.246.562.11.00000000000000000064";
    private final String hakemusOid3 = "1.2.246.562.11.00000000000000000065";

    private final HttpClient httpClient = mock(HttpClient.class);
    private final OppijanumerorekisteriAsyncResource mockOnr = mock(OppijanumerorekisteriAsyncResource.class);
    private final KoodistoCachedAsyncResource mockKoodisto = mock(KoodistoCachedAsyncResource.class);
    private final AtaruAsyncResourceImpl ataruAsyncResource = new AtaruAsyncResourceImpl(httpClient, mockOnr, mockKoodisto);
    private final String applicationsUrl = "/url/to/applications";

    private final UrlConfiguration urlConfiguration = new UrlConfiguration() {
        @Override
        public String url(String key, Object... params) {
            return applicationsUrl;
        }
    };

    @Before
    public void setMockInsideResourceUnderTest() {
        ReflectionTestUtils.setField(ataruAsyncResource, "urlConfiguration", urlConfiguration);
    }

    @Test
    public void getAtaruApplicationsWithPersonAndISOCountryOfResidence() throws InterruptedException, ExecutionException, TimeoutException {
        Koodi suomiKoodi = new Koodi();
        suomiKoodi.setKoodiArvo("FIN");
        Koodi saintMartinKoodi = new Koodi();
        saintMartinKoodi.setKoodiArvo("MAF");

        HenkiloPerustietoDto onrHenkilo = new HenkiloPerustietoDto();
        onrHenkilo.setOidHenkilo("1.2.246.562.24.86368188549");
        onrHenkilo.setEtunimet("Feliks Esaias");
        onrHenkilo.setSukunimi("Pakarinen");

        KansalaisuusDto kansalaisuus = new KansalaisuusDto();
        kansalaisuus.setKansalaisuusKoodi("246");
        onrHenkilo.setKansalaisuus(Sets.newHashSet(kansalaisuus));

        when(httpClient.postJson(
                eq(applicationsUrl),
                any(Duration.class),
                eq(Lists.newArrayList(hakemusOid1, hakemusOid2, hakemusOid3)),
                eq(new TypeToken<List<String>>() {}.getType()),
                eq(new TypeToken<List<AtaruHakemus>>() {}.getType())
        )).thenReturn(CompletableFuture.completedFuture(MockAtaruAsyncResource.getAtaruHakemukset(null)));

        when(mockKoodisto.maatjavaltiot2ToMaatjavaltiot1(eq("maatjavaltiot2_246"))).thenReturn(CompletableFuture.completedFuture(suomiKoodi));
        when(mockKoodisto.maatjavaltiot2ToMaatjavaltiot1(eq("maatjavaltiot2_663"))).thenReturn(CompletableFuture.completedFuture(saintMartinKoodi));

        Map<String, HenkiloPerustietoDto> henkiloResponse = new HashMap<>();
        henkiloResponse.put("1.2.246.562.24.86368188549", onrHenkilo);
        when(mockOnr.haeHenkilot(Collections.singletonList("1.2.246.562.24.86368188549"))).thenReturn(CompletableFuture.completedFuture(henkiloResponse));

        List<HakemusWrapper> applications = ataruAsyncResource.getApplicationsByOids(Lists.newArrayList(hakemusOid1, hakemusOid2, hakemusOid3))
                .get(1, SECONDS);
        assertEquals(3, applications.size());
        assertEquals("FIN", applications.get(0).getAsuinmaa());
        assertEquals("MAF", applications.get(1).getAsuinmaa());
        assertEquals("Feliks", applications.get(2).getEtunimi());
        assertEquals("FIN", applications.get(0).getKansalaisuus());
    }
}
