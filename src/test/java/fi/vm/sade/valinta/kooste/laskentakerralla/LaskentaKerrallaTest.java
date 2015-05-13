package fi.vm.sade.valinta.kooste.laskentakerralla;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.external.resource.haku.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.seuranta.dto.*;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import junit.framework.Assert;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.container.AsyncResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@RunWith(SpringJUnit4ClassRunner.class)
public class LaskentaKerrallaTest extends LaskentaKerrallaBase {
    private static final String HAKU_OID = "haku.oid";
    private static final String TARJOAJA_OID = "tarjoaja.oid";
    private static final String HAKUKOHDE_OID = "hakukohde.oid";
    private static final String PERSON_OID = "007.007";
    private static final String LASKENTASEURANTA_ID = "laskentaseuranta.id";
    private static final String HAKEMUS_OID = "123.123";

    @Test
    public void testOnnistunutLaskenta() throws InterruptedException {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        final Object signal = new Object();
        doAnswer(invocation -> {
            synchronized (signal) {
                signal.notify();
            }
            return null;
        }).when(laskentaActorSystem).valmis(any());

        valintalaskentaKerralla.valintalaskentaHaulle(
            HAKU_OID,
            false,
            0,
            false,
            LaskentaTyyppi.HAKUKOHDE,
            false,
            new ArrayList(),
            asyncResponse);

        synchronized (signal) {
            signal.wait(10000);
        }
        verify(asyncResponse, times(1)).resume(isA(ResponseImpl.class));
        ArgumentCaptor<ResponseImpl> responseCaptor = ArgumentCaptor.forClass(ResponseImpl.class);
        verify(asyncResponse).resume(responseCaptor.capture());
        Assert.assertEquals(200, responseCaptor.getValue().getStatus());

        verify(applicationAsyncResource, times(1)).getApplicationsByOid(eq(HAKU_OID), eq(HAKUKOHDE_OID), any(), any());
        verify(suoritusrekisteriAsyncResource, times(1)).getOppijatByHakukohde(eq(HAKUKOHDE_OID), any(), any(), any());

        ArgumentCaptor<LaskeDTO> laskentaInputCaptor = ArgumentCaptor.forClass(LaskeDTO.class);
        verify(valintalaskentaAsyncResource, times(1)).laske(laskentaInputCaptor.capture(), any(), any());

        LaskeDTO laskeDTO = laskentaInputCaptor.getValue();
        Assert.assertEquals(1, laskeDTO.getHakemus().size());
        Assert.assertEquals(PERSON_OID, laskeDTO.getHakemus().get(0).getHakijaOid());
        Assert.assertEquals(HAKUKOHDE_OID, laskeDTO.getHakukohdeOid());

        verify(laskentaSeurantaAsyncResource, times(1)).merkkaaHakukohteenTila(LASKENTASEURANTA_ID, HAKUKOHDE_OID, HakukohdeTila.VALMIS);
    }

    @Before
    public void build() {
        ArgumentCaptor<Consumer> argument = ArgumentCaptor.forClass(Consumer.class);
        when(valintaperusteetAsyncResource.haunHakukohteet(any(), argument.capture(), any()))
            .thenAnswer(
                invocation -> {
                    argument.getValue().accept(Arrays.asList(LaskentaKerrallaTestData.julkaistuHakukohdeViite(HAKUKOHDE_OID, TARJOAJA_OID)));
                    return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                }
            );
        when(valintaperusteetAsyncResource.haeValintaperusteet(any(), any(), argument.capture(), any()))
            .thenAnswer(
                invocation -> {
                    argument.getValue().accept(Arrays.asList(LaskentaKerrallaTestData.valintaperusteet(HAKU_OID, TARJOAJA_OID, HAKUKOHDE_OID)));
                    return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                }
            );

        when(valintaperusteetAsyncResource.haeHakijaryhmat(any(), argument.capture(), any()))
            .thenAnswer(
                invocation -> {
                    argument.getValue().accept(Arrays.asList(LaskentaKerrallaTestData.valintaperusteet(HAKU_OID, TARJOAJA_OID, HAKUKOHDE_OID)));
                    return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                }
            );

        when(ohjausparametritAsyncResource.haeHaunOhjausparametrit(any(), argument.capture(), any()))
            .thenAnswer(
                invocation -> {
                    argument.getValue().accept(LaskentaKerrallaTestData.ohjausparametrit());
                    return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                }
            );

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((Consumer)args[6]).accept(LASKENTASEURANTA_ID);
            return new PeruutettavaImpl(Futures.immediateFuture(LASKENTASEURANTA_ID));
        }).when(laskentaSeurantaAsyncResource).luoLaskenta(any(), any(), any(), any(), any(), any(), argument.capture(), any());

        AtomicInteger seurantaCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (seurantaCount.getAndIncrement() < 1)
                ((Consumer<String>) invocation.getArguments()[0]).accept(LASKENTASEURANTA_ID);
            else {
                ((Consumer<String>) invocation.getArguments()[0]).accept(null);
            }
            return null;
        }).when(laskentaSeurantaAsyncResource).otaSeuraavaLaskentaTyonAlle(any(), any());

        doAnswer(invocation -> {
                    Consumer<LaskentaDto> laskentaDtoConsumer = (Consumer<LaskentaDto>) invocation.getArguments()[1];
                    laskentaDtoConsumer.accept(new LaskentaDto(LASKENTASEURANTA_ID, HAKU_OID, System.currentTimeMillis(), LaskentaTila.MENEILLAAN, LaskentaTyyppi.HAKUKOHDE, Lists.newArrayList(new HakukohdeDto(HAKUKOHDE_OID, "org_oid")), false, 0, false));
                    return null;
                }
        ).when(laskentaSeurantaAsyncResource).laskenta(eq(LASKENTASEURANTA_ID), any(), any());

        when(applicationAsyncResource.getApplicationsByOid(eq(HAKU_OID), eq(HAKUKOHDE_OID), argument.capture(), any()))
            .thenAnswer(
                invocation -> {
                    argument.getValue().accept(Arrays.asList(LaskentaKerrallaTestData.hakemus(HAKEMUS_OID, PERSON_OID)));
                    return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                }
            );

        when(suoritusrekisteriAsyncResource.getOppijatByHakukohde(eq(HAKUKOHDE_OID), any(), argument.capture(), any()))
            .thenAnswer(
                invocation -> {
                    argument.getValue().accept(Arrays.asList(LaskentaKerrallaTestData.oppija()));
                    return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                }
            );
        when(valintalaskentaAsyncResource.laske(any(), argument.capture(), any()))
            .thenAnswer(
                invocation -> {
                    argument.getValue().accept("string");
                    return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                }
            );
    }
}

