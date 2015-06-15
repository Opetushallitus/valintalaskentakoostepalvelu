package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
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

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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
    @Path("/haku/{hakuOid}/tyyppi/{tyyppi}/whitelist/{whitelist}")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public void valintalaskentaHaulle(
            @PathParam("hakuOid") String hakuOid,
            @QueryParam("erillishaku") Boolean erillishaku,
            @QueryParam("valinnanvaihe") Integer valinnanvaihe,
            @QueryParam("valintakoelaskenta") Boolean valintakoelaskenta,
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
            valintalaskentaKerrallaService.kaynnistaLaskentaHaulle(new LaskentaParams(laskentatyyppi, valintakoelaskenta, valinnanvaihe, hakuOid, maski, Boolean.TRUE.equals(erillishaku)), (Response response) -> asyncResponse.resume(response));
        } catch (Throwable e) {
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}", e.getMessage());
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
            LOG.error("Laskennan kaynnistamisessa tapahtui odottamaton virhe: {}", e.getMessage());
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
            LOG.error("Valintalaskennan statuksen luku heitti poikkeuksen! {}", e.getMessage());
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
    public Response lopetaLaskenta(@PathParam("uuid") String uuid) {
        if (uuid == null) {
            return errorResponce("Uuid on pakollinen");
        }
        final Laskenta l = valintalaskentaValvomo.fetchLaskenta(uuid);
        if (l != null) {
            l.lopeta();
            seurantaAsyncResource.merkkaaLaskennanTila(uuid, LaskentaTila.PERUUTETTU);
        }
        return Response.ok().build();
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
                return e.getMessage();
            }
        }
        return null;
    }
}
