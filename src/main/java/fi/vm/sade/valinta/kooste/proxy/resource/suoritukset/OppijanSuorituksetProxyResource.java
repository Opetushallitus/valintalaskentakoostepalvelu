package fi.vm.sade.valinta.kooste.proxy.resource.suoritukset;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.createAuditSession;
import static java.util.concurrent.TimeUnit.MINUTES;

import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.Hakemus;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.HakemusHakija;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.OhjausparametritAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.ohjausparametrit.dto.ParametritDTO;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.SuoritusrekisteriAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.suoritusrekisteri.dto.Oppija;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.Haku;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.PisteetWithLastModified;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.dto.Valintapisteet;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.util.HakuappHakemusWrapper;
import fi.vm.sade.valinta.kooste.valintalaskenta.util.HakemuksetConverterUtil;
import fi.vm.sade.valintalaskenta.domain.dto.AvainArvoDTO;
import fi.vm.sade.valintalaskenta.domain.dto.HakemusDTO;
import io.reactivex.Observable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController("SuorituksenArvosanatProxyResource")
@RequestMapping("/resources/proxy/suoritukset")
@PreAuthorize("isAuthenticated()")
@Tag(
    name = "/proxy/suoritukset",
    description = "Käyttöliittymäkutsujen välityspalvelin suoritusrekisteriin")
