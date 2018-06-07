package fi.vm.sade.valinta.kooste.valintakokeet;

import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakukohdeV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.spec.hakemus.HakemusSpec;
import fi.vm.sade.valinta.kooste.spec.tarjonta.TarjontaSpec;
import fi.vm.sade.valinta.kooste.spec.valintalaskenta.ValintalaskentaSpec.ValintakoeOsallistuminenBuilder;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import rx.Observable;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

public class AktiivistenHakemustenValintakoeResourceTest {
    private final String hakukohdeOid = "hakukohdeOid";
    private final String hakuOid = "hakuOid";
    private final String ataruLomakeAvain = "AtaruLomakeAvain";
    private Hakemus hakemus1 = new HakemusSpec.HakemusBuilder().setOid("hakemus1").build();
    private Hakemus hakemus2 = new HakemusSpec.HakemusBuilder().setOid("hakemus2").build();
    private Hakemus hakemus3 = new HakemusSpec.HakemusBuilder().setOid("hakemus3").build();

    private AtaruHakemus ataruHakemus1 = new HakemusSpec.AtaruHakemusBuilder("ataruHakemus1").build();
    private AtaruHakemus ataruHakemus2 = new HakemusSpec.AtaruHakemusBuilder("ataruHakemus2").build();
    private AtaruHakemus ataruHakemus3 = new HakemusSpec.AtaruHakemusBuilder("ataruHakemus3").build();

    private List<String> hakemusOids = Arrays.asList("hakemus1", "hakemus2", "hakemus3");
    private List<String> ataruHakemusOids = Arrays.asList("ataruHakemus1", "ataruHakemus2", "ataruHakemus3");
    private ValintakoeOsallistuminenDTO osallistuminen1 = new ValintakoeOsallistuminenBuilder().
        setHakemusOid(hakemus1.getOid()).build();
    private ValintakoeOsallistuminenDTO osallistuminen2 = new ValintakoeOsallistuminenBuilder().
        setHakemusOid(hakemus2.getOid()).build();
    private ValintakoeOsallistuminenDTO osallistuminen3 = new ValintakoeOsallistuminenBuilder().
        setHakemusOid(hakemus3.getOid()).build();

    private ValintakoeOsallistuminenDTO ataruOsallistuminen1 = new ValintakoeOsallistuminenBuilder().
            setHakemusOid(ataruHakemus1.getHakemusOid()).build();
    private ValintakoeOsallistuminenDTO ataruOsallistuminen2 = new ValintakoeOsallistuminenBuilder().
            setHakemusOid(ataruHakemus2.getHakemusOid()).build();
    private ValintakoeOsallistuminenDTO ataruOsallistuminen3 = new ValintakoeOsallistuminenBuilder().
            setHakemusOid(ataruHakemus3.getHakemusOid()).build();

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
            .thenReturn(Observable.just(hakukohdeDTO));
        when(tarjontaAsyncResource.haeHaku(hakuOid))
            .thenReturn(Observable.just(hakuDTOHakuApp));

        when(asyncResponse.resume(any(Response.class))).thenAnswer((Answer<Boolean>) invocation -> {
            responseReceivedInAsyncResponse = invocation.getArgumentAt(0, Response.class);
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
                .thenReturn(Observable.just(Arrays.asList(ataruHakemus1, ataruHakemus3)));
        when(tarjontaAsyncResource.haeHakukohde(hakukohdeOid))
                .thenReturn(Observable.just(hakukohdeDTO));
        when(tarjontaAsyncResource.haeHaku(hakuOid))
                .thenReturn(Observable.just(hakuDTOEditori));

        when(asyncResponse.resume(any(Response.class))).thenAnswer((Answer<Boolean>) invocation -> {
            responseReceivedInAsyncResponse = invocation.getArgumentAt(0, Response.class);
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
        assertEquals(ataruHakemus1.getHakemusOid(), osallistumisetVastauksessa.get(0).getHakemusOid());
        assertEquals(ataruHakemus3.getHakemusOid(), osallistumisetVastauksessa.get(1).getHakemusOid());
    }

    @Test
    public void kutsuttavanPalvelunVirhePalautetaanVastaukseen() {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        when(tarjontaAsyncResource.haeHakukohde(hakukohdeOid))
                .thenReturn(Observable.just(hakukohdeDTO));
        when(tarjontaAsyncResource.haeHaku(hakuOid))
                .thenReturn(Observable.just(hakuDTOHakuApp));
        when(valintakoeAsyncResource.haeHakutoiveelle(hakukohdeOid))
            .thenReturn(Observable.just(Arrays.asList(osallistuminen1, osallistuminen2, osallistuminen3)));
        RuntimeException applicationFetchException = new RuntimeException("Hakemusten haku kaatui!");
        when(applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids)).thenThrow(applicationFetchException);

        when(asyncResponse.resume(any(Response.class))).thenAnswer((Answer<Boolean>) invocation -> {
            responseReceivedInAsyncResponse = invocation.getArgumentAt(0, Response.class);
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
