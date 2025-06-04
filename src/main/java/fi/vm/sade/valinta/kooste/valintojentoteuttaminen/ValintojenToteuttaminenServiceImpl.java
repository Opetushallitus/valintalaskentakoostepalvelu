package fi.vm.sade.valinta.kooste.valintojentoteuttaminen;

import fi.vm.sade.valinta.kooste.dto.HakukohdeKoosteTieto;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametriDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valintalaskenta.domain.valinta.HakukohdeLaskentaTehty;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ValintojenToteuttaminenServiceImpl implements ValintojenToteuttaminenService {
  private final ValintaperusteetAsyncResource valintaperusteetAsyncResource;
  private final ValintalaskentaAsyncResource valintalaskentaAsyncResource;
  private final OhjausparametritAsyncResource ohjausparametritAsyncResource;

  @Autowired
  public ValintojenToteuttaminenServiceImpl(
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      ValintalaskentaAsyncResource valintalaskentaAsyncResource,
      @Qualifier("OhjausparametritAsyncResource")
          OhjausparametritAsyncResource ohjausparametritAsyncResource) {
    this.valintaperusteetAsyncResource = valintaperusteetAsyncResource;
    this.valintalaskentaAsyncResource = valintalaskentaAsyncResource;
    this.ohjausparametritAsyncResource = ohjausparametritAsyncResource;
  }

  @Override
  public CompletableFuture<Map<String, HakukohteenValintatiedot>> valintatiedotHakukohteittain(
      String hakuOid) {
    CompletableFuture<List<HakukohdeLaskentaTehty>> laskennatF =
        valintalaskentaAsyncResource.hakukohteidenLaskennanTila(hakuOid);
    CompletableFuture<List<HakukohdeKoosteTieto>> hakukohdeTiedotF =
        valintaperusteetAsyncResource.haunHakukohdeTiedot(hakuOid);
    CompletableFuture<ParametritDTO> ohjausparametritF =
        ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid);
    return CompletableFuture.allOf(laskennatF, hakukohdeTiedotF, ohjausparametritF)
        .thenApply(
            x -> {
              List<HakukohdeLaskentaTehty> laskennat = laskennatF.join();
              List<HakukohdeKoosteTieto> hakukohdeTiedot = hakukohdeTiedotF.join();
              ParametriDTO vstpParametri = ohjausparametritF.join().getPH_VSTP();
              Date haunVarasijatayttoPaattyy =
                  vstpParametri == null ? null : vstpParametri.getDate();
              Map<String, HakukohteenValintatiedot> result = new HashMap<>();

              hakukohdeTiedot.forEach(
                  hakukohdetieto -> {
                    result.putIfAbsent(
                        hakukohdetieto.hakukohdeOid,
                        new HakukohteenValintatiedot(hakukohdetieto.hakukohdeOid));
                    HakukohteenValintatiedot valintatieto = result.get(hakukohdetieto.hakukohdeOid);
                    valintatieto.hasValintakoe = hakukohdetieto.hasValintakoe;
                    valintatieto.varasijatayttoPaattyy =
                        ObjectUtils.min(
                            haunVarasijatayttoPaattyy, hakukohdetieto.varasijatayttoPaattyy);
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
