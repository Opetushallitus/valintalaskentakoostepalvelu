package fi.vm.sade.valinta.kooste.pistesyotto.resource;

import static fi.vm.sade.valinta.kooste.AuthorizationUtil.*;
import static fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource.IF_UNMODIFIED_SINCE;
import static java.util.Arrays.asList;
import static java.util.Collections.*;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.ApplicationAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintapiste.ValintapisteAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.dto.AuditSession;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.UlkoinenResponseDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoKoosteService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoTuontiService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoExternalTuontiService;
import fi.vm.sade.valinta.kooste.pistesyotto.service.PistesyottoVientiService;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.util.Converter;
import fi.vm.sade.valinta.kooste.util.HakemusWrapper;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import fi.vm.sade.valintalaskenta.domain.dto.HakukohdeDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.util.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;

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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller("PistesyottoResource")
@Path("pistesyotto")
@PreAuthorize("isAuthenticated()")
@Api(value = "/pistesyotto", description = "Pistesyötön tuonti ja vienti taulukkolaskentaan")
public class PistesyottoResource {
    private static final Logger LOG = LoggerFactory.getLogger(PistesyottoResource.class);

    @Autowired
    private DokumenttiProsessiKomponentti dokumenttiKomponentti;
    @Autowired
    private DokumenttiAsyncResource dokumenttiAsyncResource;
    @Autowired
    private PistesyottoVientiService vientiService;
    @Autowired
    private PistesyottoTuontiService tuontiService;
    @Autowired
    private PistesyottoExternalTuontiService externalTuontiService;
    @Autowired
    private AuthorityCheckService authorityCheckService;
    @Autowired
    private PistesyottoKoosteService pistesyottoKoosteService;
    @Autowired
    private ApplicationAsyncResource applicationAsyncResource;
    @Autowired
    private TarjontaAsyncResource tarjontaAsyncResource;
    @Context
    private HttpServletRequest httpServletRequestJaxRS;

    @GET
    @Path("/koostetutPistetiedot/hakemus/{hakemusOid}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(consumes = MediaType.APPLICATION_JSON, value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    public void koostaPistetiedotYhdelleHakemukselle(@PathParam("hakemusOid") String hakemusOid,
                                                     @Suspended final AsyncResponse response) {
        final String username = KoosteAudit.username();
        final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
        response.setTimeout(120L, TimeUnit.SECONDS);
        response.setTimeoutHandler(handler -> {
            LOG.error("koostaPistetiedotYhdelleHakemukselle-palvelukutsu on aikakatkaistu: GET /koostetutPistetiedot/hakemus/{}", hakemusOid);
            handler.resume(Response.serverError()
                    .entity("koostaPistetiedotYhdelleHakemukselle-palvelukutsu on aikakatkaistu")
                    .build());
        });
        Observable.zip(
                authorityCheckService.getAuthorityCheckForRoles(asList(
                        "ROLE_APP_HAKEMUS_READ_UPDATE",
                        "ROLE_APP_HAKEMUS_READ",
                        "ROLE_APP_HAKEMUS_CRUD",
                        "ROLE_APP_HAKEMUS_LISATIETORU",
                        "ROLE_APP_HAKEMUS_LISATIETOCRUD"
                )),
                pistesyottoKoosteService.koostaOsallistujanPistetiedot(hakemusOid, auditSession),
                (authorityCheck, pistetiedotHakukohteittain) -> {

                    Set<String> hakutoiveOids = pistetiedotHakukohteittain.getHakukohteittain().keySet();
                    if (hakutoiveOids.stream().anyMatch(authorityCheck)) {
                        return Response.ok()
                                .header("Content-Type", "application/json")
                                .entity(pistetiedotHakukohteittain)
                                .build();
                    } else {
                        String msg = String.format(
                                "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteisiin %s hakeneen hakemuksen %s pistetietoja",
                                username, hakutoiveOids, hakemusOid
                        );
                        LOG.error(msg);
                        return Response.status(Response.Status.FORBIDDEN).entity(msg).build();
                    }
                }
        ).subscribe(
                response::resume,
                error -> {
                    logError("koostaPistetiedotYhdelleHakemukselle epäonnistui", error);
                    response.resume(Response.serverError().entity(error.getMessage()).build());
                }
        );
    }

