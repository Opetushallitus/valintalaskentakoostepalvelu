package fi.vm.sade.valinta.kooste.laskentakerralla;


import com.google.common.util.concurrent.Futures;
import fi.vm.sade.valinta.kooste.external.resource.PeruutettavaImpl;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import junit.framework.Assert;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.ws.rs.container.AsyncResponse;
import java.util.ArrayList;
import java.util.function.Consumer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class LaskentaKerrallaFailTest extends LaskentaKerrallaBase {
    @Before
    public void build() {

        ArgumentCaptor<Consumer> argument = ArgumentCaptor.forClass(Consumer.class);
        when(valintaperusteetAsyncResource.haunHakukohteet(any(), any(), argument.capture()))
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

        verifyZeroInteractions(applicationAsyncResource);
        verifyZeroInteractions(ohjausparametritAsyncResource);
        verifyZeroInteractions(laskentaSeurantaAsyncResource);
        verifyZeroInteractions(valintalaskentaAsyncResource);
        verifyZeroInteractions(suoritusrekisteriAsyncResource);

        verify(asyncResponse, times(1)).resume(isA(ResponseImpl.class));
        ArgumentCaptor<ResponseImpl> responseCaptor = ArgumentCaptor.forClass(ResponseImpl.class);
        verify(asyncResponse).resume(responseCaptor.capture());

        Assert.assertEquals(500, responseCaptor.getValue().getStatus());
    }
}
