package fi.vm.sade.valinta.kooste.proxy.resource.suoritukset;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.createAuditSession;
import static fi.vm.sade.valinta.kooste.util.ResponseUtil.respondWithError;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller("SuorituksenArvosanatProxyResource")
@Path("/proxy/suoritukset")
@PreAuthorize("isAuthenticated()")
@Api(
    value = "/proxy/suoritukset",
    description = "Käyttöliittymäkutsujen välityspalvelin suoritusrekisteriin")
public class OppijanSuorituksetProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(OppijanSuorituksetProxyResource.class);

  @Context private HttpServletRequest httpServletRequestJaxRS;
  @Autowired private SuoritusrekisteriAsyncResource suoritusrekisteriAsyncResource;

  @Autowired private OhjausparametritAsyncResource ohjausparametritAsyncResource;

  @Autowired private ApplicationAsyncResource applicationAsyncResource;

  @Autowired private TarjontaAsyncResource tarjontaAsyncResource;

  @Autowired private ValintapisteAsyncResource valintapisteAsyncResource;

  @Autowired private AtaruAsyncResource ataruAsyncResource;

  @Autowired private HakemuksetConverterUtil hakemuksetConverterUtil;

  /**
   * @deprecated Use the one with the fixed path (opiskelijaOid instead of opiskeljaOid) {@link
   *     #getSuoritukset(String, String, String, AsyncResponse)} ()}
   */
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @GET
  @Path(
      "/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskeljaOid/{opiskeljaOid}/hakemusOid/{hakemusOid}")
  public void getSuorituksetOld(
      @PathParam("hakuOid") String hakuOid,
      @PathParam("opiskeljaOid") String opiskeljaOid,
      @PathParam("hakemusOid") String hakemusOid,
      @Suspended final AsyncResponse asyncResponse) {
    getSuoritukset(hakuOid, opiskeljaOid, hakemusOid, asyncResponse);
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @GET
  @Path(
      "/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskelijaOid/{opiskelijaOid}/hakemusOid/{hakemusOid}")
  public void getSuoritukset(
      @PathParam("hakuOid") String hakuOid,
      @PathParam("opiskelijaOid") String opiskelijaOid,
      @PathParam("hakemusOid") String hakemusOid,
      @Suspended final AsyncResponse asyncResponse) {
    asyncResponse.setTimeout(2L, MINUTES);
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    asyncResponse.setTimeoutHandler(
        handler -> {
          LOG.error(
              "suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskeljaOid/{oid}",
              opiskelijaOid);
          respondWithError(handler, "Suoritus proxy -palvelukutsu on aikakatkaistu");
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
              asyncResponse.resume(
                  Response.ok()
                      .header("Content-Type", "application/json")
                      .entity(getAvainArvoMap(hakemusDTO))
                      .build());
            },
            poikkeus -> {
              LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
              respondWithError(asyncResponse, poikkeus.getMessage());
            });
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Path("/suorituksetByHakemusOids/hakuOid/{hakuOid}")
  @Consumes("application/json")
  @ApiOperation(
      consumes = "application/json",
      value = "Hakemukset suoritustietoineen tietylle haulle",
      response = Response.class)
  public void getSuoritukset(
      @PathParam("hakuOid") String hakuOid,
      @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
      List<String> hakemusOids,
      @Suspended final AsyncResponse asyncResponse) {

    asyncResponse.setTimeout(2L, MINUTES);
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    asyncResponse.setTimeoutHandler(
        handler -> {
          LOG.error(
              "suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskeljaOid/{hakuOid}",
              hakuOid);
          respondWithError(handler, "Suoritus proxy -palvelukutsu on aikakatkaistu");
        });

    resolveHakemusDTOs(auditSession, hakuOid, hakemusOids, fetchEnsikertalaisuus)
        .subscribe(
            (hakemusDTOs -> {
              List<Map<String, String>> listOfMaps =
                  hakemusDTOs.stream().map(this::getAvainArvoMap).collect(Collectors.toList());

              Response resp =
                  Response.ok()
                      .header("Content-Type", "application/json")
                      .entity(listOfMaps)
                      .build();

              asyncResponse.resume(resp);
            }),
            (exception -> {
              LOG.error("OppijanSuorituksetProxyResource exception", exception);
              respondWithError(asyncResponse, exception.getMessage());
            }));
  }

  /**
   * @deprecated Use the one with the fixed path (opiskelijaOid instead of opiskeljaOid) {@link
   *     #getSuoritukset(String, String, Boolean, Hakemus, AsyncResponse)} ()}
   */
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskeljaOid/{opiskeljaOid}")
  @Deprecated
  public void getSuorituksetOld(
      @PathParam("hakuOid") String hakuOid,
      @PathParam("opiskeljaOid") String opiskeljaOid,
      @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
      Hakemus hakemus,
      @Suspended final AsyncResponse asyncResponse) {
    getSuoritukset(hakuOid, opiskeljaOid, fetchEnsikertalaisuus, hakemus, asyncResponse);
  }

  /*
  Same as above except with the typo on path fixed (opiskeljaOid -> opiskelijaOid)
   */
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}/opiskelijaOid/{opiskelijaOid}")
  public void getSuoritukset(
      @PathParam("hakuOid") String hakuOid,
      @PathParam("opiskelijaOid") String opiskelijaOid,
      @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
      Hakemus hakemus,
      @Suspended final AsyncResponse asyncResponse) {
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    asyncResponse.setTimeout(2L, MINUTES);
    asyncResponse.setTimeoutHandler(
        handler -> {
          LOG.error(
              "suorituksetByOpiskelijaOid proxy -palvelukutsu on aikakatkaistu: /suorituksetByOpiskelijaOid/{oid}",
              opiskelijaOid);
          respondWithError(handler, "Suoritus proxy -palvelukutsu on aikakatkaistu");
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
              asyncResponse.resume(
                  Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(avainArvoMap).build());
            },
            poikkeus -> {
              LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
              respondWithError(asyncResponse, poikkeus.getMessage());
            });
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/suorituksetByOpiskelijaOid/hakuOid/{hakuOid}")
  public void getSuorituksetForOpiskelijas(
      @PathParam("hakuOid") String hakuOid,
      final List<HakemusHakija> allHakemus,
      @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
      @Suspended final AsyncResponse asyncResponse) {
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    asyncResponse.setTimeout(2L, MINUTES);
    asyncResponse.setTimeoutHandler(
        handler -> {
          LOG.error("suorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu");
          respondWithError(handler, "Suoritus proxy -palvelukutsu on aikakatkaistu");
        });

    if (allHakemus == null || allHakemus.isEmpty()) {
      asyncResponse.resume(Response.status(Response.Status.NO_CONTENT).build());
      return;
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
                  haku, hakemukset, pisteet.valintapisteet, opiskelijaOids, fetchEnsikertalaisuus);
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
              asyncResponse.resume(
                  Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(allData).build());
            },
            poikkeus -> {
              LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
              respondWithError(asyncResponse, poikkeus.getMessage());
            });
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/ataruSuorituksetByOpiskelijaOid/hakuOid/{hakuOid}")
  public void getSuorituksetForAtaruOpiskelijas(
      @PathParam("hakuOid") String hakuOid,
      final List<String> hakemusOids,
      @DefaultValue("false") @QueryParam("fetchEnsikertalaisuus") Boolean fetchEnsikertalaisuus,
      @Suspended final AsyncResponse asyncResponse) {
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    asyncResponse.setTimeout(2L, MINUTES);
    asyncResponse.setTimeoutHandler(
        handler -> {
          LOG.error("ataruSuorituksetByOpiskeljaOid proxy -palvelukutsu on aikakatkaistu");
          respondWithError(handler, "Suoritus proxy -palvelukutsu on aikakatkaistu");
        });

    if (hakemusOids == null || hakemusOids.isEmpty()) {
      asyncResponse.resume(Response.status(Response.Status.NO_CONTENT).build());
      return;
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
                asyncResponse.resume(Response.status(Response.Status.NO_CONTENT).build());
              }
              if (haku == null) {
                throw new RuntimeException(String.format("Hakua %s ei löytynyt", hakuOid));
              }
              LOG.info("Hae suoritukset {} hakemukselle", hakemukset.size());

              List<String> personOids =
                  hakemukset.stream()
                      .map(HakemusWrapper::getPersonOid)
                      .collect(Collectors.toList());

              return resolveHakemusDTOs(
                  haku, hakemukset, pisteet.valintapisteet, personOids, fetchEnsikertalaisuus);
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
              asyncResponse.resume(
                  Response.ok().type(MediaType.APPLICATION_JSON_TYPE).entity(allData).build());
            },
            poikkeus -> {
              LOG.error("OppijanSuorituksetProxyResource exception", poikkeus);
              respondWithError(asyncResponse, poikkeus.getMessage());
            });
  }

  private Map<String, String> getAvainArvoMap(HakemusDTO hakemusDTO) {
    return hakemusDTO.getAvaimet().stream()
        .map(
            a ->
                a.getAvain().endsWith("_SUORITETTU")
                    ? new AvainArvoDTO(a.getAvain().replaceFirst("_SUORITETTU", ""), "S")
                    : a)
        .collect(Collectors.toMap(AvainArvoDTO::getAvain, AvainArvoDTO::getArvo));
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
      Boolean fetchEnsikertalaisuus) {

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
                haku, suoritukset, hakemukset, valintapisteet, parametrit, fetchEnsikertalaisuus));
  }

  private List<HakemusDTO> createHakemusDTOs(
      Haku haku,
      List<Oppija> suoritukset,
      List<HakemusWrapper> hakemukset,
      List<Valintapisteet> valintapisteet,
      ParametritDTO parametrit,
      Boolean fetchEnsikertalaisuus) {

    Map<String, List<String>> hakukohdeRyhmasForHakukohdes =
        Observable.fromFuture(tarjontaAsyncResource.hakukohdeRyhmasForHakukohdes(haku.oid))
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
        false);
  }
}
