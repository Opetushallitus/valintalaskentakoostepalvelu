package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import fi.vm.sade.service.valintaperusteet.dto.HakukohdeKoosteTietoDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valintalaskenta.domain.valinta.HakukohdeLaskentaTehty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    CompletableFuture<List<HakukohdeKoosteTietoDTO>> hakukohdeTiedotF =
        valintaperusteetAsyncResource.haunHakukohdeTiedot(hakuOid);
    return CompletableFuture.allOf(laskennatF, hakukohdeTiedotF)
        .thenApply(
            x -> {
              List<HakukohdeLaskentaTehty> laskennat = laskennatF.join();
              List<HakukohdeKoosteTietoDTO> hakukohdeTiedot = hakukohdeTiedotF.join();
              Map<String, HakukohteenValintatiedot> result = new HashMap<>();
              hakukohdeTiedot.forEach(
                  hakukohdetieto -> {
                    result.putIfAbsent(
                        hakukohdetieto.hakukohdeOid,
                        new HakukohteenValintatiedot(hakukohdetieto.hakukohdeOid));
                    HakukohteenValintatiedot valintatieto = result.get(hakukohdetieto.hakukohdeOid);
                    valintatieto.hasValintakoe = hakukohdetieto.hasValintakoe;
                    valintatieto.varasijatayttoPaattyy = hakukohdetieto.varasijatayttoPaattyy;
                  });

              laskennat.forEach(
                  laskenta -> {
                    result.putIfAbsent(
                        laskenta.hakukohdeOid, new HakukohteenValintatiedot(laskenta.hakukohdeOid));
                    HakukohteenValintatiedot valintatieto = result.get(laskenta.hakukohdeOid);
                    valintatieto.laskettu = valintatieto.laskettu || laskenta.lastModified != null;
                  });
              return result;
            });
  }
}
