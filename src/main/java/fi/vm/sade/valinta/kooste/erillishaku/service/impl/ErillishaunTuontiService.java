package fi.vm.sade.valinta.kooste.erillishaku.service.impl;

import static com.codepoetics.protonpack.StreamUtils.zip;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_HAKEMUSPALVELUN_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource.POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.HenkilonRivinPaattelyEpaonnistuiException;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.ainoastaanHakemuksenTilaPaivitys;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.autoTaytto;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.createHakemusprototyyppi;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.isKesken;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.toErillishaunHakijaDTO;
import static fi.vm.sade.valinta.kooste.erillishaku.service.impl.ErillishaunTuontiHelper.toPoistettavaErillishaunHakijaDTO;
import static io.reactivex.schedulers.Schedulers.newThread;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.MINUTES;

import fi.vm.sade.auditlog.Changes;
import fi.vm.sade.sijoittelu.domain.dto.ErillishaunHakijaDTO;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.excel.ErillishakuRivi;
import fi.vm.sade.valinta.kooste.erillishaku.excel.Maksuvelvollisuus;
import fi.vm.sade.valinta.kooste.erillishaku.resource.ErillishakuResource;
import fi.vm.sade.valinta.kooste.excel.ExcelValidointiPoikkeus;
import fi.vm.sade.valinta.kooste.exception.ErillishaunDataException;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruHakemusPrototyyppi;
import fi.vm.sade.valinta.kooste.external.resource.ataru.dto.AtaruSyntheticApplicationResponse;
import fi.vm.sade.valinta.kooste.external.resource.koodisto.KoodistoCachedAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.oppijanumerorekisteri.OppijanumerorekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.LukuvuosimaksuMuutos;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Maksuntila;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.Valinnantulos;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.valvomo.dto.Poikkeus;
import fi.vm.sade.valinta.kooste.valvomo.dto.Tunniste;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.KirjeProsessi;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.sharedutils.ValintaResource;
import fi.vm.sade.valinta.sharedutils.ValintaperusteetOperation;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ErillishaunTuontiService extends ErillishaunTuontiValidator {
  private static final Logger LOG = LoggerFactory.getLogger(ErillishaunTuontiService.class);

  private final AtaruAsyncResource ataruAsyncResource;
  private final OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource;
  private final ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource;
  private final Scheduler scheduler;
  private KoodistoCachedAsyncResource koodistoCachedAsyncResource;
  private final TarjontaAsyncResource hakuV1AsyncResource;

  public ErillishaunTuontiService(
      AtaruAsyncResource ataruAsyncResource,
      OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource,
      ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      TarjontaAsyncResource hakuV1AsyncResource,
      Scheduler scheduler) {
    super(koodistoCachedAsyncResource);
    this.ataruAsyncResource = ataruAsyncResource;
    this.oppijanumerorekisteriAsyncResource = oppijanumerorekisteriAsyncResource;
    this.valintaTulosServiceAsyncResource = valintaTulosServiceAsyncResource;
    this.koodistoCachedAsyncResource = koodistoCachedAsyncResource;
    this.hakuV1AsyncResource = hakuV1AsyncResource;
    this.scheduler = scheduler;
  }

  @Autowired
  public ErillishaunTuontiService(
      AtaruAsyncResource ataruAsyncResource,
      OppijanumerorekisteriAsyncResource oppijanumerorekisteriAsyncResource,
      ValintaTulosServiceAsyncResource valintaTulosServiceAsyncResource,
      KoodistoCachedAsyncResource koodistoCachedAsyncResource,
      TarjontaAsyncResource hakuV1AsyncResource) {
    this(
        ataruAsyncResource,
        oppijanumerorekisteriAsyncResource,
        valintaTulosServiceAsyncResource,
        koodistoCachedAsyncResource,
        hakuV1AsyncResource,
        newThread());
  }

  public void tuoExcelistä(
      AuditSession auditSession,
      KirjeProsessi prosessi,
      ErillishakuDTO erillishaku,
      InputStream data) {
    tuoData(
        auditSession,
        prosessi,
        erillishaku,
        (haku) ->
            new ImportedErillisHakuExcel(haku.getHakutyyppi(), data, koodistoCachedAsyncResource),
        true);
  }

  public void tuoJson(
      AuditSession auditSession,
      KirjeProsessi prosessi,
      ErillishakuDTO erillishaku,
      List<ErillishakuRivi> erillishakuRivit,
      final boolean saveApplications) {
    tuoData(
        auditSession,
        prosessi,
        erillishaku,
        (haku) -> new ImportedErillisHakuExcel(erillishakuRivit),
        saveApplications);
  }

  private void tuoData(
      AuditSession auditSession,
      KirjeProsessi prosessi,
      ErillishakuDTO erillishaku,
      Function<ErillishakuDTO, ImportedErillisHakuExcel> importer,
      final boolean saveApplications) {
    Observable.just(erillishaku)
        .subscribeOn(scheduler)
        .subscribe(
            haku -> {
              final ImportedErillisHakuExcel erillishakuExcel;
              try {
                erillishakuExcel = importer.apply(haku);
                tuoHakijatJaLuoHakemukset(
                    auditSession, prosessi, erillishakuExcel, saveApplications, haku);
              } catch (ErillishaunDataException dataException) {
                LOG.warn("excel ei validi:", dataException);
                prosessi.keskeyta(
                    Poikkeus.koostepalvelupoikkeus(
                        ErillishakuResource.POIKKEUS_VIALLINEN_DATAJOUKKO,
                        dataException.getPoikkeusRivit().stream()
                            .map(
                                p ->
                                    new Tunniste(
                                        "Rivi " + p.getIndeksi() + ": " + p.getSelite(),
                                        ErillishakuResource.RIVIN_TUNNISTE_KAYTTOLIITTYMAAN))
                            .collect(Collectors.toList())));
              } catch (ExcelValidointiPoikkeus validointiPoikkeus) {
                LOG.warn("excel ei validi", validointiPoikkeus);
                prosessi.keskeyta(validointiPoikkeus.getMessage());
              } catch (Exception e) {
                errorLogIncludingHttpResponse("unexpected tuoData exception!", e);
                prosessi.keskeyta();
              }
            },
            poikkeus -> {
              errorLogIncludingHttpResponse("Erillishaun tuonti keskeytyi virheeseen", poikkeus);
              prosessi.keskeyta();
            },
            () -> LOG.info("Tuonti lopetettiin"));
  }

  private void tuoHakijatJaLuoHakemukset(
      final AuditSession auditSession,
      final KirjeProsessi prosessi,
      final ImportedErillisHakuExcel erillishakuExcel,
      final boolean saveApplications,
      final ErillishakuDTO haku) {
    final String username = auditSession.getPersonOid();

    LOG.info("Aloitetaan tuonti. Rivit=" + erillishakuExcel.rivit.size());
    final List<ErillishakuRivi> rivit = autoTaytto(erillishakuExcel.rivit);
    validoiRivit(prosessi, haku, rivit, saveApplications);

    List<ErillishakuRivi> lisattavatTaiKeskeneraiset =
        kasitteleHakemukset(
            prosessi,
            haku,
            rivit.stream().filter(rivi -> !rivi.isPoistetaankoRivi()).collect(Collectors.toList()),
            saveApplications);

    LOG.info(
        "Viedaan hakijoita ({}kpl) jonoon {}",
        lisattavatTaiKeskeneraiset.size(),
        haku.getValintatapajonoOid());

    List<ErillishakuRivi> poistettavat =
        rivit.stream().filter(rivi -> rivi.isPoistetaankoRivi()).collect(Collectors.toList());

    Observable<Boolean> passivointi =
        Observable.fromFuture(hakuV1AsyncResource.haeHaku(haku.getHakuOid()))
            .switchMap(
                hk -> {
                  final boolean hakuAppHaku = StringUtils.isBlank(hk.ataruLomakeAvain);
                  if (hakuAppHaku) {
                    return passivoiHakemukset(poistettavat);
                  } else {
                    return Observable.just(true);
                  }
                });

    Observable<String> maksuntilojenTallennus =
        maksutilojenTallennus(auditSession, haku, lisattavatTaiKeskeneraiset);

    Observable<List<Poikkeus>> vastaanotonJaValinnantuloksenTallennus =
        vastaanotonJaValinnantuloksenTallennus(
            auditSession, haku, lisattavatTaiKeskeneraiset, poistettavat);

    Observable.combineLatest(
            passivointi,
            maksuntilojenTallennus,
            vastaanotonJaValinnantuloksenTallennus,
            (passivointiOnnistui, m, poikkeukset) -> poikkeukset)
        .subscribe(
            poikkeukset -> {
              Set<String> epaonnistuneet =
                  poikkeukset.stream()
                      .flatMap(p -> p.getTunnisteet().stream())
                      .filter(t -> t.getTyyppi().equals(Poikkeus.HAKEMUSOID))
                      .map(Tunniste::getTunniste)
                      .collect(Collectors.toSet());
              Stream<ErillishaunHakijaDTO> lokitettavat =
                  Stream.concat(
                      lisattavatTaiKeskeneraiset.stream()
                          .map(rivi -> toErillishaunHakijaDTO(haku, rivi)),
                      poistettavat.stream()
                          .map(rivi -> toPoistettavaErillishaunHakijaDTO(haku, rivi)));
              lokitettavat
                  .filter(h -> !epaonnistuneet.contains(h.getHakemusOid()))
                  .forEach(
                      hakijaDTO -> {
                        Map<String, String> additionalAuditInfo = new HashMap<>();
                        additionalAuditInfo.put("hakuOid", haku.getHakuOid());
                        additionalAuditInfo.put("hakukohdeOid", haku.getHakukohdeOid());
                        additionalAuditInfo.put("valintatapajonoOid", haku.getValintatapajonoOid());
                        if (hakijaDTO.getPoistetaankoTulokset()) {
                          AuditLog.log(
                              KoosteAudit.AUDIT,
                              auditSession.asAuditUser(),
                              ValintaperusteetOperation.ERILLISHAKU_TUONTI_HAKIJA_POISTO,
                              ValintaResource.ERILLISHAUNTUONTISERVICE,
                              hakijaDTO.getHakijaOid(),
                              Changes.deleteDto(hakijaDTO),
                              additionalAuditInfo);
                        } else {
                          AuditLog.log(
                              KoosteAudit.AUDIT,
                              auditSession.asAuditUser(),
                              ValintaperusteetOperation.ERILLISHAKU_TUONTI_HAKIJA_PAIVITYS,
                              ValintaResource.ERILLISHAUNTUONTISERVICE,
                              hakijaDTO.getHakijaOid(),
                              Changes.addedDto(hakijaDTO),
                              additionalAuditInfo);
                        }
                      });
              if (poikkeukset.isEmpty()) {
                prosessi.vaiheValmistui();
                prosessi.valmistui("ok");
              } else {
                String messages =
                    poikkeukset.stream().map(Poikkeus::getViesti).collect(Collectors.joining(", "));
                LOG.error(
                    String.format(
                        "Osa erillishaun %s tulosten tallennuksesta epäonnistui: %s",
                        haku.getHakuOid(), messages));
                prosessi.keskeyta(poikkeukset);
              }
            },
            t -> {
              errorLogIncludingHttpResponse("Erillishaun tallennus epäonnistui", t);
              prosessi.keskeyta(new Poikkeus(Poikkeus.KOOSTEPALVELU, "", t.getMessage()));
            });
  }

  // TODO: dead code, should be done in ataru side after the change.
  private Observable<Boolean> passivoiHakemukset(List<ErillishakuRivi> poistettavat) {
    return Observable.just(true);
  }

  private Observable<String> maksutilojenTallennus(
      final AuditSession auditSession,
      final ErillishakuDTO haku,
      final List<ErillishakuRivi> lisattavatTaiKeskeneraiset) {
    final Map<String, Maksuntila> maksuntilat =
        lisattavatTaiKeskeneraiset.stream()
            .filter(l -> l.getMaksuntila() != null)
            .filter(l -> Maksuvelvollisuus.REQUIRED.equals(l.getMaksuvelvollisuus()))
            .collect(Collectors.toMap(l -> l.getPersonOid(), l -> l.getMaksuntila()));

    return valintaTulosServiceAsyncResource
        .fetchLukuvuosimaksut(haku.getHakukohdeOid(), auditSession)
        .flatMap(
            nykyisetLukuvuosimaksut -> {
              Map<String, Maksuntila> vanhatMaksuntilat =
                  nykyisetLukuvuosimaksut.stream()
                      .collect(Collectors.toMap(l -> l.getPersonOid(), l -> l.getMaksuntila()));
              List<LukuvuosimaksuMuutos> muuttuneetLukuvuosimaksut =
                  maksuntilat.entrySet().stream()
                      .filter(
                          e -> {
                            final String personOid = e.getKey();
                            Maksuntila vanhaMaksuntila =
                                ofNullable(vanhatMaksuntilat.get(personOid))
                                    .filter(Objects::nonNull)
                                    .orElse(Maksuntila.MAKSAMATTA);
                            return !e.getValue().equals(vanhaMaksuntila);
                          })
                      .map(e -> new LukuvuosimaksuMuutos(e.getKey(), e.getValue()))
                      .collect(Collectors.toList());
              if (muuttuneetLukuvuosimaksut.isEmpty()) {
                return Observable.just("OK");
              } else {
                return valintaTulosServiceAsyncResource.saveLukuvuosimaksut(
                    haku.getHakukohdeOid(), auditSession, muuttuneetLukuvuosimaksut);
              }
            });
  }

  private Observable<List<Poikkeus>> vastaanotonJaValinnantuloksenTallennus(
      final AuditSession auditSession,
      final ErillishakuDTO haku,
      final List<ErillishakuRivi> lisattavatTaiKeskeneraiset,
      final List<ErillishakuRivi> poistettavat) {
    List<ErillishakuRivi> lisattavat =
        lisattavatTaiKeskeneraiset.stream()
            .filter(rivi -> !isKesken(rivi))
            .collect(Collectors.toList());
    List<ErillishakuRivi> kesken =
        lisattavatTaiKeskeneraiset.stream()
            .filter(rivi -> isKesken(rivi))
            .collect(Collectors.toList());

    final Map<String, Valinnantulos> vanhatValinnantulokset = new HashMap<>();
    if (kesken.size() > 0) {
      vanhatValinnantulokset.putAll(
          valintaTulosServiceAsyncResource
              .getErillishaunValinnantulokset(auditSession, haku.getValintatapajonoOid())
              .timeout(5, MINUTES)
              .blockingFirst()
              .stream()
              .collect(Collectors.toMap(Valinnantulos::getHakemusOid, v -> v)));
    }

    return doValinnantuloksenTallennusValintaTulosServiceen(
        auditSession,
        haku,
        createValinnantuloksetForValintaTulosService(
            haku, lisattavat, kesken, poistettavat, vanhatValinnantulokset));
  }

  private static final BiFunction<
          ErillishakuDTO,
          Function<ErillishaunHakijaDTO, Boolean>,
          Function<ErillishakuRivi, Valinnantulos>>
      toValinnantulos =
          (erillishakuDto, doAinoastaanHakemuksenTilaPaivitys) ->
              erillishakuRivi -> {
                final ErillishaunHakijaDTO erillishaunHakijaDto =
                    toErillishaunHakijaDTO(erillishakuDto, erillishakuRivi);
                final Valinnantulos valinnantulos =
                    Valinnantulos.of(
                        erillishaunHakijaDto,
                        doAinoastaanHakemuksenTilaPaivitys.apply(erillishaunHakijaDto));
                valinnantulos.setValinnantilanKuvauksenTekstiFI(
                    erillishakuRivi.getValinnantilanKuvauksenTekstiFI());
                valinnantulos.setValinnantilanKuvauksenTekstiSV(
                    erillishakuRivi.getValinnantilanKuvauksenTekstiSV());
                valinnantulos.setValinnantilanKuvauksenTekstiEN(
                    erillishakuRivi.getValinnantilanKuvauksenTekstiEN());
                return valinnantulos;
              };

  private List<Valinnantulos> createValinnantuloksetForValintaTulosService(
      final ErillishakuDTO haku,
      final List<ErillishakuRivi> lisattavat,
      final List<ErillishakuRivi> kesken,
      final List<ErillishakuRivi> poistettavat,
      final Map<String, Valinnantulos> vanhatValinnantulokset) {
    return Stream.concat(
            Stream.concat(
                poistettavat.stream().map(toValinnantulos.apply(haku, ignore -> false)),
                lisattavat.stream()
                    .map(toValinnantulos.apply(haku, ainoastaanHakemuksenTilaPaivitys))),
            kesken.stream()
                .map(rivi -> toErillishaunHakijaDTO(haku, rivi))
                .filter(hakijaDTO -> vanhatValinnantulokset.containsKey(hakijaDTO.hakemusOid))
                .map(
                    hakijaDTO -> {
                      Valinnantulos valinnantulos =
                          vanhatValinnantulokset.get(hakijaDTO.hakemusOid);
                      valinnantulos.setPoistettava(true);
                      return valinnantulos;
                    }))
        .collect(Collectors.toList());
  }

  private Observable<List<Poikkeus>> doValinnantuloksenTallennusValintaTulosServiceen(
      AuditSession auditSession,
      ErillishakuDTO haku,
      List<Valinnantulos> valinnantuloksetForValintaTulosService) {

    if (valinnantuloksetForValintaTulosService.isEmpty()) {
      return Observable.just(Collections.emptyList());
    }
    return valintaTulosServiceAsyncResource
        .postErillishaunValinnantulokset(
            auditSession, haku.getValintatapajonoOid(), valinnantuloksetForValintaTulosService)
        .map(
            r ->
                r.stream()
                    .map(
                        s ->
                            new Poikkeus(
                                Poikkeus.KOOSTEPALVELU,
                                Poikkeus.VALINTA_TULOS_SERVICE,
                                s.message,
                                new Tunniste(s.hakemusOid, Poikkeus.HAKEMUSOID)))
                    .collect(Collectors.toList()))
        .onErrorResumeNext(
            (Throwable t) ->
                Observable.error(
                    new RuntimeException(
                        String.format(
                            "Erillishaun %s valinnantulosten tallennus valinta-tulos-serviceen epäonnistui",
                            haku.getHakuOid()),
                        t)));
  }

  private List<ErillishakuRivi> kasitteleHakemukset(
      KirjeProsessi prosessi,
      ErillishakuDTO haku,
      List<ErillishakuRivi> lisattavatTaiKeskeneraiset,
      boolean saveApplications) {
    LOG.info("lisattavatTaiKeskeneraiset=" + lisattavatTaiKeskeneraiset.size());
    if (lisattavatTaiKeskeneraiset.isEmpty()) {
      return lisattavatTaiKeskeneraiset;
    }
    try {
      LOG.info("Käsitellään hakemukset ({}kpl)", lisattavatTaiKeskeneraiset.size());
      if (saveApplications) {
        List<AtaruHakemusPrototyyppi> hakemusPrototyypit =
            lisattavatTaiKeskeneraiset.stream()
                .map(
                    rivi ->
                        createHakemusprototyyppi(
                            rivi,
                            convertKuntaNimiToKuntaKoodi(rivi.getKotikunta()),
                            haku.getHakuOid(),
                            haku.getHakukohdeOid()))
                .collect(Collectors.toList());
        LOG.info(
            "Tallennetaan hakemukset ({}kpl) hakemuspalveluun", lisattavatTaiKeskeneraiset.size());
        final List<AtaruSyntheticApplicationResponse> hakemukset;
        try {
          hakemukset =
              ataruAsyncResource
                  .putApplicationPrototypes(hakemusPrototyypit)
                  .timeout(1, MINUTES)
                  .blockingFirst();
        } catch (Exception e) {
          errorLogIncludingHttpResponse(
              String.format(
                  "Error updating application prototypes %s",
                  Arrays.toString(hakemusPrototyypit.toArray())),
              e);
          LOG.error("Rivit={}", Arrays.toString(lisattavatTaiKeskeneraiset.toArray()));
          throw e;
        }
        if (hakemukset.size() != lisattavatTaiKeskeneraiset.size()) { // 1-1 relationship assumed
          LOG.warn(
              "Hakemuspalveluun tallennettujen hakemusten lukumäärä {}kpl on väärä!! Odotettiin {}kpl.",
              hakemukset.size(),
              lisattavatTaiKeskeneraiset.size());
        }
        return zip(
                hakemukset.stream(),
                lisattavatTaiKeskeneraiset.stream(),
                // TODO: ADD HENKILÖ DATA
                (hakemus, rivi) -> rivi.withHakemusOid(hakemus.getHakemusOid()))
            .collect(Collectors.toList());
      } else {
        return lisattavatTaiKeskeneraiset;
      }
    } catch (HenkilonRivinPaattelyEpaonnistuiException e) {
      errorLogIncludingHttpResponse(POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE, e);
      prosessi.keskeyta(
          Poikkeus.hakemuspalvelupoikkeus(
              POIKKEUS_RIVIN_HAKEMINEN_HENKILOLLA_VIRHE + " " + e.getMessage()));
      throw e;
    } catch (Throwable e) { // temporary catch to avoid missing service dependencies
      errorLogIncludingHttpResponse(POIKKEUS_HAKEMUSPALVELUN_VIRHE, e);
      prosessi.keskeyta(Poikkeus.hakemuspalvelupoikkeus(POIKKEUS_HAKEMUSPALVELUN_VIRHE));
      throw e;
    }
  }

  private void errorLogIncludingHttpResponse(String message, Throwable exception) {
    LOG.error(HttpExceptionWithResponse.appendWrappedResponse(message, exception), exception);
  }
}
