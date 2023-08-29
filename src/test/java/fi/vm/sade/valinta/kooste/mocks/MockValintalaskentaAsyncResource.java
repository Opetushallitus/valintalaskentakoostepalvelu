package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.JonoDto;
import fi.vm.sade.valintalaskenta.domain.dto.LaskeDTO;
import fi.vm.sade.valintalaskenta.domain.dto.SuoritustiedotDTO;
import fi.vm.sade.valintalaskenta.domain.dto.ValinnanvaiheDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.ValintatietoValinnanvaiheDTO;
import io.reactivex.Observable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** @author Jussi Jartamo */
@Profile("mockresources")
@Service
public class MockValintalaskentaAsyncResource implements ValintalaskentaAsyncResource {

  private static AtomicReference<List<ValintatietoValinnanvaiheDTO>> resultReference =
      new AtomicReference<>();

  public static void setResult(List<ValintatietoValinnanvaiheDTO> result) {
    resultReference.set(result);
  }

  public static void clear() {
    resultReference.set(null);
  }

  @Override
  public Observable<String> laske(LaskeDTO laskeDTO, SuoritustiedotDTO suoritukset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Observable<String> laskeKaikki(LaskeDTO laskeDTO, SuoritustiedotDTO suoritukset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Observable<String> valintakokeet(LaskeDTO laskeDTO, SuoritustiedotDTO suoritukset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<ValintatietoValinnanvaiheDTO>> laskennantulokset(
      String hakukohdeOid) {
    return CompletableFuture.completedFuture(resultReference.get());
  }

  @Override
  public Observable<List<JonoDto>> jonotSijoitteluun(String hakuOid) {
    return null;
  }

  @Override
  public CompletableFuture<ValinnanvaiheDTO> lisaaTuloksia(
      final String hakuOid,
      final String hakukohdeOid,
      final String tarjoajaOid,
      final ValinnanvaiheDTO vaihe) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Observable<String> laskeJaSijoittele(
      String uuid, List<LaskeDTO> lista, SuoritustiedotDTO suoritustiedot) {
    throw new UnsupportedOperationException();
  }
}
