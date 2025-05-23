package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteCreateDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValintojenToteuttaminenServiceImpl implements ValintojenToteuttaminenService {
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;

  @Autowired
  public ValintojenToteuttaminenServiceImpl(
      ValintaperusteetAsyncResource valintaperusteetAsyncResource) {
    this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
  }

  @Override
  public CompletableFuture<Map<String, HakukohteenValintatiedot>> valintatiedotHakukohteittain(
      String hakuOid) {
    return valintaperusteetAsyncResource
        .haunHakukohteetF(hakuOid, true)
        .thenApply(
            viitteet ->
                viitteet.stream()
                    .collect(
                        Collectors.toMap(
                            HakukohdeViiteCreateDTO::getOid,
                            viite -> {
                              return new HakukohteenValintatiedot(true);
                            })));
  }
}
