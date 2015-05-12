package fi.vm.sade.valinta.kooste.proxy.resource.valintatulosservice;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.ErillishakuProsessiDTO;
import fi.vm.sade.valinta.kooste.erillishaku.dto.Hakutyyppi;
import fi.vm.sade.valinta.kooste.external.resource.valintatulosservice.ValintaTulosServiceAsyncResource;
import fi.vm.sade.valinta.kooste.viestintapalvelu.dto.ProsessiId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Jussi Jartamo
 */
@Controller("ValintaTulosServiceProxyResource")
@Path("proxy/valintatulosservice")
@PreAuthorize("isAuthenticated()")
@Api(value = "/proxy/valintatulosservice", description = "Käyttöliittymäkutsujen välityspalvelin valinta-tulos-serviceen")
public class ValintaTulosServiceProxyResource {
    private static final Logger LOG = LoggerFactory.getLogger(ValintaTulosServiceProxyResource.class);

    @Autowired
    private ValintaTulosServiceAsyncResource valintaTulosService;

    @PreAuthorize("hasAnyRole('ROLE_APP_HAKEMUS_READ_UPDATE', 'ROLE_APP_HAKEMUS_READ', 'ROLE_APP_HAKEMUS_CRUD', 'ROLE_APP_HAKEMUS_LISATIETORU', 'ROLE_APP_HAKEMUS_LISATIETOCRUD')")
    @GET
    @Path("/haku/{hakuOid}/hakukohde/{hakukohdeOid}")
    @Consumes("application/json")
    @Produces("application/json")
    @ApiOperation(consumes = "application/json", value = "Hakukohteen valintatulokset", response = ProsessiId.class)
    public void vienti(
            @PathParam("hakuOid") String hakuOid,
            @PathParam("hakukohdeOid") String hakukohdeOid,
            @Suspended AsyncResponse asyncResponse) {
        //authorizer.checkOrganisationAccess(tarjoajaOid, ROLE_APP_HAKEMUS_CRUD);
        asyncResponse.setTimeout(1L, TimeUnit.MINUTES);
        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            public void handleTimeout(AsyncResponse asyncResponse) {
                LOG.error(
                        "Valinta-tulos-service -palvelukutsu on aikakatkaistu: /haku/{}/hakukohde/{}",
                        hakuOid, hakukohdeOid);
                asyncResponse.resume(Response.serverError()
                        .entity("Valinta-tulos-service -palvelukutsu on aikakatkaistu")
                        .build());
            }
        });

        //LOG.error("KUTSUTAAN {} {}", hakuOid, hakukohdeOid);
        //valintaTulosService.getValintatulokset(hakuOid,hakukohdeOid, null);

    }

}
