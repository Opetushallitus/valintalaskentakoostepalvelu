package fi.vm.sade.valinta.kooste.laskentakerralla;


import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import fi.vm.sade.valinta.kooste.mocks.MockOrganisaationAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaKerrallaResource;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = LaskentaKerrallaContext.class)
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class })
public class LaskentaKerrallaFailTest {
    private static final String LASKENTASEURANTA_ID = "laskentaseuranta.id";

    @Autowired
    ValintalaskentaKerrallaResource valintalaskentaKerralla;

    @BeforeClass
    public static void resetMocks() {
        Mocks.resetMocks();
    }

    @Before
    public void yksiLaskentaTyonAlleJokaEpaonnistuu() {
        when(Mocks.valintaperusteetAsyncResource.haunHakukohteet(any())).thenReturn(Observable.error(new Throwable("FAIL")));
        AtomicInteger seurantaCount = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (seurantaCount.getAndIncrement() < 1)
                return Observable.just(LASKENTASEURANTA_ID);
            else {
                return Observable.just(null);
            }
        }).when(Mocks.laskentaSeurantaAsyncResource).otaSeuraavaLaskentaTyonAlle();
    }

    @Test
    public void testValintaperusteetHaunHakukohteetFail() {
        MockOrganisaationAsyncResource.setOrganisaationTyyppiHierarkia(null);
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        try {
            valintalaskentaKerralla.valintalaskentaHaulle(
                    "haku.oid",
                    false,
                    0,
                    false,
                    "haun nimi",
                    "nimi",
                    LaskentaTyyppi.HAKUKOHDE,
                    false,
                    new ArrayList(),
                    asyncResponse);
        } catch (Throwable t) { }

        verifyZeroInteractions(Mocks.applicationAsyncResource);
        verifyZeroInteractions(Mocks.ohjausparametritAsyncResource);
        verifyZeroInteractions(Mocks.valintalaskentaAsyncResource);
        verifyZeroInteractions(Mocks.suoritusrekisteriAsyncResource);

        verify(asyncResponse, times(1)).resume(isA(ResponseImpl.class));
        ArgumentCaptor<ResponseImpl> responseCaptor = ArgumentCaptor.forClass(ResponseImpl.class);
        verify(asyncResponse).resume(responseCaptor.capture());

        assertEquals(500, responseCaptor.getValue().getStatus());
    }
}
