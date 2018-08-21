package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.ilmoitus;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.util.Arrays.asList;

import fi.vm.sade.valinta.kooste.pistesyotto.service.HakukohdeOIDAuthorityCheck;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import fi.vm.sade.security.service.authz.util.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.security.AuthorityCheckService;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import rx.Observable;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller("ValintalaskentaKerrallaResource")
@Path("valintalaskentakerralla")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentakerralla", description = "Valintalaskenta kaikille valinnanvaiheille kerralla")
public class ValintalaskentaKerrallaResource {
    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaKerrallaResource.class);
    private static final List<String> valintaperusteetCRUDRoles = asList("ROLE_APP_VALINTAPERUSTEET_CRUD", "ROLE_APP_VALINTAPERUSTEETKK_CRUD");

    @Autowired
    private ValintalaskentaKerrallaRouteValvomo valintalaskentaValvomo;
    @Autowired
    private ValintalaskentaKerrallaService valintalaskentaKerrallaService;
    @Autowired
    private ValintalaskentaStatusExcelHandler valintalaskentaStatusExcelHandler;
    @Autowired
    private LaskentaSeurantaAsyncResource seurantaAsyncResource;
    @Autowired
    private AuthorityCheckService authorityCheckService;

    @POST
    @Path("/haku/{hakuOid}/tyyppi/HAKU")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public void valintalaskentaKokoHaulle(
            @PathParam("hakuOid") String hakuOid,
            @QueryParam("erillishaku") Boolean erillishaku,
            @QueryParam("valinnanvaihe") Integer valinnanvaihe,
            @QueryParam("valintakoelaskenta") Boolean valintakoelaskenta,
            @QueryParam("haunnimi") String haunnimi,
            @QueryParam("nimi") String nimi,
            @Suspended AsyncResponse asyncResponse) {
        authorityCheckService.checkAuthorizationForHaku(hakuOid, valintaperusteetCRUDRoles);
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler((AsyncResponse asyncResponseTimeout) -> {
                LOG.error("Laskennan kaynnistys timeuottasi kutsulle /haku/{}/tyyppi/HAKU?valinnanvaihe={}&valintakoelaskenta={}\r\n{}", hakuOid, valinnanvaihe, valintakoelaskenta);
                asyncResponse.resume(errorResponse("Ajo laskennalle aikakatkaistu!"));
            });
            final String userOID = AuthorizationUtil.getCurrentUser();
            valintalaskentaKerrallaService.kaynnistaLaskentaHaulle(
                    new LaskentaParams(userOID, haunnimi, nimi, LaskentaTyyppi.HAKU, valintakoelaskenta, valinnanvaihe,
                            hakuOid, Optional.empty(), Boolean.TRUE.equals(erillishaku)),
                    asyncResponse::resume);
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe!", e);
            asyncResponse.resume(errorResponse("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
            throw e;
        }
    }

    @POST
    @Path("/haku/{hakuOid}/tyyppi/{tyyppi}/whitelist/{whitelist}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public void valintalaskentaHaulle(
            @PathParam("hakuOid") String hakuOid,
            @QueryParam("erillishaku") Boolean erillishaku,
            @QueryParam("valinnanvaihe") Integer valinnanvaihe,
            @QueryParam("valintakoelaskenta") Boolean valintakoelaskenta,
            @QueryParam("haunnimi") String haunnimi,
            @QueryParam("nimi") String nimi,
            @PathParam("tyyppi") LaskentaTyyppi laskentatyyppi,
            @PathParam("whitelist") boolean whitelist,
            List<String> stringMaski,
            @Suspended AsyncResponse asyncResponse) {
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler((AsyncResponse asyncResponseTimeout) -> {
                final String hakukohdeOids = hakukohdeOidsFromMaskiToString(stringMaski);
                LOG.error("Laskennan kaynnistys timeouttasi kutsulle /haku/{}/tyyppi/{}/whitelist/{}?valinnanvaihe={}&valintakoelaskenta={}\r\n{}", hakuOid, laskentatyyppi, whitelist, valinnanvaihe, valintakoelaskenta, hakukohdeOids);
                asyncResponse.resume(errorResponse("Uudelleen ajo laskennalle aikakatkaistu!"));
            });

            Maski maski = whitelist ? Maski.whitelist(stringMaski) : Maski.blacklist(stringMaski);

            final String userOID = AuthorizationUtil.getCurrentUser();
            valintalaskentaKerrallaService.kaynnistaLaskentaHaulle(
                    new LaskentaParams(userOID, haunnimi, nimi, laskentatyyppi, valintakoelaskenta,
                            valinnanvaihe, hakuOid, Optional.of(maski), Boolean.TRUE.equals(erillishaku)),
                    asyncResponse::resume,
                    authorityCheckService.getAuthorityCheckForRoles(valintaperusteetCRUDRoles));

        } catch (ForbiddenException fe) {
            asyncResponse.resume(fe);
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe!", e);
            asyncResponse.resume(errorResponse("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
            throw e;
        }
    }


    @POST
    @Path("/uudelleenyrita/{uuid}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public void uudelleenajoLaskennalle(@PathParam("uuid") String uuid, @Suspended AsyncResponse asyncResponse) {
        authorityCheckService.checkAuthorizationForHaku(uuid, valintaperusteetCRUDRoles);
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler((AsyncResponse asyncResponseTimeout) -> {
                LOG.error("Uudelleen ajo laskennalle({}) timeouttasi!", uuid);
                asyncResponseTimeout.resume(errorResponse("Uudelleen ajo laskennalle timeouttasi!"));
            });
            valintalaskentaKerrallaService.kaynnistaLaskentaUudelleen(uuid, (Response response) -> asyncResponse.resume(response));
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe", e);
            asyncResponse.resume(errorResponse("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
            throw e;
        }
    }


    @GET
    @Path("/status")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
    public List<Laskenta> status() {
        return valintalaskentaValvomo.runningLaskentas();
    }

    @GET
    @Path("/status/{uuid}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
    public Laskenta status(@PathParam("uuid") String uuid) {
        authorityCheckService.checkAuthorizationForHaku(uuid, valintaperusteetCRUDRoles);
        try {
            return valintalaskentaValvomo.fetchLaskenta(uuid);
        } catch (Exception e) {
            LOG.error("Valintalaskennan statuksen luku heitti poikkeuksen!", e);
            return null;
        }
    }

    @GET
    @Path("/status/{uuid}/xls")
    @Produces("application/vnd.ms-excel")
    @ApiOperation(value = "Valintalaskennan tila", response = LaskentaStartParams.class)
    public void statusXls(@PathParam("uuid") final String uuid, @Suspended final AsyncResponse asyncResponse) {
        authorityCheckService.checkAuthorizationForHaku(uuid, valintaperusteetCRUDRoles);
        asyncResponse.setTimeout(15L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler((AsyncResponse asyncResponseTimeout) -> asyncResponseTimeout.resume(valintalaskentaStatusExcelHandler.createTimeoutErrorXls(uuid)));
        valintalaskentaStatusExcelHandler.getStatusXls(uuid, (Response response) -> asyncResponse.resume(response));
    }


    @DELETE
    @Path("/haku/{uuid}")
    public Response lopetaLaskenta(@PathParam("uuid") String uuid, @QueryParam("lopetaVainJonossaOlevaLaskenta") Boolean lopetaVainJonossaOlevaLaskenta) {
        if (uuid == null) {
            return errorResponse("Uuid on pakollinen");
        }

        authorityCheckService.checkAuthorizationForHaku(uuid, valintaperusteetCRUDRoles);

        if(Boolean.TRUE.equals(lopetaVainJonossaOlevaLaskenta)) {
            boolean onkoLaskentaVielaJonossa = valintalaskentaValvomo.fetchLaskenta(uuid) == null;
            if(!onkoLaskentaVielaJonossa) {
                // Laskentaa suoritetaan jo joten ei pysayteta
                return Response.ok().build();
            }
        }
        stop(uuid);
        seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.PERUUTETTU, Optional.of(ilmoitus("Peruutettu käyttäjän toimesta"))).subscribe(ok -> stop(uuid), nok -> stop(uuid));
        return Response.ok().build();
    }

    private void stop(String uuid) {
        Optional.ofNullable(valintalaskentaValvomo.fetchLaskenta(uuid)).ifPresent(Laskenta::lopeta);
    }

    private Response errorResponse(final String errorMessage) {
        return Response.serverError().entity(errorMessage).build();
    }

    private String hakukohdeOidsFromMaskiToString(List<String> maski) {
        if (maski != null && !maski.isEmpty()) {
            try {
                Object[] hakukohdeOidArray = maski.toArray();
                StringBuilder sb = new StringBuilder();
                sb.append(Arrays.toString(Arrays.copyOfRange(hakukohdeOidArray, 0, Math.min(hakukohdeOidArray.length, 10))));
                if (hakukohdeOidArray.length > 10) {
                    sb.append(" ensimmaiset 10 hakukohdetta maskissa jossa on yhteensa hakukohteita ").append(hakukohdeOidArray.length);
                } else {
                    sb.append(" maskin hakukohteet");
                }
                return sb.toString();
            } catch (Exception e) {
                LOG.error("hakukohdeOidsFromMaskiToString", e);
                return e.getMessage();
            }
        }
        return null;
    }
}
