package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ValintojenToteuttaminenService {
  CompletableFuture<Map<String, HakukohteenValintatiedot>> valintatiedotHakukohteittain(
      String hakuOid);
}
