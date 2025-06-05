package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeViiteDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HaunHakukohdeTulosTiedot;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.HakukohdeTulosTiedot;
import fi.vm.sade.valintalaskenta.domain.valinta.HakukohdeLaskentaTehty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ValintojenToteuttaminenServiceImpl implements ValintojenToteuttaminenService {
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
  private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;

  @Autowired
  public ValintojenToteuttaminenServiceImpl(
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      ValintalaskentaAsyncResource valintalaskentaAsyncResource,
      ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource) {
    this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
    this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
    this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
  }

  @Override
  public CompletableFuture<Map<String, HakukohteenValintatiedot>> valintatiedotHakukohteittain(
      String hakuOid) {
    CompletableFuture<List<HakukohdeLaskentaTehty>> laskennatF =
        valintalaskentaAsyncResource.hakukohteidenLaskennanTila(hakuOid);
    CompletableFuture<List<HakukohdeViiteDTO>> hakukohteetF =
        valintaperusteetAsyncResource.haunHakukohteetF(hakuOid, true);
    CompletableFuture<HaunHakukohdeTulosTiedot> tulosF =
        valintaTulosServiceAsyncResource.getHaunHakukohdeTiedot(hakuOid);
    return CompletableFuture.allOf(laskennatF, hakukohteetF, tulosF)
        .thenApply(
            x -> {
              List<HakukohdeLaskentaTehty> laskennat = laskennatF.join();
              List<HakukohdeViiteDTO> hakukohteet = hakukohteetF.join();
              Set<HakukohdeTulosTiedot> tulokset = tulosF.join().hakukohteet;
              Stream<String> foundHakukohdeOids =
                  Stream.concat(
                          Stream.concat(
                              laskennat.stream().map(l -> l.hakukohdeOid),
                              hakukohteet.stream().map(HakukohdeViiteDTO::getOid)),
                          tulokset.stream().map(t -> t.oid))
                      .distinct();
              return foundHakukohdeOids
                  .map(
                      hk -> {
                        boolean laskettu =
                            laskennat.stream()
                                .anyMatch(l -> l.hakukohdeOid.equals(hk) && l.lastModified != null);
                        boolean hasValintakoe =
                            hakukohteet.stream().anyMatch(h -> h.getOid().equals(hk));
                        boolean sijoittelematta =
                            tulokset.stream().anyMatch(t -> t.oid.equals(hk) && t.sijoittelematta);
                        boolean julkaisematta =
                            tulokset.stream().anyMatch(t -> t.oid.equals(hk) && t.julkaisematta);
                        return new HakukohteenValintatiedot(
                            hk, hasValintakoe, laskettu, sijoittelematta, julkaisematta);
                      })
                  .collect(Collectors.toMap(v -> v.hakukohdeOid, v -> v));
            });
  }
}
