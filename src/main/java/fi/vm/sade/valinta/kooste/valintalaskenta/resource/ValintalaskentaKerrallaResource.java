package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static fi.vm.sade.valinta.seuranta.dto.IlmoitusDto.ilmoitus;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.security.service.authz.util.AuthorizationUtil;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.util.SecurityUtil;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaStartParams;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.IlmoitusDto;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Controller("ValintalaskentaKerrallaResource")
@Path("valintalaskentakerralla")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintalaskentakerralla", description = "Valintalaskenta kaikille valinnanvaiheille kerralla")
public class ValintalaskentaKerrallaResource {
    private static final Logger LOG = LoggerFactory.getLogger(ValintalaskentaKerrallaResource.class);

    @Autowired
    private ValintalaskentaKerrallaRouteValvomo valintalaskentaValvomo;
    @Autowired
    private ValintalaskentaKerrallaService valintalaskentaKerrallaService;
    @Autowired
    private ValintalaskentaStatusExcelHandler valintalaskentaStatusExcelHandler;
    @Autowired
    private LaskentaSeurantaAsyncResource seurantaAsyncResource;

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
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler((AsyncResponse asyncResponseTimeout) -> {
                LOG.error("Laskennan kaynnistys timeuottasi kutsulle /haku/{}/tyyppi/HAKU?valinnanvaihe={}&valintakoelaskenta={}\r\n{}", hakuOid, valinnanvaihe, valintakoelaskenta);
                asyncResponse.resume(errorResponce("Ajo laskennalle aikakatkaistu!"));
            });
            final String userOID = AuthorizationUtil.getCurrentUser();
            valintalaskentaKerrallaService.kaynnistaLaskentaHaulle(new LaskentaParams(userOID, haunnimi, nimi, LaskentaTyyppi.HAKU, valintakoelaskenta, valinnanvaihe, hakuOid, Optional.empty(), Boolean.TRUE.equals(erillishaku)), asyncResponse::resume);
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe!", e);
            asyncResponse.resume(errorResponce("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
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
                LOG.error("Laskennan kaynnistys timeuottasi kutsulle /haku/{}/tyyppi/{}/whitelist/{}?valinnanvaihe={}&valintakoelaskenta={}\r\n{}", hakuOid, laskentatyyppi, whitelist, valinnanvaihe, valintakoelaskenta, hakukohdeOids);
                asyncResponse.resume(errorResponce("Uudelleen ajo laskennalle aikakatkaistu!"));
            });

            Maski maski = whitelist ? Maski.whitelist(stringMaski) : Maski.blacklist(stringMaski);
            final String userOID = AuthorizationUtil.getCurrentUser();
            valintalaskentaKerrallaService.kaynnistaLaskentaHaulle(new LaskentaParams(userOID, haunnimi, nimi, laskentatyyppi, valintakoelaskenta, valinnanvaihe, hakuOid, Optional.of(maski), Boolean.TRUE.equals(erillishaku)), (Response response) -> asyncResponse.resume(response));
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe!", e);
            asyncResponse.resume(errorResponce("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
            throw e;
        }
    }

    @POST
    @Path("/uudelleenyrita/{uuid}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public void uudelleenajoLaskennalle(@PathParam("uuid") String uuid, @Suspended AsyncResponse asyncResponse) {
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler((AsyncResponse asyncResponseTimeout) -> {
                LOG.error("Uudelleen ajo laskennalle({}) timeouttasi!", uuid);
                asyncResponseTimeout.resume(errorResponce("Uudelleen ajo laskennalle timeouttasi!"));
            });
            valintalaskentaKerrallaService.kaynnistaLaskentaUudelleen(uuid, (Response response) -> asyncResponse.resume(response));
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe", e);
            asyncResponse.resume(errorResponce("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage()));
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
        asyncResponse.setTimeout(15L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler((AsyncResponse asyncResponseTimeout) -> asyncResponseTimeout.resume(valintalaskentaStatusExcelHandler.createTimeoutErrorXls(uuid)));
        valintalaskentaStatusExcelHandler.getStatusXls(uuid, (Response response) -> asyncResponse.resume(response));
    }

    @DELETE
    @Path("/haku/{uuid}")
    public Response lopetaLaskenta(@PathParam("uuid") String uuid, @QueryParam("lopetaVainJonossaOlevaLaskenta") Boolean lopetaVainJonossaOlevaLaskenta) {
        if (uuid == null) {
            return errorResponce("Uuid on pakollinen");
        }
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

    private Response errorResponce(final String errorMessage) {
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
