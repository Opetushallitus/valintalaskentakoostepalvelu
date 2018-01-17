package fi.vm.sade.valinta.kooste.valintatapajono.resource;

import static java.util.concurrent.TimeUnit.MINUTES;
import static rx.observables.BlockingObservable.from;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import fi.vm.sade.authentication.business.service.Authorizer;
import fi.vm.sade.valinta.kooste.KoosteAudit;
import fi.vm.sade.valinta.kooste.external.resource.seuranta.DokumentinSeurantaAsyncResource;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.HakukohdeHelper;
import fi.vm.sade.valinta.kooste.external.resource.tarjonta.TarjontaAsyncResource;
import fi.vm.sade.valinta.kooste.valintatapajono.dto.ValintatapajonoRivit;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoDataRiviListAdapter;
import fi.vm.sade.valinta.kooste.valintatapajono.excel.ValintatapajonoExcel;
import fi.vm.sade.valinta.kooste.valintatapajono.service.ValintatapajonoTuontiService;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import io.swagger.annotations.ApiOperation;

import fi.vm.sade.valinta.kooste.valintatapajono.route.ValintatapajonoVientiRoute;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.DokumenttiProsessi;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import fi.vm.sade.valinta.kooste.viestintapalvelu.komponentti.DokumenttiProsessiKomponentti;

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
    @Autowired
    private DokumentinSeurantaAsyncResource dokumentinSeurantaAsyncResource;

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
                       @Suspended AsyncResponse asyncResponse) {
        final String username = KoosteAudit.username();
        asyncResponse.setTimeout(1L, MINUTES);
        asyncResponse.setTimeoutHandler(getTimeoutHandler(hakuOid, hakukohdeOid));
        String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
        authorizer.checkOrganisationAccess(tarjoajaOid, ValintatapajonoResource.ROLE_TULOSTENTUONTI);
        final ByteArrayOutputStream bytes;
        try {
            IOUtils.copy(file, bytes = new ByteArrayOutputStream());
            IOUtils.closeQuietly(file);
            valintatapajonoTuontiService.tuo(username, (valinnanvaiheet, hakemukset) -> {
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
            }, hakuOid, hakukohdeOid, tarjoajaOid, valintatapajonoOid, asyncResponse);
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
                       @Suspended AsyncResponse asyncResponse) {
        final String username = KoosteAudit.username();
        asyncResponse.setTimeout(1L, MINUTES);
        asyncResponse.setTimeoutHandler(getTimeoutHandler(hakuOid, hakukohdeOid));
        String tarjoajaOid = findTarjoajaOid(hakukohdeOid);
        authorizer.checkOrganisationAccess(tarjoajaOid, ValintatapajonoResource.ROLE_TULOSTENTUONTI);
        valintatapajonoTuontiService.tuo(
                username,
                (valinnanvaiheet, hakemukset) -> {
                    return rivit.getRivit();
                }, hakuOid, hakukohdeOid, tarjoajaOid, valintatapajonoOid, asyncResponse);
    }

    private TimeoutHandler getTimeoutHandler(String hakuOid, String hakukohdeOid) {
        return asyncResponse1 -> {
            LOG.error("Valintatapajonon tuonti on aikakatkaistu: /haku/{}/hakukohde/{}", hakuOid, hakukohdeOid);
            asyncResponse1.resume(Response.serverError().entity("Valintatapajonon tuonti on aikakatkaistu").build());
        };
    }

    private String findTarjoajaOid(@QueryParam("hakukohdeOid") String hakukohdeOid) {
        return HakukohdeHelper.tarjoajaOid(from(tarjontaResource.haeHakukohde(hakukohdeOid).timeout(1, MINUTES)).first());
    }
}
