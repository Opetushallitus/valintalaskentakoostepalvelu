package fi.vm.sade.valinta.kooste.pistesyotto.service;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.TuontiErrorDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoExcel;
import fi.vm.sade.valinta.kooste.pistesyotto.excel.PistesyottoRivi;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import io.reactivex.Observable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PistesyottoTuontiService extends AbstractPistesyottoKoosteService {
  private static final Logger LOG = LoggerFactory.getLogger(PistesyottoTuontiService.class);

  @Autowired
  public PistesyottoTuontiService(
      ValintalaskentaValintakoeAsyncResource valintakoeResource,
      ValintaperusteetAsyncResource valintaperusteetResource,
      ValintapisteAsyncResource valintapisteAsyncResource,
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      OhjausparametritAsyncResource ohjausparametritAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
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
  }

  private void logPistesyotonTuontiEpaonnistui(Throwable t) {
    LOG.error(
        HttpExceptionWithResponse.appendWrappedResponse("Pistesyötön tuonti epäonnistui", t), t);
  }

  private List<TuontiErrorDTO> getPistesyottoExcelVirheet(
      PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri,
      Map<String, HakemusWrapper> hakemukset) {
    return pistesyottoTuontiAdapteri.getRivit().stream()
        .flatMap(
            rivi -> {
              HakemusWrapper hakemus = hakemukset.get(rivi.getOid());
              String nimi = hakemus.getSukunimi() + ", " + hakemus.getEtunimet();
              if (!Optional.ofNullable(rivi.getNimi()).orElse("").equals(nimi)) {
                String virheIlmoitus =
                    String.format("nimet eivät täsmää: %s != %s", rivi.getNimi(), nimi);
                return Stream.of(new TuontiErrorDTO(rivi.getOid(), rivi.getNimi(), virheIlmoitus));
              }
              if (!rivi.isValidi()) {
                LOG.warn("Rivi on muuttunut mutta viallinen joten ilmoitetaan virheestä!");
                String rivinVirheilmoitukset =
                    rivi.getArvot().stream()
                        .filter(pistesyottoArvo -> !pistesyottoArvo.isValidi())
                        .map(
                            virhellinenArvo ->
                                String.format(
                                    "virheellinen arvo '%s' kohdassa %s",
                                    virhellinenArvo.getArvo(), virhellinenArvo.getTunniste()))
                        .collect(Collectors.joining(", "));
                return Stream.of(
                    new TuontiErrorDTO(rivi.getOid(), rivi.getNimi(), rivinVirheilmoitukset));
              }
              return Stream.empty();
            })
        .collect(Collectors.toList());
  }

  public Observable<Set<TuontiErrorDTO>> tuo(
      AuditSession auditSession,
      String hakuOid,
      String hakukohdeOid,
      DokumenttiProsessi prosessi,
      InputStream stream) {
    PistesyottoDataRiviListAdapter pistesyottoTuontiAdapteri = new PistesyottoDataRiviListAdapter();
    Observable<Set<TuontiErrorDTO>> errors =
        muodostaPistesyottoExcel(
                hakuOid,
                hakukohdeOid,
                auditSession,
                prosessi,
                Collections.singleton(pistesyottoTuontiAdapteri))
            .flatMap(
                p -> {
                  PistesyottoExcel pistesyottoExcel = p.getLeft();
                  Map<String, ApplicationAdditionalDataDTO> pistetiedot = p.getMiddle();
                  Map<String, HakemusWrapper> hakemukset = p.getRight();
                  try {
                    pistesyottoExcel.getExcel().tuoXlsx(stream);
                  } catch (IOException e) {
                    return Observable.error(e);
                  }
                  List<TuontiErrorDTO> virheet =
                      getPistesyottoExcelVirheet(pistesyottoTuontiAdapteri, hakemukset);
                  if (!virheet.isEmpty()) {
                    return Observable.error(new PistesyotonTuontivirhe(virheet));
                  }
                  Date valmistuminen = new Date();
                  Map<String, List<SingleKielikoeTulos>> uudetKielikoetulokset = new HashMap<>();
                  Optional<String> ifUnmodifiedSince = pistesyottoExcel.getAikaleima();
                  List<ApplicationAdditionalDataDTO> uudetPistetiedot =
                      pistesyottoTuontiAdapteri.getRivit().stream()
                          .filter(PistesyottoRivi::isValidi)
                          .flatMap(
                              rivi -> {
                                final String hakemusOid = rivi.getOid();
                                ApplicationAdditionalDataDTO additionalData =
                                    pistetiedot.get(hakemusOid);
                                Map<String, String> newPistetiedot =
                                    rivi.asAdditionalData(
                                        valintakoetunniste ->
                                            pistesyottoExcel.onkoHakijaOsallistujaValintakokeeseen(
                                                hakemusOid, valintakoetunniste));
                                siirraKielikoepistetiedotKielikoetulosMapiin(
                                    valmistuminen,
                                    uudetKielikoetulokset,
                                    hakemusOid,
                                    newPistetiedot);
                                additionalData.setAdditionalData(newPistetiedot);
                                return Stream.of(additionalData);
                              })
                          .filter(Objects::nonNull)
                          .filter(a -> !a.getAdditionalData().isEmpty())
                          .collect(Collectors.toList());

                  if (uudetPistetiedot.isEmpty()) {
                    LOG.info(
                        "Pistesyötössä hakukohteeseen {} ei yhtäkään muuttunutta tietoa tallennettavaksi",
                        hakukohdeOid);
                    return Observable.just(Collections.emptySet());
                  } else {
                    LOG.info(
                        "Pistesyötössä hakukohteeseen {} muuttunutta {} tietoa tallennettavaksi",
                        hakukohdeOid,
                        uudetPistetiedot.size());
                    Observable<Set<String>> failedPisteet =
                        Observable.fromFuture(
                            tallennaKoostetutPistetiedot(
                                hakuOid,
                                hakukohdeOid,
                                ifUnmodifiedSince,
                                uudetPistetiedot,
                                uudetKielikoetulokset,
                                ValintaperusteetOperation.PISTETIEDOT_TUONTI_EXCEL,
                                auditSession));
                    return failedPisteet.map(
                        ids ->
                            uudetPistetiedot.stream()
                                .filter(u -> ids.contains(u.getOid()))
                                .map(
                                    dto ->
                                        new TuontiErrorDTO(
                                            dto.getOid(),
                                            dto.getFirstNames() + " " + dto.getLastName(),
                                            "Yritettiin kirjoittaa yli uudempaa pistetietoa"))
                                .collect(Collectors.toSet()));
                  }
                });

    errors =
        errors.doOnNext(
            failedIds -> {
              prosessi.inkrementoiTehtyjaToita();
              prosessi.setDokumenttiId("valmis");
              if (failedIds != null && !failedIds.isEmpty()) {
                LOG.info(
                    "Pistesyöttö epäonnistui hakemuksille: {}", StringUtils.join(failedIds, ","));
              }
            });

    errors =
        errors.doOnError(
            t -> {
              logPistesyotonTuontiEpaonnistui(t);
              prosessi
                  .getPoikkeukset()
                  .add(new Poikkeus(Poikkeus.KOOSTEPALVELU, "Pistesyötön tuonti:", t.getMessage()));
            });
    return errors;
  }
}
