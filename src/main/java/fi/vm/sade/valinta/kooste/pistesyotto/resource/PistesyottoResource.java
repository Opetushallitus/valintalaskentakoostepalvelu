package fi.vm.sade.valinta.kooste.pistesyotto.resource;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.createAuditSession;
import static fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource.IF_UNMODIFIED_SINCE;
import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.MINUTES;

import com.google.common.collect.Lists;
import fi.vm.sade.valinta.kooste.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.external.resource.ataru.AtaruAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.TuontiErrorDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.UlkoinenResponseDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyotonTuontivirhe;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoExternalTuontiService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoKoosteService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoTuontiService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoVientiService;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

@Controller("PistesyottoResource")
@Path("pistesyotto")
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
  @Context private HttpServletRequest httpServletRequestJaxRS;

  @GET
  @Path("/koostetutPistetiedot/hakemus/{hakemusOid}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      consumes = MediaType.APPLICATION_JSON,
      value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public void koostaPistetiedotYhdelleHakemukselle(
      @PathParam("hakemusOid") String hakemusOid, @Suspended final AsyncResponse response) {
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    response.setTimeout(120L, TimeUnit.SECONDS);
    response.setTimeoutHandler(
        handler -> {
          LOG.error(
              "koostaPistetiedotYhdelleHakemukselle-palvelukutsu on aikakatkaistu: GET /koostetutPistetiedot/hakemus/{}",
              hakemusOid);
          handler.resume(
              Response.serverError()
                  .entity("koostaPistetiedotYhdelleHakemukselle-palvelukutsu on aikakatkaistu")
                  .build());
        });
    Observable.zip(
            authorityCheckService.getAuthorityCheckForRoles(
                asList(
                    "ROLE_APP_HAKEMUS_READ_UPDATE",
                    "ROLE_APP_HAKEMUS_READ",
                    "ROLE_APP_HAKEMUS_CRUD",
                    "ROLE_APP_HAKEMUS_LISATIETORU",
                    "ROLE_APP_HAKEMUS_LISATIETOCRUD")),
            pistesyottoKoosteService.koostaOsallistujanPistetiedot(hakemusOid, auditSession),
            (authorityCheck, pistetiedotHakukohteittain) -> {
              Set<String> hakutoiveOids = pistetiedotHakukohteittain.getHakukohteittain().keySet();
              if (hakutoiveOids.stream().anyMatch(authorityCheck)) {
                return Response.ok()
                    .header("Content-Type", "application/json")
                    .entity(pistetiedotHakukohteittain)
                    .build();
              } else {
                String msg =
                    String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteisiin %s hakeneen hakemuksen %s pistetietoja",
                        auditSession.getPersonOid(), hakutoiveOids, hakemusOid);
                LOG.error(msg);
                return Response.status(Response.Status.FORBIDDEN).entity(msg).build();
              }
            })
        .subscribe(
            response::resume,
            error -> {
              logError("koostaPistetiedotYhdelleHakemukselle epäonnistui", error);
              response.resume(Response.serverError().entity(error.getMessage()).build());
            });
  }

  @PUT
  @Path("/koostetutPistetiedot/hakemus/{hakemusOid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(
      consumes = MediaType.APPLICATION_JSON,
      value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public void tallennaKoostetutPistetiedotHakemukselle(
      @PathParam("hakemusOid") String hakemusOid,
      ApplicationAdditionalDataDTO pistetiedot,
      @Suspended final AsyncResponse response) {
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    final Optional<String> ifUnmodifiedSince = ifUnmodifiedSinceFromHeader();
    response.setTimeout(120L, TimeUnit.SECONDS);
    response.setTimeoutHandler(
        handler -> {
          LOG.error(
              "tallennaKoostetutPistetiedotHakemukselle-palvelukutsu on aikakatkaistu: PUT /koostetutPistetiedot/hakemus/{}",
              hakemusOid);
          handler.resume(
              Response.serverError()
                  .entity("tallennaKoostetutPistetiedotHakemukselle-palvelukutsu on aikakatkaistu")
                  .build());
        });

    if (!hakemusOid.equals(pistetiedot.getOid())) {
      String errorMessage =
          String.format(
              "URLissa tuli hakemusOid %s , mutta PUT-datassa hakemusOid %s",
              hakemusOid, pistetiedot.getOid());
      LOG.error(errorMessage);
      response.resume(Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build());
      return;
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

    Observable.merge(
            Observable.zip(
                authorityCheckService.getAuthorityCheckForRoles(
                    asList(
                        "ROLE_APP_HAKEMUS_READ_UPDATE",
                        "ROLE_APP_HAKEMUS_CRUD",
                        "ROLE_APP_HAKEMUS_LISATIETORU",
                        "ROLE_APP_HAKEMUS_LISATIETOCRUD")),
                hakemusO,
                (authorityCheck, hakemus) -> {
                  Collection<String> hakutoiveOids = hakemus.getHakutoiveOids();
                  if (hakutoiveOids.stream().anyMatch(authorityCheck)) {
                    return pistesyottoKoosteService.tallennaKoostetutPistetiedotHakemukselle(
                        pistetiedot, ifUnmodifiedSince, auditSession);
                  } else {
                    String msg =
                        String.format(
                            "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteisiin %s hakeneen hakemuksen %s pistetietoja",
                            auditSession.getPersonOid(), hakutoiveOids, hakemusOid);
                    return Observable.error(
                        new ForbiddenException(
                            msg, Response.status(Response.Status.FORBIDDEN).entity(msg).build()));
                  }
                }))
        .subscribe(
            x -> {
              if (x.isEmpty()) {
                response.resume(Response.noContent().build());
              } else {
                response.resume(Response.ok(x).build());
              }
            },
            error -> {
              logError("tallennaKoostetutPistetiedotHakemukselle epäonnistui", error);
              resumeWithException(response, error);
            });
  }

  @GET
  @Path("/koostetutPistetiedot/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(
      consumes = MediaType.APPLICATION_JSON,
      value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public void koostaPistetiedotHakemuksille(
      @PathParam("hakuOid") String hakuOid,
      @PathParam("hakukohdeOid") String hakukohdeOid,
      @Suspended final AsyncResponse response) {
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    response.setTimeout(120L, TimeUnit.SECONDS);
    response.setTimeoutHandler(
        handler -> {
          LOG.error(
              "koostaPistetiedotHakemuksille-palvelukutsu on aikakatkaistu: GET /koostetutPistetiedot/haku/{}/hakukohde/{}",
              hakuOid,
              hakukohdeOid);
          handler.resume(
              Response.serverError()
                  .entity("koostaPistetiedotHakemuksille-palvelukutsu on aikakatkaistu")
                  .build());
        });
    authorityCheckService
        .getAuthorityCheckForRoles(
            asList(
                "ROLE_APP_HAKEMUS_READ_UPDATE",
                "ROLE_APP_HAKEMUS_READ",
                "ROLE_APP_HAKEMUS_CRUD",
                "ROLE_APP_HAKEMUS_LISATIETORU",
                "ROLE_APP_HAKEMUS_LISATIETOCRUD"))
        .switchMap(
            authorityCheck -> {
              if (authorityCheck.test(hakukohdeOid)) {
                return Observable.fromFuture(
                    pistesyottoKoosteService
                        .koostaOsallistujienPistetiedot(hakuOid, hakukohdeOid, auditSession)
                        .thenApplyAsync(
                            pistetiedot -> {
                              LOG.debug("Saatiin pistetiedot {}", pistetiedot);
                              return Response.ok()
                                  .header("Content-Type", "application/json")
                                  .entity(pistetiedot)
                                  .build();
                            }));
              } else {
                String msg =
                    String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                        auditSession.getPersonOid(), hakukohdeOid);
                LOG.error(msg);
                return Observable.just(
                    Response.status(Response.Status.FORBIDDEN).entity(msg).build());
              }
            })
        .subscribe(
            response::resume,
            error -> {
              logError("koostaPistetiedotHakemuksille epäonnistui", error);
              response.resume(Response.serverError().entity(error.getMessage()).build());
            });
  }

  private Optional<String> ifUnmodifiedSinceFromHeader() {
    HttpServletRequest h = AuthorizationUtil.request(httpServletRequestJaxRS);
    return list(h.getHeaderNames()).stream()
        .map(String::toLowerCase)
        .filter(IF_UNMODIFIED_SINCE.toLowerCase()::equals)
        .map(h::getHeader)
        .findAny();
  }

  @PUT
  @Path("/koostetutPistetiedot/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(
      consumes = MediaType.APPLICATION_JSON,
      value = "Lisätietokenttien tallennus hakemuksille ja suoritusrekisteriin")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  public void tallennaKoostetutPistetiedot(
      @PathParam("hakuOid") String hakuOid,
      @PathParam("hakukohdeOid") String hakukohdeOid,
      List<ApplicationAdditionalDataDTO> pistetiedot,
      @Suspended final AsyncResponse response) {
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    Optional<String> ifUnmodifiedSince = ifUnmodifiedSinceFromHeader();
    response.setTimeout(120L, TimeUnit.SECONDS);
    response.setTimeoutHandler(
        handler -> {
          LOG.error(
              "tallennaKoostetutPistetiedot-palvelukutsu on aikakatkaistu: PUT /koostetutPistetiedot/haku/{}/hakukohde/{}",
              hakuOid,
              hakukohdeOid);
          handler.resume(
              Response.serverError()
                  .entity("tallennaKoostetutPistetiedot-palvelukutsu on aikakatkaistu")
                  .build());
        });
    authorityCheckService
        .getAuthorityCheckForRoles(
            asList(
                "ROLE_APP_HAKEMUS_READ_UPDATE",
                "ROLE_APP_HAKEMUS_CRUD",
                "ROLE_APP_HAKEMUS_LISATIETORU",
                "ROLE_APP_HAKEMUS_LISATIETOCRUD"))
        .flatMap(
            authorityCheck -> {
              if (authorityCheck.test(hakukohdeOid)) {
                return Observable.just("OK");
              }
              String msg =
                  String.format(
                      "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                      auditSession.getPersonOid(), hakukohdeOid);
              return Observable.error(
                  new ForbiddenException(
                      msg, Response.status(Response.Status.FORBIDDEN).entity(msg).build()));
            })
        .flatMap(
            x ->
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
                              new ForbiddenException(
                                  String.format(
                                      "Käyttäjällä %s ei ole oikeuksia käsitellä hakemuksien %s pistetietoja, koska niillä ei ole haettu hakukohteeseen %s",
                                      auditSession.getPersonOid(),
                                      eiHakukohteeseenHakeneet,
                                      hakukohdeOid)));
                        }))
        .flatMap(
            x ->
                Observable.fromFuture(
                    pistesyottoKoosteService.tallennaKoostetutPistetiedot(
                        hakuOid, hakukohdeOid, ifUnmodifiedSince, pistetiedot, auditSession)))
        .subscribe(
            x -> {
              if (x.isEmpty()) {
                response.resume(Response.noContent().build());
              } else {
                response.resume(Response.ok(x).build());
              }
            },
            error -> {
              logError("tallennaKoostetutPistetiedot epäonnistui", error);
              resumeWithException(response, error);
            });
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Path("/vienti")
  @Consumes("application/json")
  @Produces("application/json")
  @ApiOperation(
      consumes = "application/json",
      value = "Pistesyötön vienti taulukkolaskentaan",
      response = ProsessiId.class)
  public void vienti(
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      @Suspended AsyncResponse asyncResponse) {
    final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
    asyncResponse.setTimeout(120L, TimeUnit.SECONDS);
    asyncResponse.setTimeoutHandler(
        handler -> {
          LOG.error("vienti-palvelukutsu on aikakatkaistu: POST /vienti");
          handler.resume(
              Response.serverError().entity("vienti-palvelukutsu on aikakatkaistu").build());
        });

    authorityCheckService
        .getAuthorityCheckForRoles(
            asList(
                "ROLE_APP_HAKEMUS_READ_UPDATE",
                "ROLE_APP_HAKEMUS_READ",
                "ROLE_APP_HAKEMUS_CRUD",
                "ROLE_APP_HAKEMUS_LISATIETORU",
                "ROLE_APP_HAKEMUS_LISATIETOCRUD"))
        .flatMap(
            authorityCheck -> {
              if (authorityCheck.test(hakukohdeOid)) {
                DokumenttiProsessi prosessi =
                    new DokumenttiProsessi("Pistesyöttö", "vienti", hakuOid, asList(hakukohdeOid));
                dokumenttiKomponentti.tuoUusiProsessi(prosessi);
                vientiService.vie(hakuOid, hakukohdeOid, auditSession, prosessi);
                return Observable.just(prosessi.toProsessiId());
              } else {
                String msg =
                    String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                        auditSession.getPersonOid(), hakukohdeOid);
                return Observable.error(
                    new ForbiddenException(
                        msg, Response.status(Response.Status.FORBIDDEN).entity(msg).build()));
              }
            })
        .subscribe(
            id -> asyncResponse.resume(Response.ok(id).build()),
            error -> {
              logError("Pistetietojen vienti epäonnistui", error);
              resumeWithException(asyncResponse, error);
            });
  }

  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @POST
  @Path("/tuonti")
  @Consumes("application/octet-stream")
  @Produces("application/json")
  @ApiOperation(
      consumes = "application/json",
      value = "Pistesyötön tuonti taulukkolaskentaan",
      response = ProsessiId.class)
  public void tuonti(
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("hakukohdeOid") String hakukohdeOid,
      InputStream file,
      @Suspended AsyncResponse asyncResponse) {
    asyncResponse.setTimeout(120L, TimeUnit.SECONDS);
    asyncResponse.setTimeoutHandler(
        handler -> {
          LOG.error("tuonti-palvelukutsu on aikakatkaistu: POST /tuonti");
          handler.resume(
              Response.serverError().entity("tuonti-palvelukutsu on aikakatkaistu").build());
        });

    try {
      final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
      Observable<Object> authCheck =
          authorityCheckService
              .getAuthorityCheckForRoles(
                  asList(
                      "ROLE_APP_HAKEMUS_READ_UPDATE",
                      "ROLE_APP_HAKEMUS_CRUD",
                      "ROLE_APP_HAKEMUS_LISATIETORU",
                      "ROLE_APP_HAKEMUS_LISATIETOCRUD"))
              .flatMap(
                  authorityCheck -> {
                    if (authorityCheck.test(hakukohdeOid)) {
                      return Observable.just("OK");
                    }
                    String msg =
                        String.format(
                            "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                            auditSession.getPersonOid(), hakukohdeOid);
                    return Observable.error(
                        new ForbiddenException(
                            msg, Response.status(Response.Status.FORBIDDEN).entity(msg).build()));
                  });

      Observable<Set<TuontiErrorDTO>> map =
          authCheck.flatMap(
              x -> {
                DokumenttiProsessi prosessi =
                    new DokumenttiProsessi(
                        "Pistesyöttö", "tuonti", hakuOid, singletonList(hakukohdeOid));
                dokumenttiKomponentti.tuoUusiProsessi(prosessi);
                ByteArrayOutputStream xlsx = readFileToBytearray(file);
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
              asyncResponse.resume(Response.noContent().build());
            } else {
              LOG.info(
                  "Joitakin pistetietoja ei voitu tallentaa: {}",
                  StringUtils.join(failedIds.toArray(), ","));
              asyncResponse.resume(Response.ok(failedIds).build());
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
              asyncResponse.resume(
                  Response.status(Response.Status.BAD_REQUEST)
                      .entity(pistesyotonTuontivirhe.virheet)
                      .build());
            } else {
              logError("Tuntematon virhetilanne", error);
              resumeWithException(asyncResponse, error);
            }
          });
    } catch (Exception e) {
      LOG.error("Odottamaton virhe", e);
      asyncResponse.resume(e);
    }
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

  @POST
  @Path("/ulkoinen")
  @PreAuthorize(
      "hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
  @Consumes("application/json")
  @Produces("application/json")
  @ApiOperation(
      consumes = "application/json",
      value = "Pistesyötön tuonti hakemuksille ulkoisesta järjestelmästä",
      response = UlkoinenResponseDTO.class)
  public void ulkoinenTuonti(
      @QueryParam("hakuOid") String hakuOid,
      @QueryParam("valinnanvaiheOid") String valinnanvaiheOid,
      List<HakemusDTO> hakemukset,
      @Suspended AsyncResponse asyncResponse) {
    try {
      final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
      if (hakemukset == null || hakemukset.isEmpty()) {
        asyncResponse.resume(
            Response.serverError()
                .entity("Ulkoinen pistesyotto API requires at least one hakemus")
                .build());
      } else {
        asyncResponse.setTimeout(120L, MINUTES);
        asyncResponse.setTimeoutHandler(
            asyncResponse1 -> {
              LOG.error("Ulkoinen pistesyotto -palvelukutsu on aikakatkaistu: /haku/{}", hakuOid);
              asyncResponse1.resume(
                  Response.serverError()
                      .entity("Ulkoinen pistesyotto -palvelukutsu on aikakatkaistu")
                      .build());
            });

        authorityCheckService
            .getAuthorityCheckForRoles(
                asList(
                    "ROLE_APP_HAKEMUS_READ_UPDATE",
                    "ROLE_APP_HAKEMUS_CRUD",
                    "ROLE_APP_HAKEMUS_LISATIETORU",
                    "ROLE_APP_HAKEMUS_LISATIETOCRUD"))
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
                        asyncResponse.resume(Response.ok(response).build());
                      },
                      sisainenPoikkeus -> {
                        logError("Tuonti ulkoisesta jarjestelmasta epaonnistui!", sisainenPoikkeus);
                        asyncResponse.resume(
                            Response.serverError().entity(sisainenPoikkeus.toString()).build());
                      });
                },
                sisainenPoikkeus -> {
                  logError("Tuonti ulkoisesta jarjestelmasta epaonnistui!", sisainenPoikkeus);
                  asyncResponse.resume(
                      Response.serverError().entity(sisainenPoikkeus.toString()).build());
                });
      }
    } catch (Exception e) {
      LOG.error("Tuonti ulkoisesta järjestelmästä epäonnistui", e);
      asyncResponse.resume(Response.serverError().entity(e.toString()).build());
    }
  }

  private void resumeWithException(@Suspended AsyncResponse response, Throwable error) {
    if (error instanceof WebApplicationException) {
      response.resume(((WebApplicationException) error).getResponse());
    } else {
      response.resume(Response.serverError().entity(error.getMessage()).build());
    }
  }

  private void logError(String errorMessage, Throwable error) {
    LOG.error(HttpExceptionWithResponse.appendWrappedResponse(errorMessage, error), error);
  }
}
