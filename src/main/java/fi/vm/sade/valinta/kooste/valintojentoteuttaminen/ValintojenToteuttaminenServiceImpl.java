package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.dto.HakukohdeLaskentaTehty;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValintojenToteuttaminenServiceImpl implements ValintojenToteuttaminenService {
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;

  @Autowired
  public ValintojenToteuttaminenServiceImpl(
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      ValintalaskentaAsyncResource valintalaskentaAsyncResource) {
    this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
    this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
  }

  @Override
  public CompletableFuture<Map<String, HakukohteenValintatiedot>> valintatiedotHakukohteittain(
      String hakuOid) {
    CompletableFuture<List<HakukohdeLaskentaTehty>> laskennatF =
        valintalaskentaAsyncResource.hakukohteidenLaskennanTila(hakuOid);
    CompletableFuture<List<HakukohdeViiteDTO>> hakukohteetF =
        valintaperusteetAsyncResource.haunHakukohteetF(hakuOid, true);
    return CompletableFuture.allOf(laskennatF, hakukohteetF)
        .thenApply(
            x -> {
              List<HakukohdeLaskentaTehty> laskennat = laskennatF.join();
              List<HakukohdeViiteDTO> hakukohteet = hakukohteetF.join();
              Stream<String> foundHakukohdeOids =
                  Stream.concat(
                          laskennat.stream().map(l -> l.hakukohdeOid),
                          hakukohteet.stream().map(HakukohdeViiteDTO::getOid))
                      .distinct();
              return foundHakukohdeOids
                  .map(
                      hk -> {
                        boolean laskettu =
                            laskennat.stream()
                                .anyMatch(l -> l.hakukohdeOid.equals(hk) && l.lastModified != null);
                        boolean hasValintakoe =
                            hakukohteet.stream().anyMatch(h -> h.getOid().equals(hk));
                        return new HakukohteenValintatiedot(hk, hasValintakoe, laskettu);
                      })
                  .collect(Collectors.toMap(v -> v.hakukohdeOid, v -> v));
            });
  }
}
