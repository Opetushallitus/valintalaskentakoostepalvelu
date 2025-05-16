package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeImportDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeJaValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValinnanVaiheJonoillaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintakoeDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteetHakijaryhmaDTO;
import fi.vm.sade.service.valintaperusteet.dto.ValintatapajonoDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import io.reactivex.Observable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @author Jussi Jartamo
 */
@Profile("mockresources")
@Service
public class MockValintaperusteetAsyncResource implements ValintaperusteetAsyncResource {
  private static AtomicReference<List<HakukohdeJaValintakoeDTO>> hakukohdeResultReference =
      new AtomicReference<>();
  private static AtomicReference<Set<String>> hakukohteetValinnanvaiheelleResultReference =
      new AtomicReference<>();
  private static AtomicReference<List<ValinnanVaiheJonoillaDTO>> resultReference =
      new AtomicReference<>();
  private static AtomicReference<List<HakukohdeJaValintaperusteDTO>>
      hakukohdeJaValintaperusteetResultReference = new AtomicReference<>();
  private static AtomicReference<List<ValintaperusteDTO>> valintaperusteetResultReference =
      new AtomicReference<>();
  private static AtomicReference<List<ValintakoeDTO>> valintakokeetResultReference =
      new AtomicReference<>();
  private static AtomicReference<List<ValintaperusteetDTO>> hakukohteenValintaperusteetReference =
      new AtomicReference<>();

  public static void setValintaperusteetResult(List<ValintaperusteDTO> result) {
    valintaperusteetResultReference.set(result);
  }

  public static void setHakukohteetValinnanvaiheelleResult(Set<String> result) {
    hakukohteetValinnanvaiheelleResultReference.set(result);
  }

  public Observable<Map<String, List<ValintatapajonoDTO>>> haeValintatapajonotSijoittelulle(
      Collection<String> hakukohdeOids) {
    return null;
  }

  @Override
  public CompletableFuture<List<ValintaperusteetHakijaryhmaDTO>> haeHakijaryhmat(
      String hakukohdeOid) {
    return null;
  }

  @Override
  public CompletableFuture<List<ValintaperusteetDTO>> haeValintaperusteet(
      String hakukohdeOid, Integer valinnanVaiheJarjestysluku) {
    return CompletableFuture.completedFuture(hakukohteenValintaperusteetReference.get());
  }

  public static void setHakukohteenValintaperusteetResult(
      List<ValintaperusteetDTO> hakukohteenValintaperusteetResult) {
    MockValintaperusteetAsyncResource.hakukohteenValintaperusteetReference.set(
        hakukohteenValintaperusteetResult);
  }

  public static void setHakukohdeValintaperusteResult(List<HakukohdeJaValintaperusteDTO> result) {
    hakukohdeJaValintaperusteetResultReference.set(result);
  }

  public static void setValintakokeetResult(List<ValintakoeDTO> result) {
    valintakokeetResultReference.set(result);
  }

  public static void setResult(List<ValinnanVaiheJonoillaDTO> result) {
    resultReference.set(result);
  }

  public static void setHakukohdeResult(List<HakukohdeJaValintakoeDTO> result) {
    hakukohdeResultReference.set(result);
  }

  public static void clear() {
    resultReference.set(null);
    hakukohdeResultReference.set(null);
    hakukohdeJaValintaperusteetResultReference.set(null);
    valintaperusteetResultReference.set(null);
    valintakokeetResultReference.set(null);
  }

  @Override
  public Observable<List<ValintakoeDTO>> haeValintakokeetHakukohteelle(String hakukohdeOid) {
    return Observable.just(valintakokeetResultReference.get());
  }

  @Override
  public Observable<Set<String>> haeHakukohteetValinnanvaiheelle(String oid) {
    return Observable.just(hakukohteetValinnanvaiheelleResultReference.get());
  }

  @Override
  public Observable<List<HakukohdeViiteDTO>> haunHakukohteet(String hakuOid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<HakukohdeViiteDTO>> haunHakukohteetF(
      String hakuOid, Boolean vainValintakokeelliset) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<ValintaperusteDTO>> findAvaimet(String hakukohdeOid) {
    return CompletableFuture.completedFuture(valintaperusteetResultReference.get());
  }

  @Override
  public Observable<List<HakukohdeJaValintaperusteDTO>> findAvaimet(
      Collection<String> hakukohdeOids) {
    return Observable.just(hakukohdeJaValintaperusteetResultReference.get());
  }

  @Override
  public Observable<List<ValintaperusteetDTO>> valintaperusteet(String valinnanvaiheOid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Observable<ResponseEntity> tuoHakukohde(HakukohdeImportDTO hakukohde) {
    throw new UnsupportedOperationException();
  }

  @Override
  public CompletableFuture<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteilleF(
      Collection<String> hakukohdeOids) {
    return CompletableFuture.completedFuture(hakukohdeResultReference.get());
  }

  @Override
  public Observable<List<HakukohdeJaValintakoeDTO>> haeValintakokeetHakukohteille(
      Collection<String> hakukohdeOids) {
    return Observable.just(hakukohdeResultReference.get());
  }

  @Override
  public Observable<List<ValinnanVaiheJonoillaDTO>> haeIlmanlaskentaa(String hakukohdeOid) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Observable<String> haeValintaryhmaVastuuorganisaatio(String valintaryhmaOid) {
    throw new UnsupportedOperationException();
  }
}
