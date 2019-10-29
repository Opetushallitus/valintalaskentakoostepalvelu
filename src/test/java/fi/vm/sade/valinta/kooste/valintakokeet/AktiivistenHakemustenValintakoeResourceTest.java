package fi.vm.sade.valinta.kooste.valintakokeet;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec;
import fi.vm.sade.valinta.kooste.spec.tarjonta.TarjontaSpec;
import fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.ValintakoeOsallistuminenBuilder;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import io.reactivex.Observable;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AktiivistenHakemustenValintakoeResourceTest {
    private final String hakukohdeOid = "hakukohdeOid";
    private final String hakuOid = "hakuOid";
    private final String ataruLomakeAvain = "AtaruLomakeAvain";
    private HakemusWrapper hakemus1 = new HakemusSpec.HakemusBuilder().setOid("hakemus1").build();
    private HakemusWrapper hakemus2 = new HakemusSpec.HakemusBuilder().setOid("hakemus2").build();
    private HakemusWrapper hakemus3 = new HakemusSpec.HakemusBuilder().setOid("hakemus3").build();

    private HakemusWrapper ataruHakemus1 = new HakemusSpec.AtaruHakemusBuilder("ataruHakemus1", "personOid1", "hetu1").build();
    private HakemusWrapper ataruHakemus2 = new HakemusSpec.AtaruHakemusBuilder("ataruHakemus2", "personOid2", "hetu2").build();
    private HakemusWrapper ataruHakemus3 = new HakemusSpec.AtaruHakemusBuilder("ataruHakemus3", "personOid3", "hetu3").build();

    private List<String> hakemusOids = Arrays.asList("hakemus1", "hakemus2", "hakemus3");
    private List<String> ataruHakemusOids = Arrays.asList("ataruHakemus1", "ataruHakemus2", "ataruHakemus3");
    private ValintakoeOsallistuminenDTO osallistuminen1 = new ValintakoeOsallistuminenBuilder().
        setHakemusOid(hakemus1.getOid()).build();
    private ValintakoeOsallistuminenDTO osallistuminen2 = new ValintakoeOsallistuminenBuilder().
        setHakemusOid(hakemus2.getOid()).build();
    private ValintakoeOsallistuminenDTO osallistuminen3 = new ValintakoeOsallistuminenBuilder().
        setHakemusOid(hakemus3.getOid()).build();

    private ValintakoeOsallistuminenDTO ataruOsallistuminen1 = new ValintakoeOsallistuminenBuilder().
            setHakemusOid(ataruHakemus1.getOid()).build();
    private ValintakoeOsallistuminenDTO ataruOsallistuminen2 = new ValintakoeOsallistuminenBuilder().
            setHakemusOid(ataruHakemus2.getOid()).build();
    private ValintakoeOsallistuminenDTO ataruOsallistuminen3 = new ValintakoeOsallistuminenBuilder().
            setHakemusOid(ataruHakemus3.getOid()).build();

    private final HakukohdeV1RDTO hakukohdeDTO = new TarjontaSpec.HakukohdeBuilder(hakuOid).build();
    private final HakuV1RDTO hakuDTOEditori = new TarjontaSpec.HakuBuilder(hakuOid, ataruLomakeAvain).build();
    private final HakuV1RDTO hakuDTOHakuApp = new TarjontaSpec.HakuBuilder(hakuOid, null).build();

    private final ValintalaskentaValintakoeAsyncResource valintakoeAsyncResource
        = mock(ValintalaskentaValintakoeAsyncResource.class);
    private final ApplicationAsyncResource applicationAsyncResource = mock(ApplicationAsyncResource.class);
    private final AtaruAsyncResource ataruAsyncResource = mock(AtaruAsyncResource.class);
    private final TarjontaAsyncResource tarjontaAsyncResource = mock(TarjontaAsyncResource.class);
    private final AktiivistenHakemustenValintakoeResource resource = new AktiivistenHakemustenValintakoeResource(
        valintakoeAsyncResource, applicationAsyncResource, ataruAsyncResource, tarjontaAsyncResource);
    private Response responseReceivedInAsyncResponse;

    @Test
    public void vainApplicationAsyncResourcenPalauttamienHakemustenOsallistumisetPalautetaan() {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        when(valintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid))
            .thenReturn(Observable.just(Arrays.asList(osallistuminen1, osallistuminen2, osallistuminen3)));
        when(applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids))
            .thenReturn(Observable.just(Arrays.asList(hakemus1, hakemus3)));
        when(tarjontaAsyncResource.haeHakukohde(hakukohdeOid))
            .thenReturn(CompletableFuture.completedFuture(hakukohdeDTO));
        when(tarjontaAsyncResource.haeHaku(hakuOid))
            .thenReturn(CompletableFuture.completedFuture(hakuDTOHakuApp));

        when(asyncResponse.resume(any(Response.class))).thenAnswer((Answer<Boolean>) invocation -> {
            responseReceivedInAsyncResponse = invocation.getArgument(0);
            return true;
        });

        resource.osallistumisetByHakutoive(hakukohdeOid, asyncResponse);

        verify(valintakoeAsyncResource).haeHakutoiveelle(hakukohdeOid);
        verify(applicationAsyncResource).getApplicationsByHakemusOids(hakemusOids);
        verify(asyncResponse).setTimeout(anyLong(), any(TimeUnit.class));
        verify(asyncResponse).resume(any(Response.class));

        verifyNoMoreInteractions(valintakoeAsyncResource);
        verifyNoMoreInteractions(applicationAsyncResource);
        verifyNoMoreInteractions(asyncResponse);

        assertEquals(OK.getStatusCode(), responseReceivedInAsyncResponse.getStatus());
        List<ValintakoeOsallistuminenDTO> osallistumisetVastauksessa =
            (List<ValintakoeOsallistuminenDTO>) responseReceivedInAsyncResponse.getEntity();
        assertThat(osallistumisetVastauksessa, Matchers.hasSize(2));
        assertEquals(hakemus1.getOid(), osallistumisetVastauksessa.get(0).getHakemusOid());
        assertEquals(hakemus3.getOid(), osallistumisetVastauksessa.get(1).getHakemusOid());
    }

    @Test
    public void ataruVainApplicationAsyncResourcenPalauttamienHakemustenOsallistumisetPalautetaan() {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        when(valintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid))
                .thenReturn(Observable.just(Arrays.asList(ataruOsallistuminen1, ataruOsallistuminen2, ataruOsallistuminen3)));
        when(ataruAsyncResource.getApplicationsByOids(ataruHakemusOids))
                .thenReturn(CompletableFuture.completedFuture(Arrays.asList(ataruHakemus1, ataruHakemus3)));
        when(tarjontaAsyncResource.haeHakukohde(hakukohdeOid))
                .thenReturn(CompletableFuture.completedFuture(hakukohdeDTO));
        when(tarjontaAsyncResource.haeHaku(hakuOid))
                .thenReturn(CompletableFuture.completedFuture(hakuDTOEditori));

        when(asyncResponse.resume(any(Response.class))).thenAnswer((Answer<Boolean>) invocation -> {
            responseReceivedInAsyncResponse = invocation.getArgument(0);
            return true;
        });

        resource.osallistumisetByHakutoive(hakukohdeOid, asyncResponse);

        verify(valintakoeAsyncResource).haeHakutoiveelle(hakukohdeOid);
        verify(ataruAsyncResource).getApplicationsByOids(ataruHakemusOids);
        verify(asyncResponse).setTimeout(anyLong(), any(TimeUnit.class));
        verify(asyncResponse).resume(any(Response.class));

        verifyNoMoreInteractions(valintakoeAsyncResource);
        verifyNoMoreInteractions(applicationAsyncResource);
        verifyNoMoreInteractions(asyncResponse);

        assertEquals(OK.getStatusCode(), responseReceivedInAsyncResponse.getStatus());
        List<ValintakoeOsallistuminenDTO> osallistumisetVastauksessa =
                (List<ValintakoeOsallistuminenDTO>) responseReceivedInAsyncResponse.getEntity();
        assertThat(osallistumisetVastauksessa, Matchers.hasSize(2));
        assertEquals(ataruHakemus1.getOid(), osallistumisetVastauksessa.get(0).getHakemusOid());
        assertEquals(ataruHakemus3.getOid(), osallistumisetVastauksessa.get(1).getHakemusOid());
    }

    @Test
    public void kutsuttavanPalvelunVirhePalautetaanVastaukseen() {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        when(tarjontaAsyncResource.haeHakukohde(hakukohdeOid))
                .thenReturn(CompletableFuture.completedFuture(hakukohdeDTO));
        when(tarjontaAsyncResource.haeHaku(hakuOid))
                .thenReturn(CompletableFuture.completedFuture(hakuDTOHakuApp));
        when(valintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid))
            .thenReturn(Observable.just(Arrays.asList(osallistuminen1, osallistuminen2, osallistuminen3)));
        RuntimeException applicationFetchException = new RuntimeException("Hakemusten haku kaatui!");
        when(applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids)).thenThrow(applicationFetchException);

        when(asyncResponse.resume(any(Response.class))).thenAnswer((Answer<Boolean>) invocation -> {
            responseReceivedInAsyncResponse = invocation.getArgument(0);
            return true;
        });

        resource.osallistumisetByHakutoive(hakukohdeOid, asyncResponse);

        verify(valintakoeAsyncResource).haeHakutoiveelle(hakukohdeOid);
        verify(applicationAsyncResource).getApplicationsByHakemusOids(hakemusOids);
        verify(asyncResponse).setTimeout(anyLong(), any(TimeUnit.class));
        verify(asyncResponse).resume(any(Response.class));

        verifyNoMoreInteractions(valintakoeAsyncResource);
        verifyNoMoreInteractions(applicationAsyncResource);
        verifyNoMoreInteractions(asyncResponse);

        assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), responseReceivedInAsyncResponse.getStatus());
        String responseContent = responseReceivedInAsyncResponse.getEntity().toString();
        assertThat(responseContent, containsString(applicationFetchException.getMessage()));
    }
}
