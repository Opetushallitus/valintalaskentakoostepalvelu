package fi.vm.sade.valinta.kooste.mocks;

import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import fi.vm.sade.valintalaskenta.domain.dto.valintatieto.HakemusOsallistuminenDTO;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

/** @author Jussi Jartamo */
@Service
public class MockValintalaskentaValintakoeAsyncResource
    implements ValintalaskentaValintakoeAsyncResource {

  private static final AtomicReference<List<ValintakoeOsallistuminenDTO>> osallistumistiedot =
      new AtomicReference<>();
  private static final AtomicReference<List<HakemusOsallistuminenDTO>> hakemusOsallistuminen =
      new AtomicReference<>();

  public static void setHakemusOsallistuminenResult(List<HakemusOsallistuminenDTO> res) {
    hakemusOsallistuminen.set(res);
  }

  public static void setResult(List<ValintakoeOsallistuminenDTO> res) {
    osallistumistiedot.set(res);
  }

  public static void clear() {
    osallistumistiedot.set(null);
    hakemusOsallistuminen.set(null);
  }

  @Override
  public CompletableFuture<List<HakemusOsallistuminenDTO>> haeValintatiedotHakukohteelle(
      String hakukohdeOid, List<String> valintakoeOid) {
    return CompletableFuture.completedFuture(hakemusOsallistuminen.get());
  }

  @Override
  public CompletableFuture<ValintakoeOsallistuminenDTO> haeHakemukselle(String hakemusOid) {
    throw new UnsupportedOperationException("Not implemented yet.");
  }

  @Override
  public CompletableFuture<List<ValintakoeOsallistuminenDTO>> haeHakutoiveelle(
      String hakukohdeOid) {
    return CompletableFuture.completedFuture(osallistumistiedot.get());
  }

  @Override
  public CompletableFuture<List<ValintakoeOsallistuminenDTO>> haeHakutoiveille(
      Collection<String> hakukohdeOids) {
    return CompletableFuture.completedFuture(osallistumistiedot.get());
  }
}
