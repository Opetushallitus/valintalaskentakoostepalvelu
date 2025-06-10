package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.util.PoikkeusKasittelijaSovitin;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PistesyottoVientiService extends AbstractPistesyottoKoosteService {
  private static final Logger LOG = LoggerFactory.getLogger(PistesyottoVientiService.class);
  private final DokumenttiAsyncResource dokumenttiAsyncResource;

  @Autowired
  public PistesyottoVientiService(
      ValintapisteAsyncResource valintapisteAsyncResource,
      ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource,
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      OhjausparametritAsyncResource ohjausparametritAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      DokumenttiAsyncResource dokumenttiAsyncResource,
      SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource) {
    super(
        applicationAsyncResource,
        ataruAsyncResource,
        valintapisteAsyncResource,
        suoritusrekisteriAsyncResource,
        tarjontaAsyncResource,
        ohjausparametritAsyncResource,
        organisaatioAsyncResource,
        valintaperusteetAsyncResource,
        valintalaskentaValintakoeAsyncResource);
    this.dokumenttiAsyncResource = dokumenttiAsyncResource;
  }

  public void vie(
      String hakuOid, String hakukohdeOid, AuditSession auditSession, DokumenttiProsessi prosessi) {
    PoikkeusKasittelijaSovitin poikkeuskasittelija =
        new PoikkeusKasittelijaSovitin(
            poikkeus -> {
              LOG.error(
                  String.format(
                      "Käyttäjän %s tekemässä haun %s hakukohteen %s pistesyötön viennissä tapahtui poikkeus:",
                      auditSession.getPersonOid(), hakuOid, hakukohdeOid),
                  poikkeus);
              prosessi
                  .getPoikkeukset()
                  .add(
                      new Poikkeus(
                          Poikkeus.KOOSTEPALVELU, "Pistesyötön vienti", poikkeus.getMessage()));
            });
    prosessi.inkrementoiKokonaistyota();
    muodostaPistesyottoExcel(hakuOid, hakukohdeOid, auditSession, prosessi, Collections.emptyList())
        .timeout(2, TimeUnit.MINUTES)
        .flatMap(
            p -> {
              PistesyottoExcel pistesyottoExcel = p.getLeft();
              String id = UUID.randomUUID().toString();
              Observable<ResponseEntity<Void>> tallennus =
                  dokumenttiAsyncResource.tallenna(
                      id,
                      "pistesyotto.xlsx",
                      defaultExpirationDate().getTime(),
                      prosessi.getTags(),
                      "application/octet-stream",
                      pistesyottoExcel.getExcel().vieXlsx());
              return Observable.just(id).zipWith(tallennus, Pair::of);
            })
        .subscribe(
            idWithResponse -> {
              prosessi.inkrementoiTehtyjaToita();
              prosessi.setDokumenttiId(idWithResponse.getLeft());
            },
            poikkeuskasittelija);
  }

  protected Date defaultExpirationDate() {
    return DateTime.now().plusHours(168).toDate();
  }
}
