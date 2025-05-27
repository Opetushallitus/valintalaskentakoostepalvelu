package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ValintojenToteuttaminenServiceImpl implements ValintojenToteuttaminenService {
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  private final OhjausparametritAsyncResource ohjausparametritAsyncResource;

  @Autowired
  public ValintojenToteuttaminenServiceImpl(
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      @Qualifier("OhjausparametritAsyncResource")
          OhjausparametritAsyncResource ohjausparametritAsyncResource) {
    this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
    this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
  }

  @Override
  public CompletableFuture<Map<String, HakukohteenValintatiedot>> valintatiedotHakukohteittain(
      String hakuOid) {
    CompletableFuture<ParametritDTO> ohausparametrit =
        ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid);

    return valintaperusteetAsyncResource
        .haunHakukohdeTiedot(hakuOid)
        .thenApply(
            valintatiedot -> {
              Date varasijatayttoPaattyy = ohausparametrit.join().getPH_VSTP().getDate();
              return valintatiedot.stream()
                  .collect(
                      Collectors.toMap(
                          ht -> ht.hakukohdeOid,
                          ht -> {
                            return new HakukohteenValintatiedot(
                                ht.hakukohdeOid,
                                ht.hasValintakoe,
                                ht.varasijatayttoPaattyy == null
                                    ? varasijatayttoPaattyy
                                    : ht.varasijatayttoPaattyy);
                          }));
            });
  }
}
