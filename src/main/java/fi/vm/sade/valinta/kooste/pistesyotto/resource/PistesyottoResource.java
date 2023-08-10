package fi.vm.sade.valinta.kooste.pistesyotto.resource;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.createAuditSession;
import static fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource.IF_UNMODIFIED_SINCE;
import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Collections.singletonList;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.*;
import fi.vm.sade.valinta.kooste.pistesyotto.service.*;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valinta.sharedutils.http.HttpExceptionWithResponse;
import io.reactivex.Observable;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.util.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController("PistesyottoResource")
@RequestMapping("/pistesyotto")
@PreAuthorize("isAuthenticated()")
@Api(value = "/pistesyotto", description = "Pistesyötön tuonti ja vienti taulukkolaskentaan")
public class PistesyottoResource {
  private static final Logger LOG = LoggerFactory.getLogger(PistesyottoResource.class);

  @Autowired private DokumenttiProsessiKomponentti dokumenttiKomponentti;
  @Autowired private DokumenttiAsyncResource dokumenttiAsyncResource;
  @Autowired private PistesyottoVientiService vientiService;
  @Autowired private PistesyottoTuontiService tuontiService;
  @Autowired private PistesyottoExternalTuontiService externalTuontiService;
  @Autowired private AuthorityCheckService authorityCheckService;
  @Autowired private PistesyottoKoosteService pistesyottoKoosteService;
  @Autowired private ApplicationAsyncResource applicationAsyncResource;
  @Autowired private AtaruAsyncResource ataruAsyncResource;
  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  @GetMapping(
      value = "/koostetutPistetiedot/hakemus/{hakemusOid:.+}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity> koostaPistetiedotYhdelleHakemukselle(
      @PathVariable("hakemusOid") String hakemusOid, HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity> result = new DeferredResult<>(120000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "koostaPistetiedotYhdelleHakemukselle-palvelukutsu on aikakatkaistu: GET /koostetutPistetiedot/hakemus/{}",
              hakemusOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("koostaPistetiedotYhdelleHakemukselle-palvelukutsu on aikakatkaistu"));
        });

    Observable.zip(
            Observable.fromFuture(
                authorityCheckService.getAuthorityCheckForRoles(
                    asList(
                        "ROLE_APP_HAKEMUS_READ_UPDATE",
                        "ROLE_APP_HAKEMUS_READ",
                        "ROLE_APP_HAKEMUS_CRUD",
                        "ROLE_APP_HAKEMUS_LISATIETORU",
                        "ROLE_APP_HAKEMUS_LISATIETOCRUD"))),
            pistesyottoKoosteService.koostaOsallistujanPistetiedot(hakemusOid, auditSession),
            Pair::of)
        .subscribe(
            pair -> {
              HenkiloValilehtiDTO henkiloValilehtiDTO = pair.getRight();
              Set<String> hakutoiveOids = pair.getRight().getHakukohteittain().keySet();
              HakukohdeOIDAuthorityCheck hakukohdeOIDAuthorityCheck = pair.getLeft();

              if (hakutoiveOids.stream().anyMatch(hakukohdeOIDAuthorityCheck)) {
                result.setResult(ResponseEntity.status(HttpStatus.OK).body(henkiloValilehtiDTO));
              } else {
                String msg =
                    String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteisiin %s hakeneen hakemuksen %s pistetietoja",
                        auditSession.getPersonOid(), hakutoiveOids, hakemusOid);
                LOG.error(msg);
                result.setErrorResult(ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg));
              }
            },
            error -> {
              logError("koostaPistetiedotYhdelleHakemukselle epäonnistui", error);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.getMessage()));
            });

    return result;
  }

  @PutMapping(
      value = "/koostetutPistetiedot/hakemus/{hakemusOid:.+}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity> tallennaKoostetutPistetiedotHakemukselle(
      @PathVariable("hakemusOid") String hakemusOid,
      @RequestBody ApplicationAdditionalDataDTO pistetiedot,
      HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);
    final Optional<String> ifUnmodifiedSince = ifUnmodifiedSinceFromHeader(request);

    DeferredResult<ResponseEntity> result = new DeferredResult<>(120000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "tallennaKoostetutPistetiedotHakemukselle-palvelukutsu on aikakatkaistu: PUT /koostetutPistetiedot/hakemus/{}",
              hakemusOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("tallennaKoostetutPistetiedotHakemukselle-palvelukutsu on aikakatkaistu"));
        });

    if (!hakemusOid.equals(pistetiedot.getOid())) {
      String errorMessage =
          String.format(
              "URLissa tuli hakemusOid %s , mutta PUT-datassa hakemusOid %s",
              hakemusOid, pistetiedot.getOid());
      LOG.error(errorMessage);
      result.setResult(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage));
      return result;
    }

    Observable<HakemusWrapper> hakemusO =
        Observable.fromFuture(
                ataruAsyncResource.getApplicationsByOids(Collections.singletonList(hakemusOid)))
            .flatMap(
                hakemukset -> {
                  if (hakemukset.isEmpty()) {
                    return applicationAsyncResource.getApplication(hakemusOid);
                  } else {
                    return Observable.just(hakemukset.iterator().next());
                  }
                });

    Observable.zip(
            Observable.fromFuture(
                authorityCheckService.getAuthorityCheckForRoles(
                    asList(
                        "ROLE_APP_HAKEMUS_READ_UPDATE",
                        "ROLE_APP_HAKEMUS_CRUD",
                        "ROLE_APP_HAKEMUS_LISATIETORU",
                        "ROLE_APP_HAKEMUS_LISATIETOCRUD"))),
            hakemusO,
            Pair::of)
        .subscribe(
            pair -> {
              Collection<String> hakutoiveOids = pair.getRight().getHakutoiveOids();
              HakukohdeOIDAuthorityCheck authorityCheck = pair.getLeft();
              if (hakutoiveOids.stream().anyMatch(authorityCheck)) {
                Set<TuontiErrorDTO> errors =
                    pistesyottoKoosteService
                        .tallennaKoostetutPistetiedotHakemukselle(
                            pistetiedot, ifUnmodifiedSince, auditSession)
                        .toFuture()
                        .get();
                result.setResult(ResponseEntity.status(HttpStatus.NO_CONTENT).body(errors));
              } else {
                String msg =
                    String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteisiin %s hakeneen hakemuksen %s pistetietoja",
                        auditSession.getPersonOid(), hakutoiveOids, hakemusOid);
                result.setErrorResult(ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg));
              }
            },
            error -> {
              logError("tallennaKoostetutPistetiedotHakemukselle epäonnistui", error);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.getMessage()));
            });

    return result;
  }

  @GetMapping(
      value = "/koostetutPistetiedot/haku/{hakuOid}/hakukohde/{hakukohdeOid:.+}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity<PistesyottoValilehtiDTO>> koostaPistetiedotHakemuksille(
      @PathVariable("hakuOid") String hakuOid,
      @PathVariable("hakukohdeOid") String hakukohdeOid,
      HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity<PistesyottoValilehtiDTO>> result = new DeferredResult<>(120000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "koostaPistetiedotHakemuksille-palvelukutsu on aikakatkaistu: GET /koostetutPistetiedot/haku/{}/hakukohde/{}",
              hakuOid,
              hakukohdeOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("koostaPistetiedotHakemuksille-palvelukutsu on aikakatkaistu"));
        });

    Observable.fromFuture(
            authorityCheckService.getAuthorityCheckForRoles(
                asList(
                    "ROLE_APP_HAKEMUS_READ_UPDATE",
                    "ROLE_APP_HAKEMUS_READ",
                    "ROLE_APP_HAKEMUS_CRUD",
                    "ROLE_APP_HAKEMUS_LISATIETORU",
                    "ROLE_APP_HAKEMUS_LISATIETOCRUD")))
        .subscribe(
            authorityCheck -> {
              if (authorityCheck.test(hakukohdeOid)) {
                PistesyottoValilehtiDTO pistetiedot =
                    pistesyottoKoosteService
                        .koostaOsallistujienPistetiedot(hakuOid, hakukohdeOid, auditSession)
                        .get();
                LOG.debug("Saatiin pistetiedot {}", pistetiedot);
                result.setResult(ResponseEntity.status(HttpStatus.OK).body(pistetiedot));
              } else {
                String msg =
                    String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                        auditSession.getPersonOid(), hakukohdeOid);
                LOG.error(msg);
                result.setErrorResult(ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg));
              }
            },
            error -> {
              logError("koostaPistetiedotHakemuksille epäonnistui", error);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.getMessage()));
            });

    return result;
  }

  private Optional<String> ifUnmodifiedSinceFromHeader(HttpServletRequest request) {
    return list(request.getHeaderNames()).stream()
        .map(String::toLowerCase)
        .filter(IF_UNMODIFIED_SINCE.toLowerCase()::equals)
        .map(request::getHeader)
        .findAny();
  }

  @PutMapping(
      value = "/koostetutPistetiedot/haku/{hakuOid}/hakukohde/{hakukohdeOid:.+}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      value = "Lisätietokenttien tallennus hakemuksille ja suoritusrekisteriin")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity<Set<TuontiErrorDTO>>> tallennaKoostetutPistetiedot(
      @PathVariable("hakuOid") String hakuOid,
      @PathVariable("hakukohdeOid") String hakukohdeOid,
      @RequestBody List<ApplicationAdditionalDataDTO> pistetiedot,
      HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);
    Optional<String> ifUnmodifiedSince = ifUnmodifiedSinceFromHeader(request);

    DeferredResult<ResponseEntity<Set<TuontiErrorDTO>>> result = new DeferredResult<>(120000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "tallennaKoostetutPistetiedot-palvelukutsu on aikakatkaistu: PUT /koostetutPistetiedot/haku/{}/hakukohde/{}",
              hakuOid,
              hakukohdeOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("tallennaKoostetutPistetiedot-palvelukutsu on aikakatkaistu"));
        });

    Observable.fromFuture(
            authorityCheckService.getAuthorityCheckForRoles(
                asList(
                    "ROLE_APP_HAKEMUS_READ_UPDATE",
                    "ROLE_APP_HAKEMUS_CRUD",
                    "ROLE_APP_HAKEMUS_LISATIETORU",
                    "ROLE_APP_HAKEMUS_LISATIETOCRUD")))
        .flatMap(
            authorityCheck -> {
              if (authorityCheck.test(hakukohdeOid)) {
                return Observable.just("OK");
              }
              String msg =
                  String.format(
                      "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                      auditSession.getPersonOid(), hakukohdeOid);
              return Observable.error(new AccessDeniedException(msg));
            })
        .flatMap(
            unused ->
                Observable.fromFuture(ataruAsyncResource.getApplicationsByHakukohde(hakukohdeOid))
                    .flatMap(
                        hakemukset -> {
                          if (hakemukset.isEmpty()) {
                            return applicationAsyncResource.getApplicationOids(
                                hakuOid, hakukohdeOid);
                          } else {
                            return Observable.just(
                                hakemukset.stream()
                                    .map(HakemusWrapper::getOid)
                                    .collect(Collectors.toSet()));
                          }
                        })
                    .flatMap(
                        hakukohteenHakemusOidit -> {
                          Set<String> eiHakukohteeseenHakeneet =
                              pistetiedot.stream()
                                  .map(ApplicationAdditionalDataDTO::getOid)
                                  .filter(oid -> !hakukohteenHakemusOidit.contains(oid))
                                  .collect(Collectors.toSet());
                          if (eiHakukohteeseenHakeneet.isEmpty()) {
                            return Observable.just("OK");
                          }
                          return Observable.error(
                              new AccessDeniedException(
                                  String.format(
                                      "Käyttäjällä %s ei ole oikeuksia käsitellä hakemuksien %s pistetietoja, koska niillä ei ole haettu hakukohteeseen %s",
                                      auditSession.getPersonOid(),
                                      eiHakukohteeseenHakeneet,
                                      hakukohdeOid)));
                        }))
        .flatMap(
            unused ->
                Observable.fromFuture(
                    pistesyottoKoosteService.tallennaKoostetutPistetiedot(
                        hakuOid, hakukohdeOid, ifUnmodifiedSince, pistetiedot, auditSession)))
        .subscribe(
            errors -> {
              if (errors.isEmpty()) {
                result.setResult(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
              } else {
                result.setResult(ResponseEntity.status(HttpStatus.NO_CONTENT).body(errors));
              }
            },
            error -> {
              if (error instanceof AccessDeniedException) {
                result.setErrorResult(
                    ResponseEntity.status(HttpStatus.FORBIDDEN).body(error.getMessage()));
              } else {
                logError("tallennaKoostetutPistetiedot epäonnistui", error);
                result.setErrorResult(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(error.getMessage()));
              }
            });

    return result;
  }

  @PostMapping(
      value = "/vienti",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @ApiOperation(
      consumes = "application/json",
      value = "Pistesyötön vienti taulukkolaskentaan",
      response = ProsessiId.class)
  public DeferredResult<ResponseEntity<ProsessiId>> vienti(
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity<ProsessiId>> result = new DeferredResult<>(120000l);
    result.onTimeout(
        () -> {
          LOG.error("vienti-palvelukutsu on aikakatkaistu: POST /vienti");
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("vienti-palvelukutsu on aikakatkaistu"));
        });

    Observable.fromFuture(
            authorityCheckService.getAuthorityCheckForRoles(
                asList(
                    "ROLE_APP_HAKEMUS_READ_UPDATE",
                    "ROLE_APP_HAKEMUS_READ",
                    "ROLE_APP_HAKEMUS_CRUD",
                    "ROLE_APP_HAKEMUS_LISATIETORU",
                    "ROLE_APP_HAKEMUS_LISATIETOCRUD")))
        .subscribe(
            authorityCheck -> {
              if (authorityCheck.test(hakukohdeOid)) {
                DokumenttiProsessi prosessi =
                    new DokumenttiProsessi("Pistesyöttö", "vienti", hakuOid, asList(hakukohdeOid));
                dokumenttiKomponentti.tuoUusiProsessi(prosessi);
                vientiService.vie(hakuOid, hakukohdeOid, auditSession, prosessi);
                result.setResult(
                    ResponseEntity.status(HttpStatus.OK).body(prosessi.toProsessiId()));
              } else {
                String msg =
                    String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                        auditSession.getPersonOid(), hakukohdeOid);
                result.setErrorResult(ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg));
              }
            },
            error -> {
              logError("Pistetietojen vienti epäonnistui", error);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error.getMessage()));
            });

    return result;
  }

  @PostMapping(
      value = "/tuonti",
      consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @ApiOperation(
      consumes = "application/json",
      value = "Pistesyötön tuonti taulukkolaskentaan",
      response = ProsessiId.class)
  public DeferredResult<ResponseEntity<Set<TuontiErrorDTO>>> tuonti(
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestParam(value = "hakukohdeOid", required = false) String hakukohdeOid,
      HttpServletRequest request) {

    DeferredResult<ResponseEntity<Set<TuontiErrorDTO>>> result = new DeferredResult<>(120000l);
    result.onTimeout(
        () -> {
          LOG.error("tuonti-palvelukutsu on aikakatkaistu: POST /tuonti");
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("tuonti-palvelukutsu on aikakatkaistu"));
        });

    try {
      final AuditSession auditSession = createAuditSession(request);
      Observable<Object> authCheck =
          Observable.fromFuture(
                  authorityCheckService.getAuthorityCheckForRoles(
                      asList(
                          "ROLE_APP_HAKEMUS_READ_UPDATE",
                          "ROLE_APP_HAKEMUS_CRUD",
                          "ROLE_APP_HAKEMUS_LISATIETORU",
                          "ROLE_APP_HAKEMUS_LISATIETOCRUD")))
              .flatMap(
                  authorityCheck -> {
                    if (authorityCheck.test(hakukohdeOid)) {
                      return Observable.just("OK");
                    }
                    String msg =
                        String.format(
                            "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                            auditSession.getPersonOid(), hakukohdeOid);
                    return Observable.error(new AccessDeniedException(msg));
                  });

      Observable<Set<TuontiErrorDTO>> map =
          authCheck.flatMap(
              unused -> {
                DokumenttiProsessi prosessi =
                    new DokumenttiProsessi(
                        "Pistesyöttö", "tuonti", hakuOid, singletonList(hakukohdeOid));
                dokumenttiKomponentti.tuoUusiProsessi(prosessi);
                ByteArrayOutputStream xlsx = readFileToBytearray(request.getInputStream());
                final String uuid = UUID.randomUUID().toString();
                Long expirationTime = DateTime.now().plusDays(7).toDate().getTime();
                List<String> tags = asList();
                dokumenttiAsyncResource
                    .tallenna(
                        uuid,
                        "pistesyotto.xlsx",
                        expirationTime,
                        tags,
                        "application/octet-stream",
                        new ByteArrayInputStream(xlsx.toByteArray()))
                    .subscribe(
                        response ->
                            LOG.info(
                                "Käyttäjä {} aloitti pistesyötön tuonnin haussa {} ja hakukohteelle {}. Excel on tallennettu dokumenttipalveluun uuid:lla {} 7 päiväksi.",
                                auditSession.getPersonOid(),
                                hakuOid,
                                hakukohdeOid,
                                uuid),
                        poikkeus ->
                            logError(
                                String.format(
                                    "Käyttäjä %s aloitti pistesyötön tuonnin haussa %s ja hakukohteelle %s. Exceliä ei voitu tallentaa dokumenttipalveluun.",
                                    auditSession.getPersonOid(), hakuOid, hakukohdeOid),
                                poikkeus));
                return tuontiService.tuo(
                    auditSession,
                    hakuOid,
                    hakukohdeOid,
                    prosessi,
                    new ByteArrayInputStream(xlsx.toByteArray()));
              });

      map.subscribe(
          failedIds -> {
            if (failedIds.isEmpty()) {
              LOG.info("Kaikki pistetiedot tallennettu onnistuneesti");
              result.setResult(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
            } else {
              LOG.info(
                  "Joitakin pistetietoja ei voitu tallentaa: {}",
                  StringUtils.join(failedIds.toArray(), ","));
              result.setResult(ResponseEntity.status(HttpStatus.OK).body(failedIds));
            }
          },
          error -> {
            if (error instanceof PistesyotonTuontivirhe) {
              PistesyotonTuontivirhe pistesyotonTuontivirhe = (PistesyotonTuontivirhe) error;
              LOG.warn(
                  String.format(
                      "tallennaKoostetutPistetiedot epäonnistui, vaikuttaa huonolta syötteeltä: "
                          + "%s",
                      pistesyotonTuontivirhe.virheet));
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.BAD_REQUEST)
                      .body(pistesyotonTuontivirhe.virheet));
            } else if (error instanceof AccessDeniedException) {
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.FORBIDDEN)
                      .body(error.getMessage() == null ? "" : error.getMessage()));
            } else {
              logError("Tuntematon virhetilanne", error);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(error.getMessage() == null ? "" : error.getMessage()));
            }
          });
    } catch (Exception e) {
      LOG.error("Odottamaton virhe", e);
      result.setErrorResult(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage()));
    }

    return result;
  }

  private ByteArrayOutputStream readFileToBytearray(InputStream file) {
    try {
      ByteArrayOutputStream xlsx = new ByteArrayOutputStream();
      IOUtils.copy(file, xlsx);
      IOUtils.closeQuietly(file);
      return xlsx;
    } catch (IOException e) {
      throw new RuntimeException("Virhe kopioitaessa syötettyä excel-sheettiä", e);
    }
  }

  @PostMapping(
      value = "/ulkoinen",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @ApiOperation(
      consumes = "application/json",
      value = "Pistesyötön tuonti hakemuksille ulkoisesta järjestelmästä",
      response = UlkoinenResponseDTO.class)
  public DeferredResult<ResponseEntity<UlkoinenResponseDTO>> ulkoinenTuonti(
      @RequestParam(value = "hakuOid", required = false) String hakuOid,
      @RequestBody List<HakemusDTO> hakemukset,
      HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity<UlkoinenResponseDTO>> result = new DeferredResult<>(120000l);
    result.onTimeout(
        () -> {
          LOG.error("Ulkoinen pistesyotto -palvelukutsu on aikakatkaistu: /haku/{}", hakuOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Ulkoinen pistesyotto -palvelukutsu on aikakatkaistu"));
        });

    try {
      if (hakemukset == null || hakemukset.isEmpty()) {
        result.setErrorResult(
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Ulkoinen pistesyotto API requires at least one hakemus"));
        return result;
      }

      Observable.fromFuture(
              authorityCheckService.getAuthorityCheckForRoles(
                  asList(
                      "ROLE_APP_HAKEMUS_READ_UPDATE",
                      "ROLE_APP_HAKEMUS_CRUD",
                      "ROLE_APP_HAKEMUS_LISATIETORU",
                      "ROLE_APP_HAKEMUS_LISATIETOCRUD")))
          .subscribe(
              authorityCheck -> {
                LOG.info(
                    "Pisteiden tuonti ulkoisesta järjestelmästä (haku: {}): {}",
                    hakuOid,
                    hakemukset);
                externalTuontiService.tuo(
                    authorityCheck,
                    hakemukset,
                    auditSession,
                    hakuOid,
                    (onnistuneet, validointivirheet) -> {
                      UlkoinenResponseDTO response = new UlkoinenResponseDTO();
                      response.setKasiteltyOk(onnistuneet);
                      response.setVirheet(Lists.newArrayList(validointivirheet));
                      result.setResult(ResponseEntity.status(HttpStatus.OK).body(response));
                    },
                    sisainenPoikkeus -> {
                      logError("Tuonti ulkoisesta jarjestelmasta epaonnistui!", sisainenPoikkeus);
                      result.setErrorResult(
                          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                              .body(sisainenPoikkeus.toString()));
                    });
              },
              sisainenPoikkeus -> {
                logError("Tuonti ulkoisesta jarjestelmasta epaonnistui!", sisainenPoikkeus);
                result.setErrorResult(
                    ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(sisainenPoikkeus.toString()));
              });
    } catch (Exception e) {
      LOG.error("Tuonti ulkoisesta järjestelmästä epäonnistui", e);
      result.setErrorResult(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.toString()));
    }

    return result;
  }

  private void logError(String errorMessage, Throwable error) {
    LOG.error(HttpExceptionWithResponse.appendWrappedResponse(errorMessage, error), error);
  }
}
