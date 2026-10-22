package fi.vm.sade.valinta.kooste.pistesyotto.service;

import static java.util.Collections.singletonList;

import fi.vm.sade.service.valintaperusteet.dto.ValintaperusteDTO;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.organisaatio.OrganisaatioAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintalaskenta.ValintalaskentaValintakoeAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintaperusteet.ValintaperusteetAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemuksenKoetulosYhteenveto;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HenkiloValilehtiDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.PistesyottoValilehtiDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.TuontiErrorDTO;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valintalaskenta.domain.dto.valintakoe.ValintakoeOsallistuminenDTO;
import io.reactivex.Observable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PistesyottoKoosteService extends AbstractPistesyottoKoosteService {
  private static final Logger LOG = LoggerFactory.getLogger(PistesyottoKoosteService.class);

  @Autowired
  public PistesyottoKoosteService(
      ApplicationAsyncResource applicationAsyncResource,
      AtaruAsyncResource ataruAsyncResource,
      ValintapisteAsyncResource valintapisteAsyncResource,
      TarjontaAsyncResource tarjontaAsyncResource,
      OrganisaatioAsyncResource organisaatioAsyncResource,
      ValintaperusteetAsyncResource valintaperusteetAsyncResource,
      ValintalaskentaValintakoeAsyncResource valintalaskentaValintakoeAsyncResource) {
    super(
        applicationAsyncResource,
        ataruAsyncResource,
        valintapisteAsyncResource,
        tarjontaAsyncResource,
        organisaatioAsyncResource,
        valintaperusteetAsyncResource,
        valintalaskentaValintakoeAsyncResource);
  }

  public CompletableFuture<PistesyottoValilehtiDTO> koostaOsallistujienPistetiedot(
      String hakuOid, String hakukohdeOid, AuditSession auditSession) {
    try {
      CompletableFuture<PisteetWithLastModified> valintapisteetF =
          valintapisteAsyncResource.getValintapisteet(hakuOid, hakukohdeOid, auditSession);
      CompletableFuture<Map<String, ValintakoeOsallistuminenDTO>> osallistuminenByHakemusF =
          valintalaskentaValintakoeAsyncResource
              .haeHakutoiveelle(hakukohdeOid)
              .thenApplyAsync(
                  vs -> vs.stream().collect(Collectors.toMap(v -> v.getHakemusOid(), v -> v)));
      CompletableFuture<List<ValintaperusteDTO>> valintaperusteetF =
          valintaperusteetAsyncResource.findAvaimet(hakukohdeOid);
      return CompletableFuture.allOf(valintapisteetF, valintaperusteetF, osallistuminenByHakemusF)
          .thenApplyAsync(
              x -> {
                PisteetWithLastModified valintapisteet = valintapisteetF.join();
                List<ValintaperusteDTO> valintaperusteet = valintaperusteetF.join();
                Map<String, ValintakoeOsallistuminenDTO> valintakokeet =
                    osallistuminenByHakemusF.join();
                return new PistesyottoValilehtiDTO(
                    valintapisteet.lastModified.orElse(null),
                    valintapisteet.valintapisteet.stream()
                        .map(
                            vps ->
                                new HakemuksenKoetulosYhteenveto(
                                    vps,
                                    Pair.of(hakukohdeOid, valintaperusteet),
                                    valintakokeet.get(vps.getHakemusOID())))
                        .collect(Collectors.toList()));
              });
    } catch (Exception e) {
      LOG.error(
          String.format(
              "Ongelma koostettaessa haun %s kohteen %s pistetietoja", hakuOid, hakukohdeOid),
          e);
      return CompletableFuture.failedFuture(e);
    }
  }

  private Observable<List<Pair<String, List<ValintaperusteDTO>>>> getValintaperusteet(
      HakemusWrapper hakemus) {
    return Observable.merge(
            hakemus.getHakutoiveOids().stream()
                .map(
                    hakukohdeOid ->
                        Observable.fromFuture(
                                valintaperusteetAsyncResource.findAvaimet(hakukohdeOid))
                            .map(valintaperusteet -> Pair.of(hakukohdeOid, valintaperusteet)))
                .collect(Collectors.toList()))
        .toList()
        .toObservable();
  }

  private Observable<HakemusWrapper> getHakemus(String hakemusOid) {
    return Observable.fromFuture(
            ataruAsyncResource.getApplicationsByOids(Collections.singletonList(hakemusOid)))
        .flatMap(
            hakemukset -> {
              if (hakemukset.isEmpty()) {
                return applicationAsyncResource.getApplication(hakemusOid);
              } else {
                return Observable.just(hakemukset.iterator().next());
              }
            });
  }

  public Observable<HenkiloValilehtiDTO> koostaOsallistujanPistetiedot(
      String hakemusOid, AuditSession auditSession) {
    return getHakemus(hakemusOid)
        .flatMap(
            hakemus ->
                Observable.zip(
                    getValintaperusteet(hakemus),
                    Observable.fromFuture(
                        valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                            Collections.singletonList(hakemusOid), auditSession)),
                    Observable.fromFuture(
                        valintalaskentaValintakoeAsyncResource.haeHakemukselle(hakemusOid)),
                    (valintaperusteetByHakukohdeOid, valintapisteet, valintakoeOsallistuminen) ->
                        new HenkiloValilehtiDTO(
                            valintapisteet.lastModified.orElse(null),
                            valintaperusteetByHakukohdeOid.stream()
                                .collect(
                                    Collectors.toMap(
                                        Pair::getLeft,
                                        valintaperusteet ->
                                            new HakemuksenKoetulosYhteenveto(
                                                valintapisteet.valintapisteet.iterator().next(),
                                                valintaperusteet,
                                                valintakoeOsallistuminen))))));
  }

  public Observable<Set<TuontiErrorDTO>> tallennaKoostetutPistetiedotHakemukselle(
      ApplicationAdditionalDataDTO pistetietoDTO,
      Optional<String> ifUnmodifiedSince,
      AuditSession auditSession) {
    return Observable.fromFuture(
            valintapisteAsyncResource.putValintapisteet(
                ifUnmodifiedSince,
                singletonList(
                    new Valintapisteet(Pair.of(auditSession.getPersonOid(), pistetietoDTO))),
                auditSession))
        .map(
            hakemusOids ->
                hakemusOids.stream()
                    .map(
                        hakemusOid ->
                            new TuontiErrorDTO(
                                hakemusOid,
                                pistetietoDTO.getFirstNames() + " " + pistetietoDTO.getLastName(),
                                "Yritettiin kirjoittaa yli uudempia pistetietoja"))
                    .collect(Collectors.toSet()));
  }

  public CompletableFuture<Set<TuontiErrorDTO>> tallennaKoostetutPistetiedot(
      String hakuOid,
      String hakukohdeOid,
      Optional<String> ifUnmodifiedSince,
      List<ApplicationAdditionalDataDTO> pistetietoDTOs,
      AuditSession auditSession) {
    List<ApplicationAdditionalDataDTO> pistetiedotHakemukselle =
        pistetietoDTOs.stream()
            .filter(a -> !a.getAdditionalData().isEmpty())
            .collect(Collectors.toList());
    try {
      return tallennaKoostetutPistetiedot(
              hakuOid,
              hakukohdeOid,
              ifUnmodifiedSince,
              pistetiedotHakemukselle,
              ValintaperusteetOperation.PISTETIEDOT_KAYTTOLIITTYMA,
              auditSession)
          .thenApplyAsync(
              hakemusOids ->
                  pistetiedotHakemukselle.stream()
                      .filter(dto -> hakemusOids.contains(dto.getOid()))
                      .map(
                          dto ->
                              new TuontiErrorDTO(
                                  dto.getOid(),
                                  dto.getFirstNames() + " " + dto.getLastName(),
                                  "Yritettiin kirjoittaa yli uudempia pistetietoja"))
                      .collect(Collectors.toSet()));
    } catch (Exception e) {
      LOG.error(
          String.format(
              "Ongelma käsiteltäessä pistetietoja haun %s kohteelle %s , käyttäjä %s ",
              hakuOid, hakukohdeOid, auditSession.getPersonOid()),
          e);
      return CompletableFuture.failedFuture(e);
    }
  }
}
