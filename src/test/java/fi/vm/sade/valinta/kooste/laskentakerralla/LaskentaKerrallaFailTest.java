package fi.vm.sade.valinta.kooste.laskentakerralla;


import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaKerrallaResource;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import junit.framework.Assert;
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

import javax.ws.rs.container.AsyncResponse;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = LaskentaKerrallaContext.class)
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class })
public class LaskentaKerrallaFailTest {

    @Autowired
    ValintalaskentaKerrallaResource valintalaskentaKerralla;


    @BeforeClass
    public static void resetMocks() {
        Mocks.resetMocks();
    }

    @Before
    public void build() {

        ArgumentCaptor<Consumer> argument = ArgumentCaptor.forClass(Consumer.class);
        when(Mocks.valintaperusteetAsyncResource.haunHakukohteet(any(), any(), argument.capture()))
            .thenAnswer(
                invocation -> {
                    argument.getValue().accept(new RuntimeException());
                    return new PeruutettavaImpl(Futures.immediateFailedFuture(new Throwable("FAIL")));
                }
            );
    }

    @Test
    public void testValintaperusteetHaunHakukohteetFail() throws InterruptedException {
        AsyncResponse asyncResponse = mock(AsyncResponse.class);
        try {
            valintalaskentaKerralla.valintalaskentaHaulle(
                    "haku.oid",
                    false,
                    0,
                    false,
                    LaskentaTyyppi.HAKUKOHDE,
                    false,
                    new ArrayList(),
                    asyncResponse);
        } catch (Throwable t) { }

        verify(Mocks.laskentaSeurantaAsyncResource, times(1)).otaSeuraavaLaskentaTyonAlle(any(), any());
        verifyNoMoreInteractions(Mocks.laskentaSeurantaAsyncResource);
        verifyZeroInteractions(Mocks.applicationAsyncResource);
        verifyZeroInteractions(Mocks.ohjausparametritAsyncResource);
        verifyZeroInteractions(Mocks.valintalaskentaAsyncResource);
        verifyZeroInteractions(Mocks.suoritusrekisteriAsyncResource);

        verify(asyncResponse, times(1)).resume(isA(ResponseImpl.class));
        ArgumentCaptor<ResponseImpl> responseCaptor = ArgumentCaptor.forClass(ResponseImpl.class);
        verify(asyncResponse).resume(responseCaptor.capture());

        Assert.assertEquals(500, responseCaptor.getValue().getStatus());
    }
}
