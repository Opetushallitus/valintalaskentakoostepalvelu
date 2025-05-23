package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ValintojenToteuttaminenService {
  /**
   * Hakee eri palveluista koostettuja hakukohdekohtaisia valintojen tietoja annetulle haulle. Ei
   * välttämättä palauta kaikkia haun hakukohteita.
   *
   * @param hakuOid Haun tunniste
   */
  CompletableFuture<Map<String, HakukohteenValintatiedot>> valintatiedotHakukohteittain(
      String hakuOid);
}