    @PUT
    @Path("/koostetutPistetiedot/hakemus/{hakemusOid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(consumes = MediaType.APPLICATION_JSON, value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    public void tallennaKoostetutPistetiedotHakemukselle(@PathParam("hakemusOid") String hakemusOid,
                                                         ApplicationAdditionalDataDTO pistetiedot,
                                                         @Suspended final AsyncResponse response) {
        final String username = KoosteAudit.username();
        final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
        final Optional<String> ifUnmodifiedSince = ifUnmodifiedSinceFromHeader();
        response.setTimeout(120L, TimeUnit.SECONDS);
        response.setTimeoutHandler(handler -> {
            LOG.error("tallennaKoostetutPistetiedotHakemukselle-palvelukutsu on aikakatkaistu: PUT /koostetutPistetiedot/hakemus/{}", hakemusOid);
            handler.resume(Response.serverError()
                    .entity("tallennaKoostetutPistetiedotHakemukselle-palvelukutsu on aikakatkaistu")
                    .build());
        });

        if (!hakemusOid.equals(pistetiedot.getOid())) {
            String errorMessage = String.format(
                    "URLissa tuli hakemusOid %s , mutta PUT-datassa hakemusOid %s",
                    hakemusOid, pistetiedot.getOid()
            );
            LOG.error(errorMessage);
            response.resume(Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build());
            return;
        }

        Observable.merge(Observable.zip(
                authorityCheckService.getAuthorityCheckForRoles(asList(
                        "ROLE_APP_HAKEMUS_READ_UPDATE",
                        "ROLE_APP_HAKEMUS_CRUD",
                        "ROLE_APP_HAKEMUS_LISATIETORU",
                        "ROLE_APP_HAKEMUS_LISATIETOCRUD"
                )),
                applicationAsyncResource.getApplication(hakemusOid),
                (authorityCheck, hakemus) -> {
                    Collection<String> hakutoiveOids = new HakemusWrapper(hakemus).getHakutoiveOids();
                    if (hakutoiveOids.stream().anyMatch(authorityCheck)) {
                        return pistesyottoKoosteService.tallennaKoostetutPistetiedotHakemukselle(
                                pistetiedot, ifUnmodifiedSince, username, auditSession
                        );
                    } else {
                        String msg = String.format(
                                "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteisiin %s hakeneen hakemuksen %s pistetietoja",
                                username, hakutoiveOids, hakemusOid
                        );
                        return Observable.error(new ForbiddenException(
                                msg, Response.status(Response.Status.FORBIDDEN).entity(msg).build()
                        ));
                    }
                }
        )).subscribe(
                x -> {
                    if(x.isEmpty()) {
                        response.resume(Response.noContent().build());
                    } else {
                        response.resume(Response.ok(x).build());
                    }
                },
                error -> {
                    logError("tallennaKoostetutPistetiedotHakemukselle epäonnistui", error);
                    resumeWithException(response, error);
                }
        );
    }

