package fi.vm.sade.valinta.kooste.laskentakerralla;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import fi.vm.sade.tarjonta.service.resources.v1.dto.HakuV1RDTO;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaKerrallaResource;
import fi.vm.sade.valinta.seuranta.dto.*;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import rx.Observable;

import javax.ws.rs.container.AsyncResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;


@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {LaskentaKerrallaContext.class})
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class })
public class LaskentaKerrallaTest {
    private static final String HAKU_OID = "haku.oid";
    private static final String TARJOAJA_OID = "tarjoaja.oid";
    private static final String HAKUKOHDE_OID = "hakukohde.oid";
    private static final String PERSON_OID = "007.007";
    private static final String LASKENTASEURANTA_ID = "laskentaseuranta.id";
    private static final String HAKEMUS_OID = "123.123";

    @Autowired
    ValintalaskentaKerrallaResource valintalaskentaKerralla;

    @BeforeClass
    public static void resetMocks() {
        Mocks.resetMocks();
    }

    @Ignore
    @Test
    public void testOnnistunutLaskenta() throws InterruptedException {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        final Object signal = new Object();
        doAnswer(invocation -> {
            synchronized (signal) {
                signal.notify();
            }
            return null;
        }).when(Mocks.laskentaActorSystem).ready(any());
        releaseWorkerAfterStartupReservesThemAll();
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
        assertEquals(200, responseCaptor.getValue().getStatus());

        verify(Mocks.applicationAsyncResource, times(1)).getApplicationsByOid(eq(HAKU_OID), eq(HAKUKOHDE_OID));
        verify(Mocks.suoritusrekisteriAsyncResource, times(1)).getOppijatByHakukohde(eq(HAKUKOHDE_OID), any());

        ArgumentCaptor<LaskeDTO> laskentaInputCaptor = ArgumentCaptor.forClass(LaskeDTO.class);
        verify(Mocks.valintalaskentaAsyncResource, times(1)).laske(laskentaInputCaptor.capture());

        LaskeDTO laskeDTO = laskentaInputCaptor.getValue();
        assertEquals(1, laskeDTO.getHakemus().size());
        assertEquals(PERSON_OID, laskeDTO.getHakemus().get(0).getHakijaOid());
        assertEquals(HAKUKOHDE_OID, laskeDTO.getHakukohdeOid());

        verify(Mocks.laskentaSeurantaAsyncResource, times(1)).merkkaaHakukohteenTila(LASKENTASEURANTA_ID, HAKUKOHDE_OID, HakukohdeTila.VALMIS, Optional.empty());
    }

    private void releaseWorkerAfterStartupReservesThemAll() {
        Mocks.laskentaActorSystem.resetActorCounter();
    }

    @Before
    public void build() {
        ArgumentCaptor<Consumer> argument = ArgumentCaptor.forClass(Consumer.class);
        when(Mocks.valintaperusteetAsyncResource.haunHakukohteet(any(), argument.capture(), any()))
                .thenAnswer(
                        invocation -> {
                            argument.getValue().accept(Arrays.asList(LaskentaKerrallaTestData.julkaistuHakukohdeViite(HAKUKOHDE_OID, TARJOAJA_OID)));
                            return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                        }
                );
        when(Mocks.valintaperusteetAsyncResource.haeValintaperusteet(any(), any()))
                .thenAnswer(
                        invocation -> Observable
                            .from(Arrays.asList(Arrays.asList(LaskentaKerrallaTestData.valintaperusteet(HAKU_OID, TARJOAJA_OID, HAKUKOHDE_OID))))
                );

        when(Mocks.valintaperusteetAsyncResource.haeHakijaryhmat(any()))
                .thenAnswer(
                        invocation -> Observable
                                .from(Arrays.asList(Arrays.asList(LaskentaKerrallaTestData.valintaperusteet(HAKU_OID, TARJOAJA_OID, HAKUKOHDE_OID)))
                                ));

        when(Mocks.ohjausparametritAsyncResource.haeHaunOhjausparametrit(any(), argument.capture(), any()))
                .thenAnswer(
                        invocation -> {
                            argument.getValue().accept(LaskentaKerrallaTestData.ohjausparametrit());
                            return new PeruutettavaImpl(Futures.immediateCancelledFuture());
                        }
                );

        when(Mocks.applicationAsyncResource.getApplicationsByOid(eq(HAKU_OID), eq(HAKUKOHDE_OID)))
                .thenAnswer(
                        invocation -> Observable
                                .from(Arrays.asList(Arrays.asList(LaskentaKerrallaTestData.hakemus(HAKEMUS_OID, PERSON_OID)))
                                ));

        when(Mocks.suoritusrekisteriAsyncResource.getOppijatByHakukohde(eq(HAKUKOHDE_OID), any()))
                .thenAnswer(
                        invocation ->
                            Observable
                                    .from(Arrays.asList(Arrays.asList(LaskentaKerrallaTestData.oppija()))
                ));
        when(Mocks.valintalaskentaAsyncResource.laske(any()))
                .thenAnswer(
                        invocation -> Observable
                                .from(Arrays.asList("string"))
                );
        when(Mocks.tarjontaAsyncResource.haeHaku(any()))
                .thenAnswer(invocation -> rx.Observable.just(buildHakuDto()));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            ((Consumer)args[2]).accept(LASKENTASEURANTA_ID);
            return new PeruutettavaImpl(Futures.immediateFuture(LASKENTASEURANTA_ID));
        }).when(Mocks.laskentaSeurantaAsyncResource).luoLaskenta(any(), any(), argument.capture(), any());

        doAnswer(invocation -> {
                    Consumer<LaskentaDto> laskentaDtoConsumer = (Consumer<LaskentaDto>) invocation.getArguments()[1];
                    laskentaDtoConsumer.accept(new LaskentaDto(LASKENTASEURANTA_ID, "", HAKU_OID, System.currentTimeMillis(), LaskentaTila.MENEILLAAN, LaskentaTyyppi.HAKUKOHDE, null, Lists.newArrayList(new HakukohdeDto(HAKUKOHDE_OID, "org_oid")), false, 0, false, null));
                    return null;
                }
        ).when(Mocks.laskentaSeurantaAsyncResource).laskenta(eq(LASKENTASEURANTA_ID), any(), any());

        AtomicInteger seurantaCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (seurantaCount.getAndIncrement() < 1)
                ((Consumer<String>) invocation.getArguments()[0]).accept(LASKENTASEURANTA_ID);
            else {
                ((Consumer<String>) invocation.getArguments()[0]).accept(null);
            }
            return null;
        }).when(Mocks.laskentaSeurantaAsyncResource).otaSeuraavaLaskentaTyonAlle(any(), any());

    }

    private HakuV1RDTO buildHakuDto() {
        HakuV1RDTO dto = new HakuV1RDTO();
        dto.setOid(HAKU_OID);
        dto.setHakukohdeOids(Arrays.asList(HAKUKOHDE_OID));
        return dto;
    }
}