public class OppijanSuorituksetProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(OppijanSuorituksetProxyResource.class);

  @Autowired private SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;

  @Autowired private OhjausparametritAsyncResource ohjausparametritAsyncResource;

  @Autowired private ApplicationAsyncResource applicationAsyncResource;

  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  @Autowired private ValintapisteAsyncResource valintapisteAsyncResource;

  @Autowired private AtaruAsyncResource ataruAsyncResource;

  @Autowired private HakemuksetConverterUtil hakemuksetConverterUtil;

  /**
   * @deprecated Use the one with the fixed path (opiskelijaOid instead of opiskeljaOid) {@link
   *     #getSuoritukset(String, String, String, HttpServletRequest)} ()}
   */
  @GetMapping(
      value =
          "/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskeljaOid/{opiskeljaOid}/hakemusOid/{hakemusOid}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity<Map<String, String>>> getSuorituksetOld(
      @PathVariable("hakuOid") String hakuOid,
      @PathVariable("opiskeljaOid") String opiskeljaOid,
      @PathVariable("hakemusOid") String hakemusOid,
      HttpServletRequest request) {
    return getSuoritukset(hakuOid, opiskeljaOid, hakemusOid, request);
  }

  @GetMapping(
      value =
          "/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskelijaOid/{opiskelijaOid}/hakemusOid/{hakemusOid}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity<Map<String, String>>> getSuoritukset(
      @PathVariable("hakuOid") String hakuOid,
      @PathVariable("opiskelijaOid") String opiskelijaOid,
      @PathVariable("hakemusOid") String hakemusOid,
      HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity<Map<String, String>>> result =
        new DeferredResult<>(2 * 60 * 1000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskeljaOid/{oid}",
              opiskelijaOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Suoritus proxy -palvelukutsu on aikakatkaistu"));
        });

    resolveHakemusDTO(
            auditSession,
            hakuOid,
            opiskelijaOid,
            hakemusOid,
            applicationAsyncResource.getApplication(hakemusOid),
            true)
        .subscribe(
            hakemusDTO -> {
              result.setResult(
                  ResponseEntity.status(HttpStatus.OK).body(getAvainArvoMap(hakemusDTO)));
            },
            poikkeus -> {
              LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(poikkeus.getMessage()));
            });

    return result;
  }

  @PostMapping(
      value = "/suorituksetByHakemusOids/hakuOid/{hakuOid}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @Operation(
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
      summary = "Hakemukset suoritustietoineen tietylle haulle",
      responses = {
        @ApiResponse(
            responseCode = "OK",
            content = @Content(schema = @Schema(implementation = List.class)))
      })
  public DeferredResult<ResponseEntity<List<Map<String, String>>>> getSuoritukset(
      @PathVariable("hakuOid") String hakuOid,
      @RequestParam(value = "fetchEnsikertalaisuus", defaultValue = "false")
          Boolean fetchEnsikertalaisuus,
      @RequestBody List<String> hakemusOids,
      HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity<List<Map<String, String>>>> result =
        new DeferredResult<>(2 * 60 * 1000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskeljaOid/{hakuOid}",
              hakuOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Suoritus proxy -palvelukutsu on aikakatkaistu"));
        });

    resolveHakemusDTOs(auditSession, hakuOid, hakemusOids, fetchEnsikertalaisuus)
        .subscribe(
            (hakemusDTOs -> {
              List<Map<String, String>> listOfMaps =
                  hakemusDTOs.stream().map(this::getAvainArvoMap).collect(Collectors.toList());

              result.setResult(ResponseEntity.status(HttpStatus.OK).body(listOfMaps));
            }),
            (exception -> {
              LOG.error("OppijanSuorituksetProxyResource exception", exception);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(exception.getMessage()));
            }));

    return result;
  }

  /**
   * @deprecated Use the one with the fixed path (opiskelijaOid instead of opiskeljaOid) {@link
   *     #getSuoritukset(String, String, Boolean, Hakemus, HttpServletRequest)} ()}
   */
  @PostMapping(
      value = "/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskeljaOid/{opiskeljaOid}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @Deprecated
  public DeferredResult<ResponseEntity<Map<String, String>>> getSuorituksetOld(
      @PathVariable("hakuOid") String hakuOid,
      @PathVariable("opiskeljaOid") String opiskeljaOid,
      @RequestParam(value = "fetchEnsikertalaisuus", defaultValue = "false")
          Boolean fetchEnsikertalaisuus,
      @RequestBody Hakemus hakemus,
      HttpServletRequest request) {
    return getSuoritukset(hakuOid, opiskeljaOid, fetchEnsikertalaisuus, hakemus, request);
  }

  /*
  Same as above except with the typo on path fixed (opiskeljaOid -> opiskelijaOid)
   */
  @PostMapping(
      value = "/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskelijaOid/{opiskelijaOid}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity<Map<String, String>>> getSuoritukset(
      @PathVariable("hakuOid") String hakuOid,
      @PathVariable("opiskelijaOid") String opiskelijaOid,
      @RequestParam(value = "fetchEnsikertalaisuus", defaultValue = "false")
          Boolean fetchEnsikertalaisuus,
      @RequestBody Hakemus hakemus,
      HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity<Map<String, String>>> result =
        new DeferredResult<>(2 * 60 * 1000l);
    result.onTimeout(
        () -> {
          LOG.error(
              "suorituksetByOpiskelijaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskelijaOid/{oid}",
              opiskelijaOid);
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Suoritus proxy -palvelukutsu on aikakatkaistu"));
        });

    resolveHakemusDTO(
            auditSession,
            hakuOid,
            opiskelijaOid,
            hakemus.getOid(),
            Observable.just(new HakuappHakemusWrapper(hakemus)),
            fetchEnsikertalaisuus)
        .subscribe(
            hakemusDTO -> {
              Map<String, String> avainArvoMap = getAvainArvoMap(hakemusDTO);
              result.setResult(ResponseEntity.status(HttpStatus.OK).body(avainArvoMap));
            },
            poikkeus -> {
              LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(poikkeus.getMessage()));
            });

    return result;
  }

  @PostMapping(
      value = "/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = {MediaType.APPLICATION_JSON_VALUE})
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity<Map<String, Map<String, String>>>>
      getSuorituksetForOpiskelijas(
          @PathVariable("hakuOid") String hakuOid,
          @RequestBody final List<HakemusHakija> allHakemus,
          @RequestParam(value = "fetchEnsikertalaisuus", defaultValue = "false")
              Boolean fetchEnsikertalaisuus,
          HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity<Map<String, Map<String, String>>>> result =
        new DeferredResult<>(2 * 60 * 1000l);
    result.onTimeout(
        () -> {
          LOG.error("suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu");
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Suoritus proxy -palvelukutsu on aikakatkaistu"));
        });

    if (allHakemus == null || allHakemus.isEmpty()) {
      result.setResult(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
      return result;
    }

    // final Map<String, Map<String, String>> allData = new HashMap<>();
    Observable<PisteetWithLastModified> valintapisteet =
        Observable.fromFuture(
            valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                allHakemus.stream().map(h -> h.getHakemus().getOid()).collect(Collectors.toList()),
                auditSession));
    Observable<Haku> hakuV1RDTOObservable =
        Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid));
    Observable.combineLatest(
            hakuV1RDTOObservable,
            valintapisteet,
            (haku, pisteet) -> {
              if (haku == null) {
                throw new RuntimeException(String.format("Hakua %s ei löytynyt", hakuOid));
              }
              LOG.info("Hae suoritukset {} hakemukselle", allHakemus.size());

              List<HakemusWrapper> hakemukset =
                  allHakemus.stream()
                      .map(h -> new HakuappHakemusWrapper(h.getHakemus()))
                      .collect(Collectors.toList());
              List<String> opiskelijaOids =
                  allHakemus.stream()
                      .map(HakemusHakija::getOpiskelijaOid)
                      .collect(Collectors.toList());

              return resolveHakemusDTOs(
                  haku,
                  hakemukset,
                  pisteet.valintapisteet,
                  opiskelijaOids,
                  fetchEnsikertalaisuus,
                  false);
            })
        .flatMap(f -> f)
        .subscribe(
            hakemusDTOs -> {
              Map<String, Map<String, String>> allData =
                  hakemusDTOs.stream()
                      .collect(
                          Collectors.toMap(
                              HakemusDTO::getHakijaOid,
                              this::getAvainArvoMap,
                              (m0, m1) -> {
                                m0.putAll(m1);
                                return m0;
                              }));
              LOG.info(
                  "Haettiin {} hakemukselle {} suoritustietoa", allHakemus.size(), allData.size());
              result.setResult(ResponseEntity.status(HttpStatus.OK).body(allData));
            },
            poikkeus -> {
              LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(poikkeus.getMessage()));
            });

    return result;
  }

  @PostMapping(
      value = "/ataruSuorituksetByOpiskelijaOid/hakuOid/{hakuOid}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public DeferredResult<ResponseEntity<Map<String, Map<String, String>>>>
      getSuorituksetForAtaruOpiskelijas(
          @PathVariable("hakuOid") String hakuOid,
          @RequestBody final List<String> hakemusOids,
          @RequestParam(value = "fetchEnsikertalaisuus", defaultValue = "false")
              Boolean fetchEnsikertalaisuus,
          @RequestParam(value = "shouldUseApplicationPersonOid", defaultValue = "false")
              Boolean shouldUseApplicationPersonOid,
          HttpServletRequest request) {
    final AuditSession auditSession = createAuditSession(request);

    DeferredResult<ResponseEntity<Map<String, Map<String, String>>>> result =
        new DeferredResult<>(2 * 60 * 1000l);
    result.onTimeout(
        () -> {
          LOG.error("ataruSuorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu");
          result.setErrorResult(
              ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                  .body("Suoritus proxy -palvelukutsu on aikakatkaistu"));
        });

    LOG.info(
        "getSuorituksetForAtaruOpiskelijas - query: hakuOid={} fetchEnsikertalaisuus={} shouldUseApplicationPersonOid={} hakemusOids={}",
        hakuOid,
        fetchEnsikertalaisuus,
        shouldUseApplicationPersonOid,
        hakemusOids);

    if (hakemusOids == null || hakemusOids.isEmpty()) {
      result.setResult(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
      return result;
    }

    Observable<PisteetWithLastModified> valintapisteet =
        Observable.fromFuture(
            valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                hakemusOids, auditSession));
    Observable<List<HakemusWrapper>> ataruHakemukset =
        Observable.fromFuture(
            ataruAsyncResource.getApplicationsByOidsWithHarkinnanvaraisuustieto(hakemusOids));
    Observable<Haku> hakuV1RDTOObservable =
        Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid));
    Observable.combineLatest(
            hakuV1RDTOObservable,
            valintapisteet,
            ataruHakemukset,
            (haku, pisteet, hakemukset) -> {
              if (hakemukset == null || hakemukset.isEmpty()) {
                result.setResult(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
                Observable<List<HakemusDTO>> never = Observable.never();
                return never;
              }
              if (haku == null) {
                throw new RuntimeException(String.format("Hakua %s ei löytynyt", hakuOid));
              }
              LOG.info("Hae suoritukset {} hakemukselle", hakemukset.size());

              List<String> personOids;
              if (shouldUseApplicationPersonOid) {
                personOids =
                    hakemukset.stream()
                        .map(HakemusWrapper::getApplicationPersonOid)
                        .collect(Collectors.toList());
              } else {
                personOids =
                    hakemukset.stream()
                        .map(HakemusWrapper::getPersonOid)
                        .collect(Collectors.toList());
              }

              return resolveHakemusDTOs(
                  haku,
                  hakemukset,
                  pisteet.valintapisteet,
                  personOids,
                  fetchEnsikertalaisuus,
                  shouldUseApplicationPersonOid);
            })
        .flatMap(f -> f)
        .subscribe(
            hakemusDTOs -> {
              Map<String, Map<String, String>> allData =
                  hakemusDTOs.stream()
                      .collect(
                          Collectors.toMap(
                              HakemusDTO::getHakijaOid,
                              this::getAvainArvoMap,
                              (m0, m1) -> {
                                m0.putAll(m1);
                                return m0;
                              }));
              LOG.info(
                  "Haettiin {} hakemukselle {} suoritustietoa", hakemusOids.size(), allData.size());
              result.setResult(ResponseEntity.status(HttpStatus.OK).body(allData));
            },
            poikkeus -> {
              LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
              result.setErrorResult(
                  ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                      .body(poikkeus.getMessage()));
            });

    return result;
  }

  private Map<String, String> getAvainArvoMap(HakemusDTO hakemusDTO) {
    try {
      LOG.debug("Hakemuksen {} avaimet: {}", hakemusDTO.getHakemusoid(), hakemusDTO.getAvaimet());
      return hakemusDTO.getAvaimet().stream()
          .map(
              a ->
                  a.getAvain().endsWith("_SUORITETTU")
                      ? new AvainArvoDTO(a.getAvain().replaceFirst("_SUORITETTU", ""), "S")
                      : a)
          .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo));
    } catch (Exception e) {
      LOG.error(
          "Hakemuksen {} avaimien muuttaminen (AvainArvoDTO -> Map) epäonnistui. Avaimet: {}",
          hakemusDTO.getHakemusoid(),
          hakemusDTO.getAvaimet(),
          e);
      throw e;
    }
  }

  private Observable<HakemusDTO> resolveHakemusDTO(
      AuditSession auditSession,
      String hakuOid,
      String opiskelijaOid,
      String hakemusOid,
      Observable<HakemusWrapper> hakemusObservable,
      Boolean fetchEnsikertalaisuus) {
    Observable<Haku> hakuObservable = Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid));
    Observable<Oppija> suorituksetObservable =
        fetchEnsikertalaisuus
            ? suoritusrekisteriAsyncResource.getSuorituksetByOppija(opiskelijaOid, hakuOid)
            : suoritusrekisteriAsyncResource.getSuorituksetWithoutEnsikertalaisuus(opiskelijaOid);
    Observable<ParametritDTO> parametritObservable =
        Observable.fromFuture(ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid));
    Observable<PisteetWithLastModified> valintapisteetObservable =
        Observable.fromFuture(
            valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                Collections.singletonList(hakemusOid), auditSession));

    return Observable.combineLatest(
        valintapisteetObservable,
        hakuObservable,
        suorituksetObservable,
        hakemusObservable,
        parametritObservable,
        (v, haku, oppijanSuoritukset, hakemus, parametrit) -> {
          List<Valintapisteet> valintapisteet = v.valintapisteet;
          List<HakemusWrapper> hakemukset = Collections.singletonList(hakemus);
          List<Oppija> suoritukset = Collections.singletonList(oppijanSuoritukset);
          return createHakemusDTOs(
                  haku, suoritukset, hakemukset, valintapisteet, parametrit, fetchEnsikertalaisuus)
              .get(0);
        });
  }

  /**
   * Fetch and combine data of Hakemus and Suoritus for a single Haku
   *
   * @param hakuOid Used for retrieving Haku from Tarjonta
   * @param hakemusOids Used to limit Hakemukset from Hakuapp
   * @param fetchEnsikertalaisuus Boolean flag if 'ensikertalaisuus' should be fetched
   */
  private Observable<List<HakemusDTO>> resolveHakemusDTOs(
      AuditSession auditSession,
      String hakuOid,
      List<String> hakemusOids,
      Boolean fetchEnsikertalaisuus) {

    Observable<Haku> hakuObservable = Observable.fromFuture(tarjontaAsyncResource.haeHaku(hakuOid));
    Observable<List<HakemusWrapper>> hakemuksetObservable =
        applicationAsyncResource.getApplicationsByHakemusOids(hakemusOids);
    Observable<List<Valintapisteet>> valintapisteetObservable =
        Observable.fromFuture(
                valintapisteAsyncResource.getValintapisteetWithHakemusOidsAsFuture(
                    hakemusOids, auditSession))
            .map(f -> f.valintapisteet);
    Observable<ParametritDTO> parametritObservable =
        Observable.fromFuture(ohjausparametritAsyncResource.haeHaunOhjausparametrit(hakuOid));

    // Fetch Oppija (suoritusdata) for each personOid in hakemukset
    Observable<List<String>> opiskelijaOidsObservable =
        hakemuksetObservable
            .flatMap(Observable::fromIterable)
            .map(HakemusWrapper::getPersonOid)
            .toList()
            .toObservable();
    Observable<List<Oppija>> suorituksetObservable =
        opiskelijaOidsObservable
            .flatMap(Observable::fromIterable)
            .flatMap(
                o -> {
                  if (fetchEnsikertalaisuus) {
                    return suoritusrekisteriAsyncResource.getSuorituksetByOppija(o, hakuOid);
                  } else {
                    return suoritusrekisteriAsyncResource.getSuorituksetWithoutEnsikertalaisuus(o);
                  }
                })
            .toList()
            .toObservable();

    /**
     * Combine observables using zip
     *
     * <p>When each have a value merge the data using a converter and return a list of HakemusDTOs
     */
    return Observable.zip(
        valintapisteetObservable,
        hakuObservable,
        suorituksetObservable,
        hakemuksetObservable,
        parametritObservable,
        (valintapisteet, haku, suoritukset, hakemukset, parametrit) ->
            createHakemusDTOs(
                haku, suoritukset, hakemukset, valintapisteet, parametrit, fetchEnsikertalaisuus));
  }

  /** Fetch and combine data of Suoritus with passed Hakemus */
  private Observable<List<HakemusDTO>> resolveHakemusDTOs(
      Haku haku,
      List<HakemusWrapper> hakemukset,
      List<Valintapisteet> valintapisteet,
      List<String> opiskelijaOids,
      Boolean fetchEnsikertalaisuus,
      Boolean shouldUseApplicationPersonOid) {

    Observable<ParametritDTO> parametritObservable =
        Observable.fromFuture(ohjausparametritAsyncResource.haeHaunOhjausparametrit(haku.oid));

    Observable<List<Oppija>> suorituksetObservable =
        fetchEnsikertalaisuus
            ? Observable.fromFuture(
                suoritusrekisteriAsyncResource.getSuorituksetByOppijas(opiskelijaOids, haku.oid))
            : suoritusrekisteriAsyncResource.getSuorituksetWithoutEnsikertalaisuus(opiskelijaOids);

    return Observable.zip(
        suorituksetObservable,
        parametritObservable,
        (suoritukset, parametrit) ->
            createHakemusDTOs(
                haku,
                suoritukset,
                hakemukset,
                valintapisteet,
                parametrit,
                fetchEnsikertalaisuus,
                shouldUseApplicationPersonOid));
  }

  private List<HakemusDTO> createHakemusDTOs(
      Haku haku,
      List<Oppija> suoritukset,
      List<HakemusWrapper> hakemukset,
      List<Valintapisteet> valintapisteet,
      ParametritDTO parametrit,
      Boolean fetchEnsikertalaisuus) {

    return createHakemusDTOs(
        haku, suoritukset, hakemukset, valintapisteet, parametrit, fetchEnsikertalaisuus, false);
  }

  private List<HakemusDTO> createHakemusDTOs(
      Haku haku,
      List<Oppija> suoritukset,
      List<HakemusWrapper> hakemukset,
      List<Valintapisteet> valintapisteet,
      ParametritDTO parametrit,
      Boolean fetchEnsikertalaisuus,
      Boolean shouldUseApplicationPersonOid) {

    Map<String, List<String>> hakukohdeRyhmasForHakukohdes =
        Observable.fromFuture(tarjontaAsyncResource.haunHakukohderyhmatCached(haku.oid))
            .timeout(1, MINUTES)
            .blockingFirst();
    return hakemuksetConverterUtil.muodostaHakemuksetDTOfromHakemukset(
        haku,
        "",
        hakukohdeRyhmasForHakukohdes,
        hakemukset,
        valintapisteet,
        suoritukset,
        parametrit,
        fetchEnsikertalaisuus,
        shouldUseApplicationPersonOid);
  }
}