    @GET
    @Path("/koostetutPistetiedot/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(consumes = MediaType.APPLICATION_JSON, value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    public void koostaPistetiedotHakemuksille(@PathParam("hakuOid") String hakuOid,
                                              @PathParam("hakukohdeOid") String hakukohdeOid,
                                              @Suspended final AsyncResponse response) {
        final String username = KoosteAudit.username();
        final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
        response.setTimeout(120L, TimeUnit.SECONDS);
        response.setTimeoutHandler(handler -> {
            LOG.error("koostaPistetiedotHakemuksille-palvelukutsu on aikakatkaistu: GET /koostetutPistetiedot/haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
            handler.resume(Response.serverError()
                    .entity("koostaPistetiedotHakemuksille-palvelukutsu on aikakatkaistu")
                    .build());
        });
        authorityCheckService.getAuthorityCheckForRoles(asList(
                "ROLE_APP_HAKEMUS_READ_UPDATE",
                "ROLE_APP_HAKEMUS_READ",
                "ROLE_APP_HAKEMUS_CRUD",
                "ROLE_APP_HAKEMUS_LISATIETORU",
                "ROLE_APP_HAKEMUS_LISATIETOCRUD"
        )).switchMap(authorityCheck -> {
            if (authorityCheck.test(hakukohdeOid)) {
                return pistesyottoKoosteService.koostaOsallistujienPistetiedot(hakuOid, hakukohdeOid, auditSession)
                        .map(pistetiedot -> {
                            LOG.debug("Saatiin pistetiedot {}", pistetiedot);
                            return Response.ok()
                                    .header("Content-Type", "application/json")
                                    .entity(pistetiedot)
                                    .build();
                        });
            } else {
                String msg = String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                        username, hakukohdeOid
                );
                LOG.error(msg);
                return Observable.just(Response.status(Response.Status.FORBIDDEN).entity(msg).build());
            }
        }).subscribe(
                response::resume,
                error -> {
                    logError("koostaPistetiedotHakemuksille epäonnistui", error);
                    response.resume(Response.serverError().entity(error.getMessage()).build());
                }
        );
    }
    private Optional<String> ifUnmodifiedSinceFromHeader() {
        HttpServletRequest h = AuthorizationUtil.request(httpServletRequestJaxRS);
        return list(h.getHeaderNames()).stream()
                .map(String::toLowerCase)
                .filter(IF_UNMODIFIED_SINCE.toLowerCase()::equals)
                .map(h::getHeader).findAny();
    }

