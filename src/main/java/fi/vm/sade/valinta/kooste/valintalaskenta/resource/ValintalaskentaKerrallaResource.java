package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * @author Jussi Jartamo
 */
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

    /**
     * Koko haun laskenta
     *
     * @param hakuOid
     * @return
     */
    @POST
    @Path("/haku/{hakuOid}/tyyppi/{tyyppi}/whitelist/{whitelist}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public void valintalaskentaHaulle(
            @PathParam("hakuOid") String hakuOid,
            @QueryParam("erillishaku") Boolean erillishaku,
            @QueryParam("valinnanvaihe") Integer valinnanvaihe,
            @QueryParam("valintakoelaskenta") Boolean valintakoelaskenta,
            @PathParam("tyyppi") LaskentaTyyppi tyyppi,
            @PathParam("whitelist") boolean whitelist,
            List<String> maski,
            @Suspended AsyncResponse asyncResponse) {
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler(new ValintalaskentaKerrallaTimeoutHandler(hakuOid, valinnanvaihe, valintakoelaskenta, tyyppi, whitelist, maski, asyncResponse));

            valintalaskentaKerrallaService.kaynnistaLaskenta(
                    tyyppi,
                    hakuOid,
                    new Maski(whitelist, maski),
                    (hakukohdeOids, laskennanAloitus) -> {
                        valintalaskentaKerrallaService.kasitteleKokoPaska(
                                hakukohdeOids,
                                laskennanAloitus,
                                asyncResponse,
                                seurantaAsyncResource,
                                hakuOid,
                                tyyppi,
                                erillishaku,
                                valinnanvaihe,
                                valintakoelaskenta);
                    },
                    Boolean.TRUE.equals(erillishaku),
                    LaskentaTyyppi.VALINTARYHMA.equals(tyyppi),
                    valinnanvaihe,
                    valintakoelaskenta,
                    asyncResponse);
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}", e.getMessage());
            asyncResponse.resume(Response
                    .serverError()
                    .entity("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage())
                    .build());
            throw e;
        }
    }

    /**
     * Uudelleen aja vanha haku
     *
     * @return
     */
    @POST
    @Path("/uudelleenyrita/{uuid}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public void uudelleenajoLaskennalle(
            @PathParam("uuid") String uuid,
            @Suspended AsyncResponse asyncResponse) {
        try {
            asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
            asyncResponse.setTimeoutHandler(new TimeoutHandler() {
                public void handleTimeout(AsyncResponse asyncResponse) {
                    LOG.error("Uudelleen ajo laskennalle({}) timeouttasi!",
                            uuid);
                    asyncResponse.resume(Response.serverError()
                            .entity("Uudelleen ajo laskennalle timeouttasi!")
                            .build());
                }
            });
            final Laskenta l = valintalaskentaValvomo.haeLaskenta(uuid);
            if (l != null && !l.isValmis()) {
                LOG.warn("Laskenta {} on viela ajossa, joten palautetaan linkki siihen.", uuid);
                asyncResponse.resume(Response
                        .ok(Vastaus.uudelleenOhjaus(uuid))
                        .build());
            }
            seurantaAsyncResource.resetoiTilat(
                    uuid,
                    laskenta -> {
                        valintalaskentaKerrallaService.kasitteleKaynnistaLaskentaUudelleen(laskenta, asyncResponse);
                    },
                    t -> {
                        LOG.error("Uudelleen ajo laskennalle heitti poikkeuksen {}:\r\n{}",
                                t.getMessage(), Arrays.toString(t.getStackTrace()));
                        asyncResponse.resume(Response
                                .serverError()
                                .entity("Uudelleen ajo laskennalle heitti poikkeuksen!")
                                .build());
                    });
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}", e.getMessage());
            asyncResponse.resume(Response
                    .serverError()
                    .entity("Odottamaton virhe laskennan kaynnistamisessa! " + e.getMessage())
                    .build());
            throw e;
        }
    }


    @GET
    @Path("/status")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
    public List<Laskenta> status() {
        return valintalaskentaValvomo.ajossaOlevatLaskennat();
    }

    @GET
    @Path("/status/{uuid}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Valintalaskennan tila", response = Laskenta.class)
    public Laskenta status(@PathParam("uuid") String uuid) {
        try {
            return valintalaskentaValvomo.haeLaskenta(uuid);
        } catch (Exception e) {
            LOG.error("Valintalaskennan statuksen luku heitti poikkeuksen! {}", e.getMessage());
            return null;
        }
    }

    @GET
    @Path("/status/{uuid}/xls")
    @Produces("application/vnd.ms-excel")
    @ApiOperation(value = "Valintalaskennan tila", response = LaskentaAloitus.class)
    public void statusXls(
            @PathParam("uuid") final String uuid,
            @Suspended final AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(15L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            public void handleTimeout(AsyncResponse asyncResponse) {
                asyncResponse.resume(valintalaskentaStatusExcelHandler.createTimeoutErrorXls(uuid));
            }
        });
        valintalaskentaStatusExcelHandler.getStatusXls(
                uuid,
                responce -> {
                    asyncResponse.resume(responce);
                });
    }

    /**
     * Sammutta laskennan uuid:lla jos laskenta on kaynnissa
     *
     * @param uuid
     * @return 200 OK
     */
    @DELETE
    @Path("/haku/{uuid}")
    public Response lopetaLaskenta(@PathParam("uuid") String uuid) {
        if (uuid == null) {
            return Response
                    .serverError()
                    .entity("Uuid on pakollinen")
                    .build();
        }
        final Laskenta l = valintalaskentaValvomo.haeLaskenta(uuid);
        if (l != null) {
            l.lopeta();// getLopetusehto().set(true); // aktivoidaan
            // lopetuskasky
            seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.PERUUTETTU);
        }
        return Response
                .ok()
                .build();
    }
}
