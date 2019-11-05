package fi.vm.sade.valinta.kooste.external.resource.koski.impl;

import static fi.vm.sade.valinta.kooste.test.KoostepalveluTestingHttpUtil.createMockJsonResponse;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import fi.vm.sade.valinta.kooste.external.resource.HttpClient;
import fi.vm.sade.valinta.kooste.external.resource.koski.KoskiOppija;
import fi.vm.sade.valinta.kooste.test.KoostepalveluTestingHttpUtil;
import fi.vm.sade.valinta.kooste.url.UrlConfiguration;
import fi.vm.sade.valinta.sharedutils.http.DateDeserializer;
import org.junit.Test;

import java.io.InputStream;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class KoskiAsyncResourceImplTest {
    private final String username = "koostepalvelu2koskiUser";
    private final String password = "koostepalvelu2koskiPass";
    private final int maxOppijatPostSize = 10;
    private java.net.http.HttpClient httpClient = mock(java.net.http.HttpClient.class);
    private final HttpClient koosteHttpClient = new HttpClient(
        httpClient,
        null,
        DateDeserializer.gsonBuilder().create()
    );
    private final UrlConfiguration urlConfiguration = mock(UrlConfiguration.class);
    private final KoskiAsyncResourceImpl koskiAsyncResource = new KoskiAsyncResourceImpl(urlConfiguration, koosteHttpClient, username, password, maxOppijatPostSize);

    @Test
    public void findKoskiOppijat() throws Exception {
        when(urlConfiguration.url("koski.oppijanumeroittain.post")).thenReturn("http://localhost/koski/would/be/here");

        List<String> oppijaNumerotToTest = Arrays.asList("1.2.246.562.24.30338561184", "1.2.246.562.24.32656706483");

        HttpResponse<InputStream> response = createMockJsonResponse(
            "fi/vm/sade/valinta/kooste/external/resource/koski/impl/KoskiAsyncResourceImplTest-koskiresponse.json");

        when(httpClient.sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(CompletableFuture.completedFuture(response));

        Set<KoskiOppija> koskiOppijas = koskiAsyncResource.findKoskiOppijat(oppijaNumerotToTest).get(10, SECONDS);

        verify(httpClient).sendAsync(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        verifyNoMoreInteractions(httpClient);

        assertThat(koskiOppijas, hasSize(1));
    }
}
