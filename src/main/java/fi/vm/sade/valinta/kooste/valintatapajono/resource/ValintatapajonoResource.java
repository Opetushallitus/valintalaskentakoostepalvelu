package fi.vm.sade.valinta.kooste.valintatapajono.resource;

import static java.util.concurrent.TimeUnit.MINUTES;

import fi.vm.sade.auditlog.User;
import fi.vm.sade.authentication.business.service.Authorizer;
import fi.vm.sade.valinta.sharedutils.AuditLog;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.HakukohdeHelper;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.valintatapajono.dto.ValintatapajonoRivit;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.valintatapajono.service.ValintatapajonoTuontiService;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @Autowired(required = false) Camelin pois refaktorointi
 */
///valintaperusteet-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
///haku-app/applications/listfull?appStates=ACTIVE&appStates=INCOMPLETE&rows=100000&aoOid={hakukohdeOid}&asId={hakuOid}
///valintalaskenta-laskenta-service/resources/valintalaskentakoostepalvelu/hakukohde/{hakukohdeOid}/valinnanvaihe
@Controller
@Path("valintatapajonolaskenta")
@PreAuthorize("isAuthenticated()")
@Api(value = "/valintatapajonolaskenta", description = "Valintatapajonon tuonti ja vienti taulukkolaskentaan")
public class ValintatapajonoResource {
    public static final String ROLE_TULOSTENTUONTI = "ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI";
    private final Logger LOG = LoggerFactory.getLogger(ValintatapajonoResource.class);

    @Autowired
    private Authorizer authorizer;
    @Autowired
    private ValintatapajonoTuontiService valintatapajonoTuontiService;

    @Autowired(required = false)
    private ValintatapajonoVientiRoute valintatapajonoVienti;
    @Autowired
    private DokumenttiProsessiKomponentti dokumenttiProsessiKomponentti;
    @Autowired
    private TarjontaAsyncResource tarjontaResource;

    @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
    @POST
    @Path("/vienti")
    @Consumes("application/json")
    @ApiOperation(consumes = "application/json", value = "Valintatapajonon vienti taulukkolaskentaan", response = ProsessiId.class)
    public ProsessiId vienti(@QueryParam("hakuOid") String hakuOid,
                             @QueryParam("hakukohdeOid") String hakukohdeOid,
                             @QueryParam("valintatapajonoOid") String valintatapajonoOid) {
        String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
        authorizer.checkOrganisationAccess(tarjoajaOid, ValintatapajonoResource.ROLE_TULOSTENTUONTI);
        DokumenttiProsessi prosessi = new DokumenttiProsessi("Valintatapajono", "vienti", hakuOid, Arrays.asList(hakukohdeOid));
        valintatapajonoVienti.vie(prosessi, hakuOid, hakukohdeOid, valintatapajonoOid);
        dokumenttiProsessiKomponentti.tuoUusiProsessi(prosessi);
        return prosessi.toProsessiId();
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
    @POST
    @Path("/tuonti")
    @Consumes("application/octet-stream")
    @Produces("text/plain")
    @ApiOperation(consumes = "application/octet-stream", value = "Valintatapajonon tuonti taulukkolaskennasta", response = ProsessiId.class)
    public void tuonti(@QueryParam("hakuOid") String hakuOid,
                       @QueryParam("hakukohdeOid") String hakukohdeOid,
                       @QueryParam("valintatapajonoOid") String valintatapajonoOid,
                       InputStream file,
                       @Suspended AsyncResponse asyncResponse,
                       @Context HttpServletRequest request) {
        final User user = AuditLog.getUser(request);
        asyncResponse.setTimeout(1L, MINUTES);
        asyncResponse.setTimeoutHandler(getTimeoutHandler(hakuOid, hakukohdeOid));
        String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
        authorizer.checkOrganisationAccess(tarjoajaOid, ValintatapajonoResource.ROLE_TULOSTENTUONTI);
        final ByteArrayOutputStream bytes;
        try {
            IOUtils.copy(file, bytes = new ByteArrayOutputStream());
            IOUtils.closeQuietly(file);
            valintatapajonoTuontiService.tuo((valinnanvaiheet, hakemukset) -> {
                ValintatapajonoDataRiviListAdapter listaus = new ValintatapajonoDataRiviListAdapter();
                try {
                    ValintatapajonoExcel valintatapajonoExcel = new ValintatapajonoExcel(
                            hakuOid, hakukohdeOid, valintatapajonoOid,
                            "", "",
                            valinnanvaiheet, hakemukset, Arrays
                            .asList(listaus));
                    valintatapajonoExcel.getExcel().tuoXlsx(new ByteArrayInputStream(bytes.toByteArray()));
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
                return listaus.getRivit();
            }, hakuOid, hakukohdeOid, tarjoajaOid, valintatapajonoOid, asyncResponse, user);
        } catch (Throwable t) {
            asyncResponse.resume(Response.serverError()
                    .entity("Valintatapajonon tuonti epäonnistui tiedoston lukemiseen")
                    .build());
        }
    }

    @PreAuthorize("hasAnyRole('ROLE_APP_VALINTOJENTOTEUTTAMINEN_TULOSTENTUONTI')")
    @POST
    @Path("/tuonti/json")
    @Consumes("application/json")
    @Produces("text/plain")
    @ApiOperation(consumes = "application/json", value = "Valintatapajonon tuonti jsonista", response = String.class)
    public void tuonti(@QueryParam("hakuOid") String hakuOid,
                       @QueryParam("hakukohdeOid") String hakukohdeOid,
                       @QueryParam("valintatapajonoOid") String valintatapajonoOid,
                       ValintatapajonoRivit rivit,
                       @Suspended AsyncResponse asyncResponse,
                       @Context HttpServletRequest request) {
        final User user = AuditLog.getUser(request);
        asyncResponse.setTimeout(1L, MINUTES);
        asyncResponse.setTimeoutHandler(getTimeoutHandler(hakuOid, hakukohdeOid));
        String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
        authorizer.checkOrganisationAccess(tarjoajaOid, ValintatapajonoResource.ROLE_TULOSTENTUONTI);
        valintatapajonoTuontiService.tuo(
            (valinnanvaiheet, hakemukset) -> rivit.getRivit(),
            hakuOid,
            hakukohdeOid,
            tarjoajaOid,
            valintatapajonoOid,
            asyncResponse,
            user);
    }

    private TimeoutHandler getTimeoutHandler(String hakuOid, String hakukohdeOid) {
        return asyncResponse1 -> {
            LOG.error("Valintatapajonon tuonti on aikakatkaistu: /haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
            asyncResponse1.resume(Response.serverError().entity("Valintatapajonon tuonti on aikakatkaistu").build());
        };
    }

    private String findTarjoajaOid(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        try {
            return HakukohdeHelper.tarjoajaOid(tarjontaResource.haeHakukohde(hakukohdeOid).get(1, MINUTES));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
