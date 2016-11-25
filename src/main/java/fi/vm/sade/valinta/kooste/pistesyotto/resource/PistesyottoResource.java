package fi.vm.sade.valinta.kooste.pistesyotto.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.collect.Lists;

import fi.vm.sade.valinta.http.HttpExceptionWithResponse;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.dokumentti.DokumenttiAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.hakuapp.dto.ApplicationAdditionalDataDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.HakemusDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.dto.UlkoinenResponseDTO;
import fi.vm.sade.valinta.kooste.pistesyotto.service.*;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import org.apache.poi.util.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import rx.functions.Action1;

import static com.sun.org.apache.xalan.internal.xsltc.compiler.sym.error;
import static java.util.Arrays.*;

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
    private PistesyottoTuontiSoteliService tuontiSoteliService;
    @Autowired
    private AuthorityCheckService authorityCheckService;
    @Autowired
    private PistesyottoKoosteService pistesyottoKoosteService;

    @GET
    @Path("/koostetutPistetiedot/hakemus/{hakemusOid}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(consumes = MediaType.APPLICATION_JSON, value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    public void koostaPistetiedotYhdelleHakemukselle(
            @PathParam("hakemusOid") String hakemusOid,
            @Suspended final AsyncResponse response) {
        response.setTimeout(30L, TimeUnit.SECONDS);
        response.setTimeoutHandler(handler -> {
            LOG.error("koostaPistetiedotYhdelleHakemukselle-palvelukutsu on aikakatkaistu: GET /koostetutPistetiedot/hakemus/{}", hakemusOid);
            handler.resume(Response.serverError()
                    .entity("koostaPistetiedotYhdelleHakemukselle-palvelukutsu on aikakatkaistu")
                    .build());
        });
        pistesyottoKoosteService.koostaOsallistujanPistetiedot(hakemusOid).subscribe(
                pistetiedotHakukohteittain -> response.resume(Response.ok().header("Content-Type", "application/json").entity(pistetiedotHakukohteittain).build()),
                error -> {
                    logError("koostaPistetiedotHakemuksille epäonnistui", error);
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
    public void tallennaKoostetutPistetiedotHakemukselle(
            @PathParam("hakemusOid") String hakemusOid,
            ApplicationAdditionalDataDTO pistetiedot,
            @Suspended final AsyncResponse response) {
        response.setTimeout(30L, TimeUnit.SECONDS);
        response.setTimeoutHandler(handler -> {
            LOG.error("tallennaKoostetutPistetiedotHakemukselle-palvelukutsu on aikakatkaistu: PUT /koostetutPistetiedot/hakemus/{}", hakemusOid);
            handler.resume(Response.serverError()
                    .entity("tallennaKoostetutPistetiedotHakemukselle-palvelukutsu on aikakatkaistu")
                    .build());
        });
        Action1<Void> onSuccess = (a) -> response.resume(Response.ok().header("Content-Type", "application/json").build());
        Action1<Throwable> onError = (error) -> {
            logError("tallennaKoostetutPistetiedotHakemukselle epäonnistui", error);
            response.resume(Response.serverError().entity(error.getMessage()).build());
        };

        if (!hakemusOid.equals(pistetiedot.getOid())) {
            String errorMessage = String.format("URLissa tuli hakemusOid %s , mutta PUT-datassa hakemusOid %s", hakemusOid, pistetiedot.getOid());
            LOG.error(errorMessage);
            response.resume(Response.serverError().entity(errorMessage).build());
        }
        pistesyottoKoosteService.tallennaKoostetutPistetiedotHakemukselle(pistetiedot, KoosteAudit.username())
                .subscribe(onSuccess, onError);
    }

    @GET
    @Path("/koostetutPistetiedot/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(consumes = MediaType.APPLICATION_JSON, value = "Lisätietokenttien haku hakemukselta ja suoritusrekisteristä")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    public void koostaPistetiedotHakemuksille(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @Suspended final AsyncResponse response) {
        response.setTimeout(30L, TimeUnit.SECONDS);
        response.setTimeoutHandler(handler -> {
            LOG.error("koostaPistetiedotHakemuksille-palvelukutsu on aikakatkaistu: GET /koostetutPistetiedot/haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
            handler.resume(Response.serverError()
                    .entity("koostaPistetiedotHakemuksille-palvelukutsu on aikakatkaistu")
                    .build());
        });
        pistesyottoKoosteService.koostaOsallistujienPistetiedot(hakuOid, hakukohdeOid).subscribe(
                pistetiedot -> response.resume(Response.ok().header("Content-Type", "application/json").entity(pistetiedot).build()),
                error -> {
                    logError("koostaPistetiedotHakemuksille epäonnistui", error);
                    response.resume(Response.serverError().entity(error.getMessage()).build());
                }

        );
    }

    @PUT
    @Path("/koostetutPistetiedot/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(consumes = MediaType.APPLICATION_JSON, value = "Lisätietokenttien tallennus hakemuksille ja suoritusrekisteriin")
    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    public void tallennaKoostetutPistetiedot(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            List<ApplicationAdditionalDataDTO> pistetiedot,
            @Suspended final AsyncResponse response) {
        response.setTimeout(30L, TimeUnit.SECONDS);
        response.setTimeoutHandler(handler -> {
            LOG.error("tallennaKoostetutPistetiedot-palvelukutsu on aikakatkaistu: PUT /koostetutPistetiedot/haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
            handler.resume(Response.serverError()
                    .entity("tallennaKoostetutPistetiedot-palvelukutsu on aikakatkaistu")
                    .build());
        });
        Action1<Void> onSuccess = (a) -> response.resume(Response.ok().header("Content-Type", "application/json").build());
        Action1<Throwable> onError = (error) -> {
            logError("tallennaKoostetutPistetiedot epäonnistui", error);
            response.resume(Response.serverError().entity(error.getMessage()).build());
        };

        pistesyottoKoosteService.tallennaKoostetutPistetiedot(
                hakuOid, hakukohdeOid, pistetiedot, KoosteAudit.username()
        ).subscribe(onSuccess, onError);
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/vienti")
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön vienti taulukkolaskentaan", response = ProsessiId.class)
    public ProsessiId vienti(@QueryParam("hakuOid") String hakuOid, @QueryParam("hakukohdeOid") String hakukohdeOid) {
        DokumenttiProsessi prosessi = new DokumenttiProsessi("Pistesyöttö", "vienti", hakuOid, asList(hakukohdeOid));
        dokumenttiKomponentti.tuoUusiProsessi(prosessi);
        vientiService.vie(hakuOid, hakukohdeOid, prosessi);
        return prosessi.toProsessiId();
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @POST
    @Path("/tuonti")
    @Consumes("application/octet-stream")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Pistesyötön tuonti taulukkolaskentaan", response = ProsessiId.class)
    public ProsessiId tuonti(@QueryParam("hakuOid") String hakuOid, @QueryParam("hakukohdeOid") String hakukohdeOid, InputStream file) throws IOException {
        final String username = KoosteAudit.username();
        ByteArrayOutputStream xlsx;
        IOUtils.copy(file, xlsx = new ByteArrayOutputStream());
        IOUtils.closeQuietly(file);
        try {
            final String uuid = UUID.randomUUID().toString();
            Long expirationTime = DateTime.now().plusDays(7).toDate().getTime();
            List<String> tags = asList();
            dokumenttiAsyncResource.tallenna(uuid, "pistesyotto.xlsx", expirationTime, tags,
                    "application/octet-stream", new ByteArrayInputStream(xlsx.toByteArray()), response -> {
                        LOG.info("Käyttäjä {} aloitti pistesyötön tuonnin haussa {} ja hakukohteelle {}. Excel on tallennettu dokumenttipalveluun uuid:lla {} 7 päiväksi.", username, hakuOid, hakukohdeOid, uuid);
                    }, poikkeus -> {
                        LOG.error("Käyttäjä {} aloitti pistesyötön tuonnin haussa {} ja hakukohteelle {}. Exceliä ei voitu tallentaa dokumenttipalveluun.",
                                username, hakuOid, hakukohdeOid);
                        LOG.error(HttpExceptionWithResponse.appendWrappedResponse("Virheen tiedot", poikkeus), poikkeus);
                    });
        } catch (Throwable t) {
            LOG.error(HttpExceptionWithResponse.appendWrappedResponse("Tuntematon virhetilanne", t), t);
        }
        DokumenttiProsessi prosessi = new DokumenttiProsessi("Pistesyöttö", "tuonti", hakuOid, asList(hakukohdeOid));
        dokumenttiKomponentti.tuoUusiProsessi(prosessi);
        tuontiService.tuo(username, hakuOid, hakukohdeOid, prosessi, new ByteArrayInputStream(xlsx.toByteArray()));
        return prosessi.toProsessiId();
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
            if (hakemukset == null || hakemukset.isEmpty()) {
                asyncResponse.resume(Response.serverError().entity("Ulkoinen pistesyotto API requires at least one hakemus").build());
            } else {
                asyncResponse.setTimeout(30L, TimeUnit.MINUTES);
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
                            tuontiSoteliService.tuo(authorityCheck, hakemukset, username, hakuOid, valinnanvaiheOid,
                                    (onnistuneet, validointivirheet) -> {
                                        UlkoinenResponseDTO response = new UlkoinenResponseDTO();
                                        response.setKasiteltyOk(onnistuneet);
                                        response.setVirheet(Lists.newArrayList(validointivirheet));
                                        asyncResponse.resume(Response.ok(response).build());
                                    },
                                    sisainenPoikkeus -> {
                                        logError("Soteli tuonti epaonnistui!", sisainenPoikkeus);
                                        asyncResponse.resume(Response.serverError().entity(sisainenPoikkeus.toString()).build());
                                    });
                        },
                        sisainenPoikkeus -> {
                            logError("Soteli tuonti epaonnistui!", sisainenPoikkeus);
                            asyncResponse.resume(Response.serverError().entity(sisainenPoikkeus.toString()).build());
                        });
            }
        } catch (Exception e) {
            LOG.error("Soteli tuonti epäonnistui", e);
            asyncResponse.resume(Response.serverError().entity(e.toString()).build());
        }
    }

    private void logError(String errorMessage, Throwable error) {
        LOG.error(HttpExceptionWithResponse.appendWrappedResponse(errorMessage, error), error);
    }
}
