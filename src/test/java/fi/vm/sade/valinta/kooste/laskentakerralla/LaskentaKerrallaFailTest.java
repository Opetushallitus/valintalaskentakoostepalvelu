package fi.vm.sade.valinta.kooste.laskentakerralla;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.mocks.MockOrganisaationAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.resource.ValintalaskentaKerrallaResource;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import io.reactivex.Observable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.web.context.request.async.DeferredResult;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = LaskentaKerrallaContext.class)
@TestExecutionListeners({
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class LaskentaKerrallaFailTest {
  private static final String LASKENTASEURANTA_ID = "laskentaseuranta.id";

  @Autowired ValintalaskentaKerrallaResource valintalaskentaKerralla;

  @BeforeClass
  public static void resetMocks() {
    Mocks.resetMocks();
  }

  @Before
  public void yksiLaskentaTyonAlleJokaEpaonnistuu() {
    when(Mocks.valintaperusteetAsyncResource.haunHakukohteet(any()))
        .thenReturn(Observable.error(new Throwable("FAIL")));
    AtomicInteger seurantaCount = new AtomicInteger(0);
    doAnswer(
            invocation -> {
              if (seurantaCount.getAndIncrement() < 1) return Observable.just(LASKENTASEURANTA_ID);
              else {
                return Observable.just(Optional.empty());
              }
            })
        .when(Mocks.laskentaSeurantaAsyncResource)
        .otaSeuraavaLaskentaTyonAlle();
  }

  @Test
  public void testValintaperusteetHaunHakukohteetFail() {
    MockOrganisaationAsyncResource.setOrganisaationTyyppiHierarkia(null);
    DeferredResult<ResponseEntity<Vastaus>> result = null;

    try {
      result =
          valintalaskentaKerralla.valintalaskentaHaulle(
              "haku.oid",
              false,
              0,
              false,
              "haun nimi",
              "nimi",
              "valintaryhma.oid",
              LaskentaTyyppi.HAKUKOHDE,
              false,
              new ArrayList());
    } catch (Throwable t) {
    }

    verifyNoInteractions(Mocks.applicationAsyncResource);
    verifyNoInteractions(Mocks.ohjausparametritAsyncResource);
    verifyNoInteractions(Mocks.valintalaskentaAsyncResource);
    verifyNoInteractions(Mocks.suoritusrekisteriAsyncResource);

    assertEquals(
        HttpStatus.INTERNAL_SERVER_ERROR,
        ((ResponseEntity<Vastaus>) result.getResult()).getStatusCode());
  }
}
