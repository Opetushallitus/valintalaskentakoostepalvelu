package fi.vm.sade.valinta.kooste.valintalaskenta.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.dto.Vastaus;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.LaskentaSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.util.ExcelExportUtil;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Laskenta;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.LaskentaAloitus;
import fi.vm.sade.valinta.kooste.valintalaskenta.dto.Maski;
import fi.vm.sade.valinta.kooste.valintalaskenta.excel.LaskentaDtoAsExcel;
import fi.vm.sade.valinta.kooste.valintalaskenta.route.ValintalaskentaKerrallaRouteValvomo;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTila;
import fi.vm.sade.valinta.seuranta.dto.LaskentaTyyppi;

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

    private Response excelResponse(byte[] bytes, String tiedostonnimi) {
        return Response
                .ok()
                .entity(bytes)
                .header("Content-Length", bytes.length)
                .header("Content-Type", "application/vnd.ms-excel")
                .header("Content-Disposition", "attachment; filename=\"" + tiedostonnimi + "\"")
                .build();
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
                Map<String, Object[][]> sheetAndGrid = Maps.newHashMap();
                List<Object[]> grid = Lists.newArrayList();
                grid.add(new Object[]{"Kysely seuranapalveluun (kohteelle /laksenta/"
                        + uuid
                        + ") aikakatkaistiin. Palvelu saattaa olla ylikuormittunut!"});
                sheetAndGrid.put("Aikakatkaistu", grid.toArray(new Object[][]{}));
                byte[] bytes = ExcelExportUtil.exportGridSheetsAsXlsBytes(sheetAndGrid);
                asyncResponse.resume(excelResponse(bytes, "yhteenveto_aikakatkaistu.xls"));
                LOG.error("Aikakatkaisu Excelin luonnille (kohde /laskenta/{})", uuid);
            }
        });
        seurantaAsyncResource.laskenta(
                uuid,
                laskenta -> {
                    try {
                        byte[] bytes = LaskentaDtoAsExcel.laskentaDtoAsExcel(laskenta);
                        asyncResponse.resume(excelResponse(bytes, "yhteenveto.xls"));
                    } catch (Throwable e) {
                        LOG.error("Excelin muodostuksessa(kohteelle /laskenta/{}) tapahtui virhe: {}", uuid, e.getMessage());
                        Map<String, Object[][]> sheetAndGrid = Maps.newHashMap();
                        List<Object[]> grid = Lists.newArrayList();
                        grid.add(new Object[]{"Virhe Excelin muodostuksessa!"});
                        grid.add(new Object[]{e.getMessage()});
                        for (StackTraceElement se : e.getStackTrace()) {
                            grid.add(new Object[]{se});
                        }
                        sheetAndGrid.put("Virhe", grid.toArray(new Object[][]{}));
                        byte[] bytes = ExcelExportUtil.exportGridSheetsAsXlsBytes(sheetAndGrid);
                        asyncResponse.resume(excelResponse(bytes, "yhteenveto_virhe.xls"));
                        throw e;
                    }
                },
                poikkeus -> {
                    LOG.error("Excelin tietojen haussa seurantapalvelusta(/laskenta/{}) tapahtui virhe: {}",
                            uuid, poikkeus.getMessage());
                    final Map<String, Object[][]> sheetAndGrid = Maps.newHashMap();
                    final List<Object[]> grid = Lists.newArrayList();
                    grid.add(new Object[]{"Virhe seurantapavelun kutsumisessa!"});
                    grid.add(new Object[]{poikkeus.getMessage()});
                    for (StackTraceElement se : poikkeus.getStackTrace()) {
                        grid.add(new Object[]{se});
                    }
                    sheetAndGrid.put("Virhe", grid.toArray(new Object[][]{}));
                    byte[] bytes = ExcelExportUtil.exportGridSheetsAsXlsBytes(sheetAndGrid);
                    asyncResponse.resume(excelResponse(bytes, "yhteenveto_seurantavirhe.xls"));
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