    @PUT
    @Path("/koostetutPistetiedot/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(consumes = MediaType.APPLICATION_JSON, value = "Lisätietokenttien tallennus hakemuksille ja suoritusrekisteriin")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    public void tallennaKoostetutPistetiedot(@PathParam("hakuOid") String hakuOid,
                                             @PathParam("hakukohdeOid") String hakukohdeOid,
                                             List<ApplicationAdditionalDataDTO> pistetiedot,
                                             @Suspended final AsyncResponse response) {
        final String username = KoosteAudit.username();
        final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
        Optional<String> ifUnmodifiedSince = ifUnmodifiedSinceFromHeader();
        response.setTimeout(120L, TimeUnit.SECONDS);
        response.setTimeoutHandler(handler -> {
            LOG.error("tallennaKoostetutPistetiedot-palvelukutsu on aikakatkaistu: PUT /koostetutPistetiedot/haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
            handler.resume(Response.serverError()
                    .entity("tallennaKoostetutPistetiedot-palvelukutsu on aikakatkaistu")
                    .build());
        });
        authorityCheckService.getAuthorityCheckForRoles(asList(
                "ROLE_APP_HAKEMUS_READ_UPDATE",
                "ROLE_APP_HAKEMUS_CRUD",
                "ROLE_APP_HAKEMUS_LISATIETORU",
                "ROLE_APP_HAKEMUS_LISATIETOCRUD"
        )).flatMap(authorityCheck -> {
            if (authorityCheck.test(hakukohdeOid)) {
                return Observable.just(null);
            }
            String msg = String.format(
                    "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                    username, hakukohdeOid
            );
            return Observable.error(new ForbiddenException(
                    msg, Response.status(Response.Status.FORBIDDEN).entity(msg).build()
            ));
        }).flatMap(x -> applicationAsyncResource.getApplicationOids(hakuOid, hakukohdeOid)
                .flatMap(hakukohteenHakemusOidit -> {
                    Set<String> eiHakukohteeseenHakeneet = pistetiedot.stream()
                            .map(ApplicationAdditionalDataDTO::getOid)
                            .filter(oid -> !hakukohteenHakemusOidit.contains(oid))
                            .collect(Collectors.toSet());
                    if (eiHakukohteeseenHakeneet.isEmpty()) {
                        return Observable.just(null);
                    }
                    return Observable.error(new ForbiddenException(String.format(
                            "Käyttäjällä %s ei ole oikeuksia käsitellä hakemuksien %s pistetietoja, koska niillä ei ole haettu hakukohteeseen %s",
                            username, eiHakukohteeseenHakeneet, hakukohdeOid
                    )));
                })
        ).flatMap(x -> pistesyottoKoosteService.tallennaKoostetutPistetiedot(
                hakuOid, hakukohdeOid, ifUnmodifiedSince, pistetiedot, username, auditSession)
        ).subscribe(
                x -> {
                    if(x.isEmpty()) {
                        response.resume(Response.noContent().build());
                    } else {
                        response.resume(Response.ok(x).build());
                    }
                },
                error -> {
                    logError("tallennaKoostetutPistetiedot epäonnistui", error);
                    resumeWithException(response, error);
                }
        );
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/vienti")
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön vienti taulukkolaskentaan", response = ProsessiId.class)
    public void vienti(@QueryParam("hakuOid") String hakuOid,
                       @QueryParam("hakukohdeOid") String hakukohdeOid,
                       @Suspended AsyncResponse asyncResponse) {
        final String username = KoosteAudit.username();
        final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
        asyncResponse.setTimeout(120L, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("vienti-palvelukutsu on aikakatkaistu: POST /vienti");
            handler.resume(Response.serverError()
                    .entity("vienti-palvelukutsu on aikakatkaistu")
                    .build());
        });

        authorityCheckService.getAuthorityCheckForRoles(asList(
                "ROLE_APP_HAKEMUS_READ_UPDATE",
                "ROLE_APP_HAKEMUS_READ",
                "ROLE_APP_HAKEMUS_CRUD",
                "ROLE_APP_HAKEMUS_LISATIETORU",
                "ROLE_APP_HAKEMUS_LISATIETOCRUD"
        )).flatMap(authorityCheck -> {
            if (authorityCheck.test(hakukohdeOid)) {
                DokumenttiProsessi prosessi = new DokumenttiProsessi("Pistesyöttö", "vienti", hakuOid, asList(hakukohdeOid));
                dokumenttiKomponentti.tuoUusiProsessi(prosessi);
                vientiService.vie(hakuOid, hakukohdeOid, auditSession, prosessi);
                return Observable.just(prosessi.toProsessiId());
            } else {
                String msg = String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                        username, hakukohdeOid
                );
                return Observable.error(new ForbiddenException(
                        msg, Response.status(Response.Status.FORBIDDEN).entity(msg).build()
                ));
            }
        }).subscribe(
                id -> asyncResponse.resume(Response.ok(id).build()),
                error -> {
                    logError("Pistetietojen vienti epäonnistui", error);
                    resumeWithException(asyncResponse, error);
                }
        );
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/tuonti")
    @Consumes("application/octet-stream")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön tuonti taulukkolaskentaan", response = ProsessiId.class)
    public void tuonti(@QueryParam("hakuOid") String hakuOid,
                       @QueryParam("hakukohdeOid") String hakukohdeOid,
                       InputStream file,
                       @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(120L, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(handler -> {
            LOG.error("tuonti-palvelukutsu on aikakatkaistu: POST /tuonti");
            handler.resume(Response.serverError()
                    .entity("tuonti-palvelukutsu on aikakatkaistu")
                    .build());
        });

        try {
            final String username = KoosteAudit.username();
            final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
            authorityCheckService.getAuthorityCheckForRoles(asList(
                    "ROLE_APP_HAKEMUS_READ_UPDATE",
                    "ROLE_APP_HAKEMUS_CRUD",
                    "ROLE_APP_HAKEMUS_LISATIETORU",
                    "ROLE_APP_HAKEMUS_LISATIETOCRUD"
            )).flatMap(authorityCheck -> {
                if (authorityCheck.test(hakukohdeOid)) {
                    return Observable.just(null);
                }
                String msg = String.format(
                        "Käyttäjällä %s ei ole oikeuksia käsitellä hakukohteen %s pistetietoja",
                        username, hakukohdeOid
                );
                return Observable.error(new ForbiddenException(
                        msg, Response.status(Response.Status.FORBIDDEN).entity(msg).build()
                ));
            }).map(x -> {
                DokumenttiProsessi prosessi = new DokumenttiProsessi("Pistesyöttö", "tuonti", hakuOid, singletonList(hakukohdeOid));
                dokumenttiKomponentti.tuoUusiProsessi(prosessi);
                ByteArrayOutputStream xlsx = readFileToBytearray(file);
                final String uuid = UUID.randomUUID().toString();
                Long expirationTime = DateTime.now().plusDays(7).toDate().getTime();
                List<String> tags = asList();
                dokumenttiAsyncResource.tallenna(uuid, "pistesyotto.xlsx", expirationTime, tags,
                        "application/octet-stream", new ByteArrayInputStream(xlsx.toByteArray()),
                        response -> LOG.info(
                                "Käyttäjä {} aloitti pistesyötön tuonnin haussa {} ja hakukohteelle {}. Excel on tallennettu dokumenttipalveluun uuid:lla {} 7 päiväksi.",
                                username, hakuOid, hakukohdeOid, uuid),
                        poikkeus -> logError(
                                String.format(
                                        "Käyttäjä %s aloitti pistesyötön tuonnin haussa %s ja hakukohteelle %s. Exceliä ei voitu tallentaa dokumenttipalveluun.",
                                        username, hakuOid, hakukohdeOid),
                                poikkeus)
                );
                tuontiService.tuo(username, auditSession, hakuOid, hakukohdeOid, prosessi, new ByteArrayInputStream(xlsx.toByteArray()));
                return prosessi.toProsessiId();
            }).subscribe(
                    id -> asyncResponse.resume(Response.ok(id).build()),
                    error -> {
                        logError("Tuntematon virhetilanne", error);
                        resumeWithException(asyncResponse, error);
                    }
            );
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
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön tuonti hakemuksille ulkoisesta järjestelmästä", response = UlkoinenResponseDTO.class)
    public void ulkoinenTuonti(@QueryParam("hakuOid") String hakuOid,
                               @QueryParam("valinnanvaiheOid") String valinnanvaiheOid,
                               List<HakemusDTO> hakemukset,
                               @Suspended AsyncResponse asyncResponse) {
        try {
            final AuditSession auditSession = createAuditSession(httpServletRequestJaxRS);
            if (hakemukset == null || hakemukset.isEmpty()) {
                asyncResponse.resume(Response.serverError().entity("Ulkoinen pistesyotto API requires at least one hakemus").build());
            } else {
                asyncResponse.setTimeout(120L, TimeUnit.MINUTES);
                asyncResponse.setTimeoutHandler(asyncResponse1 -> {
                    LOG.error("Ulkoinen pistesyotto -palvelukutsu on aikakatkaistu: /haku/{}", hakuOid);
                    asyncResponse1.resume(Response.serverError().entity("Ulkoinen pistesyotto -palvelukutsu on aikakatkaistu").build());
                });

                final String username = KoosteAudit.username();
                authorityCheckService.getAuthorityCheckForRoles(
                        asList("ROLE_APP_HAKEMUS_READ_UPDATE", "ROLE_APP_HAKEMUS_CRUD", "ROLE_APP_HAKEMUS_LISATIETORU", "ROLE_APP_HAKEMUS_LISATIETOCRUD")
                ).subscribe(
                        authorityCheck -> {
                            LOG.info("Pisteiden tuonti ulkoisesta järjestelmästä (haku: {}): {}", hakuOid, hakemukset);
                            externalTuontiService.tuo(authorityCheck, hakemukset, username, auditSession, hakuOid,
                                    (onnistuneet, validointivirheet) -> {
                                        UlkoinenResponseDTO response = new UlkoinenResponseDTO();
                                        response.setKasiteltyOk(onnistuneet);
                                        response.setVirheet(Lists.newArrayList(validointivirheet));
                                        asyncResponse.resume(Response.ok(response).build());
                                    },
                                    sisainenPoikkeus -> {
                                        logError("Tuonti ulkoisesta jarjestelmasta epaonnistui!", sisainenPoikkeus);
                                        asyncResponse.resume(Response.serverError().entity(sisainenPoikkeus.toString()).build());
                                    });
                        },
                        sisainenPoikkeus -> {
                            logError("Tuonti ulkoisesta jarjestelmasta epaonnistui!", sisainenPoikkeus);
                            asyncResponse.resume(Response.serverError().entity(sisainenPoikkeus.toString()).build());
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
